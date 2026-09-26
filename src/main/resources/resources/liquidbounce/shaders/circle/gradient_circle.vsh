#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in ivec2 UV1;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in float LineWidth;

layout(location = 0) out vec2 vUv;
layout(location = 1) flat out ivec2 vOuterPacked;
layout(location = 2) flat out ivec2 vInnerPacked;
layout(location = 3) out float vInnerRatio;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vUv = UV0;
    vOuterPacked = UV1;
    vInnerPacked = UV2;
    vInnerRatio = clamp(LineWidth, 0.0, 1.0);
}
