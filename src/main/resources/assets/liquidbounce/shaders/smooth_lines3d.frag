#version 150

in vec4 vertex_color;
in vec2 vertex_line_center;

out vec4 frag_color;

uniform float line_width;

const float blend_factor = 1.5;

void main() {
    vec4 col = vertex_color;

    float d = length(vertex_line_center - gl_FragCoord.xy);
    float w = line_width;

    // FIXME: This does not work

    /*if (d > w)
        col.w = 0;
    else
        col.w *= pow(float((w - d) / w), blend_factor);*/

    frag_color = col;
}
