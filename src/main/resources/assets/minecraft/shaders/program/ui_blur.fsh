#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D Overlay;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform float Radius;

out vec4 fragColor;

const vec2 BlurDir = vec2(1.2, 0.8);

void main() {
    vec4 overlay_color = texture(Overlay, texCoord);
    if (overlay_color.a <= 0.01) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);

        return;
    }

    float opacity = clamp((overlay_color.a - 0.1) * 2.0, 0.1, 1.0);

    vec4 origColor = texture(DiffuseSampler, texCoord);

    vec4 blurred = vec4(0.0);
    float totalStrength = 0.0;
    float totalAlpha = 0.0;
    float totalSamples = 0.0;
    for(float r = -Radius; r <= Radius; r += 1.0) {
        vec4 sampleValue = texture(DiffuseSampler, texCoord + oneTexel * r * BlurDir);

        // Accumulate average alpha
        totalAlpha = totalAlpha + sampleValue.a;
        totalSamples = totalSamples + 1.0;

        // Accumulate smoothed blur
        float strength = 1.0 - abs(r / Radius);
        totalStrength = totalStrength + strength;
        blurred = blurred + sampleValue;
    }
    fragColor = vec4(mix(origColor.rgb, blurred.rgb / (Radius * 2.0 + 1.0), opacity), 1.0);

}

