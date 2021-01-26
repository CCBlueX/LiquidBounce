#version 150

in vec3 in_pos;
in vec4 in_color;

in vec3 instance_pos;
in vec4 instance_color;

out vec4 vertex_color;

uniform mat4 mvp_matrix;

void main() {
    vertex_color = in_color * instance_color;

    gl_Position = mvp_matrix * vec4(in_pos + instance_pos, 1.0);
}