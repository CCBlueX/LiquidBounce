#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:esp_glow/glow.glsl>

// Output of field_rows
uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float rowDistance(vec4 texel) {
    return texel.a >= 1.0 ? NONE : floor(texel.a * 255.0 + 0.5);
}

// Color and euclidean distance of the nearest covered pixel, see encodeField
void main() {
    ivec2 size = textureSize(InSampler, 0);
    ivec2 p = pixelAt(texCoord, size);

    vec4 center = texelFetch(InSampler, p, 0);
    float centerDistance = rowDistance(center);
    float best = centerDistance * centerDistance;
    vec3 color = center.rgb;

    int range = int(ceil(Range));
    for (int i = 1; i <= range; i++) {
        float dy2 = float(i * i);
        if (dy2 >= best) {
            break;
        }

        if (p.y + i < size.y) {
            vec4 up = texelFetch(InSampler, ivec2(p.x, p.y + i), 0);
            float dx = rowDistance(up);
            if (dx < NONE && dx * dx + dy2 < best) {
                best = dx * dx + dy2;
                color = up.rgb;
            }
        }

        if (p.y - i >= 0) {
            vec4 down = texelFetch(InSampler, ivec2(p.x, p.y - i), 0);
            float dx = rowDistance(down);
            if (dx < NONE && dx * dx + dy2 < best) {
                best = dx * dx + dy2;
                color = down.rgb;
            }
        }
    }

    float dist = sqrt(best);
    fragColor = encodeField(color, dist <= Range + 0.5 ? dist : NONE);
}
