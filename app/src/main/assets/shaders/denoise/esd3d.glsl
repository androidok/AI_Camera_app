precision highp float;

varying vec2 vTexCoord;
uniform sampler2D InputBuffer;
uniform float noiseS;
uniform float noiseO;
uniform ivec2 size;
uniform int MSIZE;

#define KSIZE ((MSIZE-1)/2)
#define SIGMA 10.0
#define BSIGMA 0.1

float normpdf(in float x, in float sigma) {
    return 0.39894 * exp(-0.5 * x * x / (sigma * sigma)) / sigma;
}

float normpdf3(in vec3 v, in float sigma) {
    return 0.39894 * exp(-0.5 * dot(v, v) / (sigma * sigma)) / sigma;
}

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    vec3 cin = texelFetch(InputBuffer, xy, 0).rgb;
    
    float noisefactor = dot(cin, vec3(0.299, 0.587, 0.114));
    float sigY = max(noisefactor * noisefactor * noiseS + noiseO, 0.0000001);
    float sigX = 2.5;
    
    float kernel[25];
    for (int j = 0; j <= KSIZE; ++j) {
        kernel[KSIZE + j] = kernel[KSIZE - j] = normpdf(float(j), sigX);
    }
    
    vec3 final_color = vec3(0.0);
    float Z = 0.0;
    
    for (int i = -KSIZE; i <= KSIZE; ++i) {
        for (int j = -KSIZE; j <= KSIZE; ++j) {
            ivec2 coord = xy + ivec2(i, j);
            coord = clamp(coord, ivec2(0, 0), size - 1);
            
            vec3 cc = texelFetch(InputBuffer, coord, 0).rgb;
            float factor = normpdf3(cc - cin, sigY) * 
                          kernel[KSIZE + j] * kernel[KSIZE + i];
            Z += factor;
            final_color += factor * cc;
        }
    }
    
    if (Z > 0.0) {
        final_color = final_color / Z;
    } else {
        final_color = cin;
    }
    
    gl_FragColor = vec4(clamp(final_color, 0.0, 1.0), 1.0);
}
