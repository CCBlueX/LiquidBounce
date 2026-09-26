/**
 * Plain PosTex
 * No ProjMat/ModelViewMat
 */
#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

layout(location = 0) out vec2 texCoord0;

void main() {
    gl_Position = vec4(Position, 1.0);
    texCoord0 = UV0;
}
