#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

layout(location = 0) out vec2 texCoord;

void main() {
    gl_Position = vec4((vec4(Position.xy, 0.0, 1.0)).xy, 0.2, 1.0);
    texCoord = UV0;
}
