#version 410 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D texture0;

void main() {
    fragColor = textureLod(texture0, fragTexCoord.xy, 0.0);
}
