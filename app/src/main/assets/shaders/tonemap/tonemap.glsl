precision mediump float;

varying vec2 vTexCoord;
uniform sampler2D InputBuffer;
uniform float strength;

vec3 tonemap(vec3 x) {
    float A = 0.15;
    float B = 0.50;
    float C = 0.10;
    float D = 0.20;
    float E = 0.02;
    float F = 0.30;
    
    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}

void main() {
    vec3 color = texture2D(InputBuffer, vTexCoord).rgb;
    
    color *= 1.5;
    
    float exposureBias = 2.0;
    vec3 curr = tonemap(exposureBias * color);
    
    vec3 whiteScale = 1.0 / tonemap(vec3(11.2));
    color = curr * whiteScale;
    
    color = pow(color, vec3(1.0 / 2.2));
    
    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
