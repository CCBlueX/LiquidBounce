#version 410 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D texture0;
uniform sampler2D overlay;
uniform float radius;

const vec2 BlurDir = vec2(1.2, 0.8);

void main() {
    vec4 overlay_color = texture(overlay, fragTexCoord);
    vec2 texelSize = vec2(1.0) / textureSize(texture0, 0).xy;

    if (overlay_color.a <= 0.01) {
        fragColor = vec4(texture(texture0, fragTexCoord).rgb, 1.0);
        return;
    }

    float opacity = clamp((overlay_color.a - 0.1) * 2.0, 0.1, 1.0);

    vec4 origColor = texture(texture0, fragTexCoord);

    vec4 blurred = vec4(0.0);
    float totalStrength = 0.0;
    float totalAlpha = 0.0;
    float totalSamples = 0.0;
    for(float r = -radius; r <= radius; r += 1.0) {
        vec4 sampleValue = texture(texture0, fragTexCoord + texelSize * r * BlurDir);

        // Accumulate average alpha
        totalAlpha = totalAlpha + sampleValue.a;
        totalSamples = totalSamples + 1.0;

        // Accumulate smoothed blur
        float strength = 1.0 - abs(r / radius);
        totalStrength = totalStrength + strength;
        blurred = blurred + sampleValue;
    }

    fragColor = vec4(mix(origColor.rgb, blurred.rgb / (radius * 2.0 + 1.0), opacity), 1.0);
}
