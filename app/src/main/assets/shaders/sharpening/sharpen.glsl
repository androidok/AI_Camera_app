precision mediump float;

varying vec2 vTexCoord;
uniform sampler2D InputBuffer;
uniform float strength;
uniform ivec2 size;
uniform float noiseLevel;  // 噪声水平，用于自适应调整锐化强度

#define MSIZE1 5
// 深度阈值 - 用于判断是否为边缘区域
#define depthMin 0.003
#define depthMax 0.950
// 噪声阈值 - 高于此值的区域减少锐化以避免噪点放大
#define NOISE_THRESHOLD 0.03

/**
 * 高斯概率密度函数
 * 用于计算卷积核权重
 */
float normpdf(in float x, in float sigma) {
    return 0.39894 * exp(-0.5 * x * x / (sigma * sigma)) / sigma;
}

/**
 * 计算像素亮度
 * 使用人眼感知权重 (ITU-R BT.709)
 */
float getLuma(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

/**
 * 高斯模糊函数
 * 用于生成锐化掩模的低频参考
 */
vec3 blur(ivec2 coords, sampler2D tex) {
    vec3 mask = vec3(0.0);
    const int kSize = (MSIZE1 - 1) / 2;
    float kernel[MSIZE1];
    float pdfsize = 0.0;

    // 初始化高斯核
    for (int j = 0; j <= kSize; ++j) {
        kernel[kSize + j] = kernel[kSize - j] = normpdf(float(j), 1.1);
    }

    // 执行卷积
    for (int i = -kSize; i <= kSize; ++i) {
        for (int j = -kSize; j <= kSize; ++j) {
            ivec2 coord = coords + ivec2(i, j);
            coord = clamp(coord, ivec2(0, 0), size - 1);
            float pdf = kernel[kSize + j] * kernel[kSize + i];
            mask += texelFetch(tex, coord, 0).rgb * pdf * 2.0;
            pdfsize += pdf;
        }
    }

    return mask / (pdfsize * 2.0);
}

/**
 * 计算局部方差（噪点估计）
 * 使用3x3邻域计算亮度方差
 */
float estimateLocalNoise(ivec2 center, sampler2D tex) {
    float sum = 0.0;
    float sumSq = 0.0;
    int count = 0;

    // 采样3x3邻域
    for (int i = -1; i <= 1; ++i) {
        for (int j = -1; j <= 1; ++j) {
            ivec2 coord = center + ivec2(i, j);
            coord = clamp(coord, ivec2(0, 0), size - 1);
            float luma = getLuma(texelFetch(tex, coord, 0).rgb);
            sum += luma;
            sumSq += luma * luma;
            count++;
        }
    }

    // 计算方差
    float mean = sum / float(count);
    float variance = (sumSq / float(count)) - (mean * mean);
    return sqrt(max(variance, 0.0));
}

/**
 * 计算局部边缘强度
 * 使用Sobel算子检测边缘
 */
float detectEdge(ivec2 center, sampler2D tex) {
    // Sobel X核
    float sobelX = 0.0;
    float sobelY = 0.0;

    // 3x3 Sobel卷积
    for (int i = -1; i <= 1; ++i) {
        for (int j = -1; j <= 1; ++j) {
            ivec2 coord = center + ivec2(j, i);
            coord = clamp(coord, ivec2(0, 0), size - 1);
            float luma = getLuma(texelFetch(tex, coord, 0).rgb);

            // Sobel X权重
            float wx = float(j) * (1.0 + float(abs(i)));
            // Sobel Y权重
            float wy = float(i) * (1.0 + float(abs(j)));

            sobelX += luma * wx;
            sobelY += luma * wy;
        }
    }

    // 边缘强度
    return sqrt(sobelX * sobelX + sobelY * sobelY);
}

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    vec3 cur = texelFetch(InputBuffer, xy, 0).rgb;

    // 估计局部噪声水平
    float localNoise = estimateLocalNoise(xy, InputBuffer);

    // 检测边缘强度
    float edgeStrength = detectEdge(xy, InputBuffer);

    // 构建锐化掩模
    vec3 mask = vec3(0.0);
    float pdfsize = 0.0;
    const int kSize = (MSIZE1 - 1) / 2;
    float kernel[MSIZE1];

    // 初始化高斯核
    for (int j = 0; j <= kSize; ++j) {
        kernel[kSize + j] = kernel[kSize - j] = normpdf(float(j), 2.2);
    }

    // 多级模糊构建锐化掩模
    for (int i = -kSize; i <= kSize; ++i) {
        for (int j = -kSize; j <= kSize; ++j) {
            float pdf = kernel[kSize + j] * kernel[kSize + i];
            mask += blur(xy + ivec2(i, j), InputBuffer) * pdf * 1.75;
            mask += blur(xy + ivec2(i * 2, j * 2), InputBuffer) * pdf * 0.25;
            pdfsize += pdf;
        }
    }

    mask /= pdfsize * 2.0;
    mask = blur(xy, InputBuffer) - mask;

    // 在极低对比度区域完全关闭锐化（抑制平坦区域噪点）
    if (abs(mask.r + mask.b + mask.g) < depthMin) {
        mask *= 0.0;
    }

    // 自适应锐化强度调整 - 增强清晰度
    // 原理：强边缘区域应用更强锐化以提升清晰度
    float adaptiveStrength = strength * 1.5; // 基础强度提升1.5倍

    // 根据局部噪声调整：噪点越多，锐化越弱
    float noiseFactor = 1.0 - smoothstep(0.0, NOISE_THRESHOLD, localNoise);
    adaptiveStrength *= noiseFactor;

    // 根据边缘强度调整：强边缘应用更强锐化
    float edgeFactor = smoothstep(0.03, 0.15, edgeStrength);
    // 边缘区域增强锐化，非边缘区域保持基础强度
    adaptiveStrength = mix(adaptiveStrength * 0.5, adaptiveStrength * 1.3, edgeFactor);

    // 应用自适应锐化 - 增强系数
    cur += (mask.r + mask.g + mask.b) * (adaptiveStrength * 5.0 / 3.0);

    gl_FragColor = vec4(clamp(cur, 0.0, 1.0), 1.0);
}
