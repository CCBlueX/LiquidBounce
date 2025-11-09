#version 410 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D UnblurredGameSampler;
uniform sampler2D BlurredGameSampler;
uniform sampler2D ClientHUDSampler;

uniform vec2 BlurRange;

void main() {
    vec4 overlay_color = texture(ClientHUDSampler, texCoord);
    vec4 unblurred_color = texture(UnblurredGameSampler, texCoord);
    vec4 blurred_color = texture(BlurredGameSampler, texCoord);

    float eased_overlay_alpha = clamp((overlay_color.a - BlurRange.x) / (BlurRange.y - BlurRange.x), 0.0, 1.0);

    // Blend between the blurred and unblurred background to get a smooth transition
    vec4 background_color = vec4(mix(unblurred_color.rgb, blurred_color.rgb, eased_overlay_alpha), 1.0);

    // Finally overlay the overlay over the game image
    fragColor = vec4(background_color.rgb * (1.0 - overlay_color.a) + overlay_color.rgb, 1.0);
}
