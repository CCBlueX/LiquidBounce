#version 410 core

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 fragTexCoord;
in vec4 fragColor;

out vec4 finalColor;

void main() {
    vec4 texColor = texture(Sampler0, fragTexCoord);
    if (texColor.a == 0.0) {
        discard;
    }
    texColor.rgb = texColor.bgr;
    finalColor = texColor * fragColor * ColorModulator;
}
