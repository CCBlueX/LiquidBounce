/**
 * Modified `core/position_color`
 * Applies camera position offset to vertex positions
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


in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

void main() {
    vec3 relativePos = Position - vec3(CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(relativePos, 1.0);

    vertexColor = Color;
}
