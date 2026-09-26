#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord0;
layout(location = 0) out vec4 fragColor;

uniform sampler2D texture0;

void main() {
    fragColor = textureLod(texture0, texCoord0.xy, 0.0);
}
