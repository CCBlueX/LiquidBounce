#version 150

in vec3 in_pos;
in vec4 in_color;
in vec2 in_uv;

out vec2 vertex_uv;
out vec4 vertex_color;

uniform mat4 mvp_matrix;

void main() {
    vertex_color = in_color;
    vertex_uv = in_uv;

    gl_Position = mvp_matrix * vec4(in_pos, 1.0);
}