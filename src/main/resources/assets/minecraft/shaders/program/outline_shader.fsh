#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform float radius;

void main(void) {
    vec4 centerCol = texture2D(DiffuseSampler, texCoord.xy);

    if (centerCol.a != 0) {
        discard;
    }

    for (float x = -radius; x <= radius; x++) {
        for (float y = -radius; y <= radius; y++) {
            vec4 currentColor = texture2D(DiffuseSampler, texCoord.xy + vec2(oneTexel.x * x, oneTexel.y * y));

            if (currentColor.a != 0) {
                gl_FragColor = currentColor;
                return;
            }
        }

    }
}
