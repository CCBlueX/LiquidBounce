#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D Overlay;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

out vec4 fragColor;

const vec2 BlurDir = vec2(1.0, 1.0);

void main() {
    vec4 overlay_color = texture(Overlay, texCoord);
    if (overlay_color.a < 0.1) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);

        return;
    }

    // This should cause terrible performance, but it doesn't (on my nvidia-based machine at least)
    float Radius = min(10.0, (overlay_color.a - 0.1) * 15.0);

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
    fragColor = vec4(blurred.rgb / (Radius * 2.0 + 1.0), 1.0);

}

