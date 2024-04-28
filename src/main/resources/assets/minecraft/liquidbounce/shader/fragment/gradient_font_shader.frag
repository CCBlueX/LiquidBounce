#version 130

uniform float offset;
uniform vec2 strength;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform sampler2D font_texture;

float hermite(float edge0, float edge1, float x) {
    return edge0 + (edge1 - edge0) * (x * x * (3.0 - 2.0 * x));
}

void main() {
    vec4 texColor = texture2D(font_texture, gl_TexCoord[0].st);

    if (texColor.a == 0.0)
    discard;

    vec2 pos = gl_FragCoord.xy * strength;
    float param = mod(pos.x + pos.y + offset, 1.0);

    float segment = 1.0 / 4.0;
    float index = param / segment;
    int idx1 = int(index);
    float frac = fract(index);

    vec4 gradientColor = mix(color1, color2, hermite(0.0, 1.0, frac));

    if (idx1 == 1) {
        gradientColor = mix(color2, color3, hermite(0.0, 1.0, frac));
    } else if (idx1 == 2) {
        gradientColor = mix(color3, color4, hermite(0.0, 1.0, frac));
    } else if (idx1 == 3) {
        gradientColor = mix(color4, color1, hermite(0.0, 1.0, frac));
    }

    gl_FragColor = vec4(gradientColor.rgb, texColor.a);
}