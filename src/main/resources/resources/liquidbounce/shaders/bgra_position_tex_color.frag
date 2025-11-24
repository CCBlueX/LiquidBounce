#version 410

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    if (texColor.a == 0.0) {
        discard;
    }
    texColor.rgb = texColor.bgr;
    fragColor = texColor * vertexColor * ColorModulator;
}
