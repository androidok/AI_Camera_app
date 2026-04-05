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

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    
    int x = coord.x;
    int y = coord.y;
    
    int patternX = x % 2;
    int patternY = y % 2;
    
    float r, g, b;
    
    if (patternX == 1 && patternY == 1) {
        b = getRawValue(ivec2(x, y));
        g = (getRawValue(ivec2(x+1, y)) + getRawValue(ivec2(x-1, y)) +
             getRawValue(ivec2(x, y+1)) + getRawValue(ivec2(x, y-1))) * 0.25;
        r = (getRawValue(ivec2(x+1, y+1)) + getRawValue(ivec2(x-1, y+1)) +
             getRawValue(ivec2(x+1, y-1)) + getRawValue(ivec2(x-1, y-1))) * 0.25;
    } else if (patternX == 0 && patternY == 1) {
        g = getRawValue(ivec2(x, y));
        b = (getRawValue(ivec2(x+1, y)) + getRawValue(ivec2(x-1, y))) * 0.5;
        r = (getRawValue(ivec2(x, y+1)) + getRawValue(ivec2(x, y-1))) * 0.5;
    } else if (patternX == 1 && patternY == 0) {
        g = getRawValue(ivec2(x, y));
        r = (getRawValue(ivec2(x+1, y)) + getRawValue(ivec2(x-1, y))) * 0.5;
        b = (getRawValue(ivec2(x, y+1)) + getRawValue(ivec2(x, y-1))) * 0.5;
    } else {
        r = getRawValue(ivec2(x, y));
        g = (getRawValue(ivec2(x+1, y)) + getRawValue(ivec2(x-1, y)) +
             getRawValue(ivec2(x, y+1)) + getRawValue(ivec2(x, y-1))) * 0.25;
        b = (getRawValue(ivec2(x+1, y+1)) + getRawValue(ivec2(x-1, y+1)) +
             getRawValue(ivec2(x+1, y-1)) + getRawValue(ivec2(x-1, y-1))) * 0.25;
    }
    
    float bl = blackLevel.r;
    float wl = whiteLevel;
    
    r = clamp((r - bl) / (wl - bl), 0.0, 1.0);
    g = clamp((g - bl) / (wl - bl), 0.0, 1.0);
    b = clamp((b - bl) / (wl - bl), 0.0, 1.0);
    
    gl_FragColor = vec4(r, g, b, 1.0);
}
