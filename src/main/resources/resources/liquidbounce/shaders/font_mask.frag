#version 330 core

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float coverage = texture(Sampler0, texCoord0).r;
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * coverage);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
