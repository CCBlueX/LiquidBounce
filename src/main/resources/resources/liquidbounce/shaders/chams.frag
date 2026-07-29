#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D entityColor;
uniform sampler2D entityDepth;
uniform sampler2D sceneDepth;
uniform sampler2D image;

layout(std140) uniform ChamsData {
    vec2 imageScale;
    vec2 imageOffset;
};

void main() {
    vec4 color = textureLod(entityColor, texCoord, 0.0);
    if (color.a == 0.0) {
        discard;
    }

    float entityDepthValue = textureLod(entityDepth, texCoord, 0.0).r;
    float sceneDepthValue = textureLod(sceneDepth, texCoord, 0.0).r;
    if (entityDepthValue > sceneDepthValue + 0.00001) {
        fragColor = color;
        return;
    }

    fragColor = textureLod(image, (gl_FragCoord.xy - imageOffset) / imageScale, 0.0);
}
