#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:effect/effect.glsl>

// Silhouettes in their color, premultiplied where translucent geometry was blended in
uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

// The nearest covered pixel in the same row as packSilhouette, its distance in whole pixels / 255 in alpha
void main() {
    ivec2 size = textureSize(InSampler, 0);
    ivec2 p = pixelAt(texCoord, size);

    vec4 center = texelFetch(InSampler, p, 0);
    if (center.a > 0.0) {
        fragColor = vec4(packSilhouette(center), 0.0);
        return;
    }

    int range = int(ceil(Range));
    for (int i = 1; i <= range; i++) {
        if (p.x - i >= 0) {
            vec4 left = texelFetch(InSampler, ivec2(p.x - i, p.y), 0);
            if (left.a > 0.0) {
                fragColor = vec4(packSilhouette(left), float(i) / 255.0);
                return;
            }
        }

        if (p.x + i < size.x) {
            vec4 right = texelFetch(InSampler, ivec2(p.x + i, p.y), 0);
            if (right.a > 0.0) {
                fragColor = vec4(packSilhouette(right), float(i) / 255.0);
                return;
            }
        }
    }

    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
}
