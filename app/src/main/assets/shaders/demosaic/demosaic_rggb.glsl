precision highp float;

varying vec2 vTexCoord;
uniform sampler2D RawBuffer;
uniform ivec2 size;
uniform vec4 blackLevel;
uniform float whiteLevel;

float getRawValue(ivec2 coord) {
    coord = clamp(coord, ivec2(0, 0), size - 1);
    return texelFetch(RawBuffer, coord, 0).r;
}

// 计算水平梯度
float gradientH(ivec2 center) {
    return abs(getRawValue(center + ivec2(2, 0)) - getRawValue(center - ivec2(2, 0))) +
           abs(getRawValue(center + ivec2(1, 0)) - getRawValue(center - ivec2(1, 0))) * 0.5;
}

// 计算垂直梯度
float gradientV(ivec2 center) {
    return abs(getRawValue(center + ivec2(0, 2)) - getRawValue(center - ivec2(0, 2))) +
           abs(getRawValue(center + ivec2(0, 1)) - getRawValue(center - ivec2(0, 1))) * 0.5;
}

// 计算对角线梯度
float gradientD1(ivec2 center) {
    return abs(getRawValue(center + ivec2(2, 2)) - getRawValue(center - ivec2(2, 2))) +
           abs(getRawValue(center + ivec2(1, 1)) - getRawValue(center - ivec2(1, 1))) * 0.5;
}

float gradientD2(ivec2 center) {
    return abs(getRawValue(center + ivec2(-2, 2)) - getRawValue(center - ivec2(-2, 2))) +
           abs(getRawValue(center + ivec2(-1, 1)) - getRawValue(center - ivec2(-1, 1))) * 0.5;
}

// 边缘感知绿色插值
float interpolateGreen(ivec2 coord) {
    float hGrad = gradientH(coord);
    float vGrad = gradientV(coord);

    float hVal = (getRawValue(coord + ivec2(1, 0)) + getRawValue(coord - ivec2(1, 0))) * 0.5;
    float vVal = (getRawValue(coord + ivec2(0, 1)) + getRawValue(coord - ivec2(0, 1))) * 0.5;

    // 根据梯度自适应选择插值方向
    float threshold = 0.1;
    if (abs(hGrad - vGrad) < threshold) {
        // 梯度相似，使用双线性
        return (hVal + vVal) * 0.5;
    } else if (hGrad < vGrad) {
        // 水平方向更平滑，加强水平权重
        return mix(hVal, vVal, 0.25);
    } else {
        // 垂直方向更平滑，加强垂直权重
        return mix(hVal, vVal, 0.75);
    }
}

// 边缘感知红色/蓝色插值
float interpolateRB(ivec2 coord, bool isRed) {
    float d1Grad = gradientD1(coord);
    float d2Grad = gradientD2(coord);

    float d1Val, d2Val;
    if (isRed) {
        d1Val = (getRawValue(coord + ivec2(1, 1)) + getRawValue(coord - ivec2(1, 1))) * 0.5;
        d2Val = (getRawValue(coord + ivec2(-1, 1)) + getRawValue(coord - ivec2(-1, 1))) * 0.5;
    } else {
        d1Val = (getRawValue(coord + ivec2(-1, -1)) + getRawValue(coord - ivec2(-1, -1))) * 0.5;
        d2Val = (getRawValue(coord + ivec2(1, -1)) + getRawValue(coord - ivec2(1, -1))) * 0.5;
    }

    float threshold = 0.1;
    if (abs(d1Grad - d2Grad) < threshold) {
        return (d1Val + d2Val) * 0.5;
    } else if (d1Grad < d2Grad) {
        return mix(d1Val, d2Val, 0.25);
    } else {
        return mix(d1Val, d2Val, 0.75);
    }
}

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);

    int x = coord.x;
    int y = coord.y;

    int patternX = x % 2;
    int patternY = y % 2;

    float r, g, b;

    if (patternX == 0 && patternY == 0) {
        // R位置: R G
        //        G B
        r = getRawValue(ivec2(x, y));
        g = interpolateGreen(ivec2(x, y));
        b = interpolateRB(ivec2(x, y), false);
    } else if (patternX == 1 && patternY == 0) {
        // G位置(在R行): G R
        //              B G
        g = getRawValue(ivec2(x, y));
        r = (getRawValue(ivec2(x+1, y)) + getRawValue(ivec2(x-1, y))) * 0.5;
        b = (getRawValue(ivec2(x, y+1)) + getRawValue(ivec2(x, y-1))) * 0.5;
    } else if (patternX == 0 && patternY == 1) {
        // G位置(在B行): B G
        //              G R
        g = getRawValue(ivec2(x, y));
        b = (getRawValue(ivec2(x+1, y)) + getRawValue(ivec2(x-1, y))) * 0.5;
        r = (getRawValue(ivec2(x, y+1)) + getRawValue(ivec2(x, y-1))) * 0.5;
    } else {
        // B位置: G B
        //        R G
        b = getRawValue(ivec2(x, y));
        g = interpolateGreen(ivec2(x, y));
        r = interpolateRB(ivec2(x, y), true);
    }

    float bl = blackLevel.r;
    float wl = whiteLevel;

    r = clamp((r - bl) / (wl - bl), 0.0, 1.0);
    g = clamp((g - bl) / (wl - bl), 0.0, 1.0);
    b = clamp((b - bl) / (wl - bl), 0.0, 1.0);

    // 轻微提升饱和度以增强色彩清晰度
    vec3 rgb = vec3(r, g, b);
    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
    vec3 chroma = rgb - vec3(luma);
    rgb = vec3(luma) + chroma * 1.05; // 轻微提升5%饱和度

    gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), 1.0);
}
