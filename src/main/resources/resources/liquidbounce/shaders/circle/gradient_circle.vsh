#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in float LineWidth;

out vec2 vUv;
flat out ivec2 vOuterPacked;
flat out ivec2 vInnerPacked;
out float vInnerRatio;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vUv = UV0;
    vOuterPacked = UV1;
    vInnerPacked = UV2;
    vInnerRatio = clamp(LineWidth, 0.0, 1.0);
}
