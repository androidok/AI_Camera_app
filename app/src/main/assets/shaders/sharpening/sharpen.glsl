precision mediump float;

varying vec2 vTexCoord;
uniform sampler2D InputBuffer;
uniform float strength;
uniform ivec2 size;

#define MSIZE1 5
#define depthMin 0.006
#define depthMax 0.890

float normpdf(in float x, in float sigma) {
    return 0.39894 * exp(-0.5 * x * x / (sigma * sigma)) / sigma;
}

vec3 blur(ivec2 coords, sampler2D tex) {
    vec3 mask = vec3(0.0);
    const int kSize = (MSIZE1 - 1) / 2;
    float kernel[MSIZE1];
    float pdfsize = 0.0;
    
    for (int j = 0; j <= kSize; ++j) {
        kernel[kSize + j] = kernel[kSize - j] = normpdf(float(j), 1.1);
    }
    
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

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    vec3 cur = texelFetch(InputBuffer, xy, 0).rgb;
    
    vec3 mask = vec3(0.0);
    float pdfsize = 0.0;
    const int kSize = (MSIZE1 - 1) / 2;
    float kernel[MSIZE1];
    
    for (int j = 0; j <= kSize; ++j) {
        kernel[kSize + j] = kernel[kSize - j] = normpdf(float(j), 2.2);
    }
    
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
    
    if (abs(mask.r + mask.b + mask.g) < depthMin) {
        mask *= 0.0;
    }
    
    cur += (mask.r + mask.g + mask.b) * (strength * 4.0 / 3.0);
    
    gl_FragColor = vec4(clamp(cur, 0.0, 1.0), 1.0);
}
