#version 120

uniform sampler2D texture;
uniform vec2 texelSize;

uniform vec4 color;
uniform float radius;

void main(void) {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].st);

    if(centerCol.a != 0F)
        return;

    for(float x = -radius; x <= radius; x++) {
        for(float y = -radius; y <= radius; y++) {
            vec4 currentColor = texture2D(texture, gl_TexCoord[0].st + vec2(texelSize.x * x, texelSize.y * y));

            if(currentColor.a != 0F) {
                gl_FragColor = color;
            }
        }
    }
}