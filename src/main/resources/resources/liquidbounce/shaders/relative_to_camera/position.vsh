/**
 * Modified `core/position_color`
 * Applies camera position offset to vertex positions
 * Without Color input
 *
 * @see net.minecraft.client.renderer.GlobalSettingsUniform
 */
#version 330

/* #moj_import <minecraft:dynamictransforms.glsl> */
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
/* #moj_import <minecraft:projection.glsl> */
layout(std140) uniform Projection {
    mat4 ProjMat;
};
/* #moj_import <minecraft:globals.glsl> */
layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

layout(std140) uniform u_DistanceFade {
    // x = nearStart
    // y = nearEnd
    // z = farStart
    // w = farEnd
    vec4 DistanceRanges;
};

in vec3 Position;

out float alphaFactor;

void main() {
    vec3 cameraPos = vec3(CameraBlockPos) - CameraOffset;
    vec3 relativePos = Position - cameraPos;
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
    alphaFactor = nearFade * farFade;
}
