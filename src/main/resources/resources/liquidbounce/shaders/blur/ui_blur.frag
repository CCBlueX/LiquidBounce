#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D texture0;
uniform sampler2D overlay;
layout(std140) uniform BlurData {
    float radius;
    float alphaBlendMin;
    float alphaBlendMax;
};

const vec2 BlurDir = vec2(1.2, 0.8);
const vec2 BlurDirPerp = vec2(-BlurDir.y, BlurDir.x);

void main() {
    vec4 overlay_color = texture(overlay, texCoord);
    vec2 texelSize = vec2(1.0) / textureSize(texture0, 0).xy;

    // Almost transparent -> skip
    if (overlay_color.a <= 0.01) {
        fragColor = texture(texture0, texCoord);
        return;
    }

    float a = overlay_color.a;
    float range = alphaBlendMax - alphaBlendMin;
    float opacity = range > 0.0
    ? clamp((a - alphaBlendMin) / range, 0.0, 1.0)
    : 1.0;
    opacity = clamp(opacity, 0.1, 1.0);

    vec4 origColor = texture(texture0, texCoord);

    // bidirectional blur
    vec4 blurred = vec4(0.0);
    float totalStrength = 0.0;
    float step = max(1.0, radius * 0.1);
    for (float r = -radius; r <= radius; r += step) {
        vec4 sample1 = texture(texture0, texCoord + texelSize * r * BlurDir);
        vec4 sample2 = texture(texture0, texCoord + texelSize * r * BlurDirPerp);
        float strength = 1.0 - abs(r / radius);
        totalStrength += strength * 2.0;
        blurred += (sample1 + sample2) * strength;
    }
    vec3 blurResult = blurred.rgb / totalStrength;
    float blurAlpha = blurred.a / totalStrength;

    // Mix color
    fragColor.rgb = mix(origColor.rgb, blurResult, opacity);
    fragColor.a = mix(origColor.a, blurAlpha, opacity);
}
