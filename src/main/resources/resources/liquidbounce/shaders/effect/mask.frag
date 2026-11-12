#version 330
#extension GL_ARB_separate_shader_objects : require

// Glow silhouettes of several modes in the entity outline target, each tagged in the lowest bits of blue
uniform sampler2D InSampler;

layout(std140) uniform EffectMask {
    // Tag kept, and with Untagged above 0 pixels without a tag as well
    int Tag;
    int Untagged;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texel = texelFetch(InSampler, ivec2(texCoord * vec2(textureSize(InSampler, 0))), 0);
    int tag = int(texel.b * 255.0 + 0.5) & 7;
    bool kept = texel.a > 0.0 && (tag == Tag || (Untagged > 0 && tag == 0));
    fragColor = kept ? texel : vec4(0.0);
}
