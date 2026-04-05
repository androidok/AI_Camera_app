precision mediump float;

varying vec2 vTexCoord;
uniform sampler2D InputBuffer;
uniform float strength;
uniform float gamma;  // Gamma值，用于暗部噪点控制

// 暗部噪点抑制阈值 - 低于此亮度的区域将应用降噪
#define SHADOW_THRESHOLD 0.08
// 暗部提升强度 - 控制阴影提亮程度
#define SHADOW_LIFT 0.15

/**
 * 计算像素亮度
 * 使用人眼感知权重
 */
float getLuma(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

/**
 * 改进的ACES色调映射函数
 * 参考：ACES Filmic Tone Mapping Curve
 * 特点：更好的暗部细节保留，高光压缩平滑
 */
vec3 acesTonemap(vec3 x) {
    float A = 2.51;
    float B = 0.03;
    float C = 2.43;
    float D = 0.59;
    float E = 0.14;

    return clamp((x * (A * x + B)) / (x * (C * x + D) + E), 0.0, 1.0);
}

/**
 * 带暗部保护的色调映射
 * 减少暗部区域的对比度增强，抑制噪点放大
 */
vec3 shadowProtectedTonemap(vec3 color) {
    float luma = getLuma(color);

    // 基础色调映射
    vec3 tonemapped = acesTonemap(color);

    // 计算暗部保护因子
    // 暗部区域：减少色调映射的增强效果
    float shadowFactor = smoothstep(0.0, SHADOW_THRESHOLD, luma);

    // 在暗部区域混合原始色彩和色调映射结果
    // 这样可以减少暗部对比度增强导致的噪点放大
    vec3 shadowProtected = mix(
        color * (1.0 + SHADOW_LIFT),  // 暗部：轻度提亮，保持平滑
        tonemapped,                    // 正常区域：标准色调映射
        shadowFactor
    );

    return shadowProtected;
}

/**
 * Gamma校正
 * 使用非线性Gamma曲线减少暗部banding
 */
vec3 applyGamma(vec3 color) {
    // SRGB Gamma曲线（近似）
    // 暗部使用线性部分减少banding
    vec3 linearPart = color * 12.92;
    vec3 gammaPart = pow(color, vec3(1.0 / 2.4)) * 1.055 - 0.055;

    // 在暗部使用线性响应
    vec3 result;
    result.r = (color.r <= 0.0031308) ? linearPart.r : gammaPart.r;
    result.g = (color.g <= 0.0031308) ? linearPart.g : gammaPart.g;
    result.b = (color.b <= 0.0031308) ? linearPart.b : gammaPart.b;

    return result;
}

void main() {
    vec3 color = texture2D(InputBuffer, vTexCoord).rgb;

    // 预曝光调整 - 根据强度参数微调
    float exposureScale = 1.0 + (strength - 1.0) * 0.3;
    color *= exposureScale;

    // 应用带暗部保护的色调映射
    color = shadowProtectedTonemap(color);

    // Gamma校正
    color = applyGamma(color);

    // 最终饱和度微调 - 避免暗部色彩失真
    float luma = getLuma(color);
    vec3 chroma = color - vec3(luma);
    // 在暗部轻微减少饱和度（噪点通常在色度通道更明显）
    float saturationFactor = 1.0 - (1.0 - smoothstep(0.0, 0.1, luma)) * 0.2;
    color = vec3(luma) + chroma * saturationFactor;

    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
