#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord0;
layout(location = 0) out vec4 fragColor;

uniform sampler2D texture0;
layout(std140) uniform BlendData {
    vec4 mixColor;
};

void main() {
    vec4 color = textureLod(texture0, texCoord0.xy, 0.0);
    fragColor = vec4(mixColor.rgb * mixColor.a + color.rgb * (1.0 - mixColor.a), 1.0);
}
