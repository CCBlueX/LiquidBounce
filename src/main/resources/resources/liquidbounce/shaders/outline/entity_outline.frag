/**
 * Author: ccetl
 * Created: 2024
 * License: GPL-3.0
 */
#version 410 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D texture0;

void main() {
    vec2 uv = fragTexCoord.xy;

    vec4 color = textureLod(texture0, uv, 0.0);
    if (color.a != 0.0) {
        // inside of the entity
        discard;
    }

    vec2 texelSize = vec2(1.0) / textureSize(texture0, 0).xy;
    vec3 outColor = vec3(0.0);
    float outAlpha = 0.0;
    int iterations = 0;

    for (int ix = -3; ix <= 3; ix++) {
        for (int iy = -3; iy <= 3; iy++) {
            if (ix == 0 && iy == 0) {
                continue;
            }

            // 1.0 could changed to some higher value to improve the shader look
            // like in the item chams shader
            float x = 1.0 * float(ix);
            float y = 1.0 * float(iy);

            vec2 offset = vec2(texelSize.x * x, texelSize.y * y);
            vec4 positionColor = textureLod(texture0, uv + offset, 0.0);

            float distance = length(vec2(x, y));
            float weight = max(0.0, 1.0 - (distance / 7.08));
            outAlpha += positionColor.a * weight;
            if (positionColor.a != 0.0) {
                outColor += positionColor.rgb;
                iterations++;
            }
        }
    }

    if (outAlpha == 0.0 || iterations == 0) {
        discard;
    }

    fragColor = vec4(outColor / iterations, outAlpha);
}
