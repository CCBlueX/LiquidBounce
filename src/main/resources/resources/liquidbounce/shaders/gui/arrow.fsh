#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec2 vLocal;
layout(location = 1) in vec4 vColor;
layout(location = 2) flat in ivec2 vSize;
layout(location = 3) flat in float vNotch;

layout(location = 0) out vec4 fragColor;

// The size is packed into shorts and has to travel as fixed point.
const float FIXED_POINT = 32.0;

/**
 * Exact signed distance to the polygon, negative inside. The arrow is concave, so its edges cannot be read as the
 * intersection of their half-planes.
 */
float polygonDistance(vec2 p, vec2 a, vec2 b, vec2 c, vec2 d) {
    vec2 vertices[4] = vec2[4](a, b, c, d);
    float closest = dot(p - a, p - a);
    float inside = 1.0;

    for (int i = 0; i < 4; i++) {
        int previous = (i + 3) & 3;
        vec2 edge = vertices[previous] - vertices[i];
        vec2 offset = p - vertices[i];
        vec2 toEdge = offset - edge * clamp(dot(offset, edge) / dot(edge, edge), 0.0, 1.0);
        closest = min(closest, dot(toEdge, toEdge));

        // A ray crossing this edge flips the winding, which tells whether the point ends up inside.
        bvec3 crosses = bvec3(
            p.y >= vertices[i].y,
            p.y < vertices[previous].y,
            edge.x * offset.y > edge.y * offset.x
        );
        if (all(crosses) || all(not(crosses))) {
            inside = -inside;
        }
    }

    return inside * sqrt(closest);
}

void main() {
    vec2 size = vec2(vSize) / FIXED_POINT;
    float halfWidth = size.x * 0.5;

    float shapeDistance = polygonDistance(
        vLocal,
        vec2(-halfWidth, 0.0), vec2(0.0, size.y), vec2(halfWidth, 0.0), vec2(0.0, vNotch)
    );

    // Local units per pixel, so the ramp follows the pose's rotation and scale on its own.
    float aa = max(fwidth(shapeDistance), 1e-4);

    vec4 color = vColor * ColorModulator;
    color.a *= 1.0 - smoothstep(-aa, 0.0, shapeDistance);
    if (color.a <= 0.001) {
        discard;
    }

    fragColor = color;
}
