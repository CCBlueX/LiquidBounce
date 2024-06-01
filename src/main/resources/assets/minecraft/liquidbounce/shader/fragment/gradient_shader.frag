#version 130

uniform float offset;
uniform vec2 strength;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform float speed;

float hermite(float edge0, float edge1, float x) {
    return edge0 + (edge1 - edge0) * (x * x * (3.0 - 2.0 * x));
}

void main() {
    vec2 pos = gl_FragCoord.xy * strength;
    float param = fract(pos.x + pos.y + offset * speed);

    // Divide the range [0, 1] into 4 segments
    float segment = 0.25;
    float index = param / segment;
    int idx1 = int(index);
    float frac = fract(index);

    vec4 gradientColor;

    if (idx1 == 0) {
        gradientColor = mix(color1, color2, hermite(0.0, 1.0, frac));
    } else if (idx1 == 1) {
        gradientColor = mix(color2, color3, hermite(0.0, 1.0, frac));
    } else if (idx1 == 2) {
        gradientColor = mix(color3, color4, hermite(0.0, 1.0, frac));
    } else {
        gradientColor = mix(color4, color1, hermite(0.0, 1.0, frac));
    }

    gl_FragColor = gradientColor;
}