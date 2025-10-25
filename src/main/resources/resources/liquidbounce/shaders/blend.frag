#version 410 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D texture0;
uniform vec4 mixColor;

void main() {
    vec4 color = textureLod(texture0, fragTexCoord.xy, 0.0);
    fragColor = vec4(mixColor.rgb * mixColor.a + color.rgb * (1.0 - mixColor.a), 1.0);
}
