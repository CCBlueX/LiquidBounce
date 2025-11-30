#version 330 core

uniform sampler2D Sampler0;

// see mc position_tex_color.fsh
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
    float LineWidth;
};

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    if (texColor.a == 0.0) {
        discard;
    }
    texColor.rgb = texColor.bgr;
    fragColor = texColor * vertexColor * ColorModulator;
}
