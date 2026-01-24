#version 330 core

in vec2 texCoord0;
out vec4 fragColor;

uniform sampler2D texture0;

void main() {
    fragColor = textureLod(texture0, texCoord0.xy, 0.0);
}
