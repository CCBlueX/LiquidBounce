#version 420 core

precision highp float;

layout (location = 0) out vec4 FragColor;
layout (location = 0) in vec2 TextureCoord;

uniform sampler2D Texture;

void main() {
    // This fragment shader mostly takes care of the fact that the texture is upside down
    // and in BGRA format instead of RGBA.

    vec2 invertedTextureCoord = vec2(TextureCoord.x, 1.0 - TextureCoord.y);
    vec3 textureData = texture(Texture, invertedTextureCoord).bgr;

    FragColor = vec4(textureData, 1.0);
}
