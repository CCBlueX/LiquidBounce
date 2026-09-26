/**
 * Modified `core/position_color`
 * Applies camera position offset to vertex positions
 *
 * @see net.minecraft.client.renderer.GlobalSettingsUniform
 */
#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:globals.glsl>

layout(std140) uniform u_MeshBaseBlockPos {
    ivec3 BaseBlockPos;
};

layout(std140) uniform u_DistanceFade {
    // x = nearStart
    // y = nearEnd
    // z = farStart
    // w = farEnd
    vec4 DistanceRanges;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(location = 0) out vec4 vertexColor;

void main() {
    vec3 relativePos = Position + vec3(BaseBlockPos - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(relativePos, 1.0);

    float dist = length(relativePos);
    // 0 -> 1
    float nearFade = smoothstep(
        DistanceRanges.x,
        DistanceRanges.y,
        dist
    );
    // 1 -> 0
    float farFade = 1.0 - smoothstep(
        DistanceRanges.z,
        DistanceRanges.w,
        dist
    );
    float alphaFactor = nearFade * farFade;
    vertexColor = vec4(Color.rgb, Color.a * alphaFactor);
}
