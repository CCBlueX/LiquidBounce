#version 330

// Uniforms
uniform float time;
uniform vec2 resolution;

// Output color
out vec4 fragColor;

// Simple hash function
float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

// 2D noise function
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
    mix(hash(i.x + hash(i.y)), hash(i.x + 1.0 + hash(i.y)), u.x),
    mix(hash(i.x + hash(i.y + 1.0)), hash(i.x + 1.0 + hash(i.y + 1.0)), u.x),
    u.y
    );
}

// Mountain range function
float mountainRange(vec2 uv) {
    float mountainHeight = 0.0;
    float frequency = 2.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        mountainHeight += noise(uv * frequency) * amplitude;
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    return mountainHeight;
}

// Aurora layer function
vec3 auroraLayer(vec2 uv, float speed, float intensity, vec3 color) {
    float t = time * speed;
    vec2 scaleXY = vec2(2.0, 2.0);
    vec2 movement = vec2(2.0, -2.0);
    vec2 p = uv * scaleXY + t * movement;
    float n = noise(p + noise(color.xy + p + t));

    float topEdgeSharpness = 0.0; // The smaller this value, the crisper the edge
    float bottomFadeOut = 0.3;   // The higher this value, the more solid the aurora appears
    float aurora = smoothstep(0.0, topEdgeSharpness, n - uv.y) * (1.0 - smoothstep(0.0, bottomFadeOut, n - uv.y));

    aurora = (n - uv.y * 0.6);

    return aurora * intensity * color * 0.5;
}

// Main image function
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / resolution.xy;
    uv.x *= resolution.x / resolution.y;

    // Create multiple aurora layers with varying colors, speeds, and intensities
    vec3 color = vec3(0.0);
    color += auroraLayer(uv, 0.05, 0.3, vec3(0.0, 1.0, 0.3));
    color += auroraLayer(uv, 0.1, 0.4, vec3(0.1, 0.5, 0.9));
    color += auroraLayer(uv, 0.15, 0.3, vec3(0.4, 0.1, 0.8));
    color += auroraLayer(uv, 0.07, 0.2, vec3(0.8, 0.1, 0.6));

    vec3 skyColor1 = vec3(0.2, 0.0, 0.4);
    vec3 skyColor2 = vec3(0.15, 0.2, 0.35);
    // Add a gradient to simulate the night sky
    color += skyColor2 * (1.0 - smoothstep(1.0, 1.0, uv.y));
    color += skyColor1 * (1.0 - smoothstep(0.0, 0.5, uv.y));

    int numLayers = 5;
    for (int i = 0; i < numLayers; i++) {
        // Calculate the height of the mountain range
        float height = float(numLayers - i) * 0.1 *
        smoothstep(1.0, 0.0,
        mountainRange(
        vec2(time * 0.03 * (float(i) + 1.0) + float(i) * 4.0, 0.0)
        + uv * vec2(1.0 + float(numLayers - i) * 0.05, 0.23)
        ));

        // Create the black silhouette of the mountain range
        float mountain = smoothstep(0.0, 0.0, height - uv.y);

        // Combine the mountain range and sky
        color = mix(color, skyColor2 * float(numLayers - i) / 4.0, mountain);
    }

    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(fragColor, gl_FragCoord.xy);
}
