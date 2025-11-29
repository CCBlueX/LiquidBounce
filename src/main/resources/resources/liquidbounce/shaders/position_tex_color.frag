#version 410

uniform sampler2D Sampler0;
// see mc position_tex_color.fsh
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
    float LineWidth;
};

in vec2 fragTexCoord;
in vec4 fragColor;

out vec4 finalColor;

void main() {
    vec4 texColor = texture(Sampler0, fragTexCoord);
    if (texColor.a == 0.0) {
        discard;
    }
    finalColor = texColor * fragColor * ColorModulator;
}
