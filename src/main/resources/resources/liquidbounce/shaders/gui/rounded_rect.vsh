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
in vec4 Color;
in ivec2 UV1;
in ivec2 UV2;
in float LineWidth;

out vec2 vUv;
out vec4 vColor;
flat out ivec2 vSize;
flat out ivec2 vParameters;
flat out float vStrokeWidth;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vUv = UV0;
    vColor = Color;
    vSize = UV1;
    vParameters = UV2;
    vStrokeWidth = max(LineWidth, 0.0);
}
