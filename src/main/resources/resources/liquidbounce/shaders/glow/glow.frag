/**
 * Author: ccetl
 * Created: 2024
 * License: GPL-3.0
 */
#version 410 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D texture0;

uniform sampler2D image;
uniform int useImage;

uniform float alpha;
uniform vec4 blendColor;

uniform float sampleMul;
uniform vec4 glowColor;
uniform float falloff;
uniform int layerCount;

vec4 getFinalColor(vec4 color) {
    if (blendColor.a == 0.0) {
        return vec4(color.rgb, alpha);
    }

    return vec4((color.rgb * vec3(1.0 - blendColor.a)) + (blendColor.rgb * vec3(blendColor.a)), alpha);
}

void main() {
    vec2 uv = fragTexCoord.xy;
    vec2 pos = gl_FragCoord.xy;

    vec4 color = textureLod(texture0, uv, 0.0);
    if (color.a != 0.0) {
        if (useImage == 1) {
            fragColor = getFinalColor(textureLod(image, uv, 0.0));
            return;
        }

        fragColor = getFinalColor(color);
        return;
    }

    if (glowColor.a == 0.0) {
        discard;
    }

    vec2 texelSize = vec2(1.0) / textureSize(texture0, 0).xy;
    float alpha = 0.0;

    for (int ix = -layerCount; ix <= layerCount; ix++) {
        for (int iy = -layerCount; iy <= layerCount; iy++) {
            if (ix == 0 && iy == 0) {
                continue;
            }

            float x = sampleMul * float(ix);
            float y = sampleMul * float(iy);

            float distance = length(vec2(x, y));
            float weight = max(0.0, 1.0 - (distance / falloff));
            vec2 offset = vec2(texelSize.x * x, texelSize.y * y);
            float positionAlpha = textureLod(texture0, uv + offset, 0.0).a;

            alpha += positionAlpha * weight;
        }
    }

    if (alpha == 0.0) {
        discard;
    }

    vec4 outColor = vec4(glowColor.rgb, alpha * glowColor.a);
    fragColor = outColor;
}
