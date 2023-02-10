#version 120

uniform sampler2D texture;
uniform vec2 texelSize;

uniform vec3 color;
uniform int radius;
uniform float fade;
uniform float targetAlpha;

void main() {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);

    if(centerCol.a != 0) {
        gl_FragColor = vec4(centerCol.rgb, targetAlpha);
    } else {

        float alpha = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                vec4 currentColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));
                int distanceSquared = x * x + y * y;
                if (currentColor.a != 0) {
                    if(fade > 0) {
                        alpha += max(0, (radius - sqrt(distanceSquared)) / radius);
                    } else {
                        alpha +=  1;
                    }
                }
            }
        }
        alpha /= fade;
        gl_FragColor = vec4(color, alpha);
    }
}