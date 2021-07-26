#version 150

in vec3 in_pos;
in vec4 in_color;

out vec4 vertex_color;
out vec2 vertex_line_center;

uniform mat4 mvp_matrix;
uniform vec2 view_port;

void main(void) {
    vec4 pos = mvp_matrix * vec4(in_pos, 1.0);

    gl_Position = pos;

    vertex_line_center = 0.5 * (pos.xy + vec2(1, 1)) * view_port;
    vertex_color = in_color;
}
