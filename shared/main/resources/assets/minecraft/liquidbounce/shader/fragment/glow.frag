#version 120

uniform sampler2D texture;
uniform vec2 texelSize;

uniform vec3 color;

uniform float radius;
uniform float divider;
uniform float maxSample;

void main() {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);

     if(centerCol.a != 0) {
         gl_FragColor = vec4(centerCol.rgb, 0);
     } else {

         float alpha = 0;

         for (float x = -radius; x < radius; x++) {
             for (float y = -radius; y < radius; y++) {
                 vec4 currentColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));

                 if (currentColor.a != 0)
                 alpha += divider > 0 ? max(0, (maxSample - distance(vec2(x, y), vec2(0))) / divider) : 1;
             }
         }
         gl_FragColor = vec4(color, alpha);
     }
}