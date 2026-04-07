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

float gradientH(ivec2 center) {
    return abs(getRawValue(center + ivec2(2, 0)) - getRawValue(center - ivec2(2, 0))) +
           abs(getRawValue(center + ivec2(1, 0)) - getRawValue(center - ivec2(1, 0))) * 0.5;
}

float gradientV(ivec2 center) {
    return abs(getRawValue(center + ivec2(0, 2)) - getRawValue(center - ivec2(0, 2))) +
           abs(getRawValue(center + ivec2(0, 1)) - getRawValue(center - ivec2(0, 1))) * 0.5;
}

float gradientD1(ivec2 center) {
    return abs(getRawValue(center + ivec2(2, 2)) - getRawValue(center - ivec2(2, 2))) +
           abs(getRawValue(center + ivec2(1, 1)) - getRawValue(center - ivec2(1, 1))) * 0.5;
}

float gradientD2(ivec2 center) {
    return abs(getRawValue(center + ivec2(-2, 2)) - getRawValue(center - ivec2(-2, 2))) +
           abs(getRawValue(center + ivec2(-1, 1)) - getRawValue(center - ivec2(-1, 1))) * 0.5;
}

float interpolateGreen(ivec2 coord) {
    float hGrad = gradientH(coord);
    float vGrad = gradientV(coord);
    float hVal = (getRawValue(coord + ivec2(1, 0)) + getRawValue(coord - ivec2(1, 0))) * 0.5;
    float vVal = (getRawValue(coord + ivec2(0, 1)) + getRawValue(coord - ivec2(0, 1))) * 0.5;
    float threshold = 0.1;
    if (abs(hGrad - vGrad) < threshold) {
        return (hVal + vVal) * 0.5;
    } else if (hGrad < vGrad) {
        return mix(hVal, vVal, 0.25);
    } else {
        return mix(hVal, vVal, 0.75);
    }
}

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

    // GRBG pattern
    if (patternX == 1 && patternY == 0) {
        r = getRawValue(ivec2(x, y));
        g = interpolateGreen(ivec2(x, y));
        b = interpolateRB(ivec2(x, y), false);
    } else if (patternX == 0 && patternY == 0) {
        g = getRawValue(ivec2(x, y));
        r = (getRawValue(ivec2(x+1, y)) + getRawValue(ivec2(x-1, y))) * 0.5;
        b = (getRawValue(ivec2(x, y+1)) + getRawValue(ivec2(x, y-1))) * 0.5;
    } else if (patternX == 1 && patternY == 1) {
        g = getRawValue(ivec2(x, y));
        b = (getRawValue(ivec2(x+1, y)) + getRawValue(ivec2(x-1, y))) * 0.5;
        r = (getRawValue(ivec2(x, y+1)) + getRawValue(ivec2(x, y-1))) * 0.5;
    } else {
        b = getRawValue(ivec2(x, y));
        g = interpolateGreen(ivec2(x, y));
        r = interpolateRB(ivec2(x, y), true);
    }

    float bl = blackLevel.r;
    float wl = whiteLevel;

    r = clamp((r - bl) / (wl - bl), 0.0, 1.0);
    g = clamp((g - bl) / (wl - bl), 0.0, 1.0);
    b = clamp((b - bl) / (wl - bl), 0.0, 1.0);

    vec3 rgb = vec3(r, g, b);
    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
    vec3 chroma = rgb - vec3(luma);
    rgb = vec3(luma) + chroma * 1.05;

    gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), 1.0);
}
