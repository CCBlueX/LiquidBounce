#version 150

in vec3 in_pos;
in vec4 in_color;
in vec3 in_center;
in float in_scale;

out vec4 vertex_color;

uniform mat4 mvp_matrix;
uniform mat4 rotation_matrix;

void main() {
    vertex_color = in_color;

    mat4 translation_matrix;

    translation_matrix[0][0] = 1.0;
    translation_matrix[1][1] = 1.0;
    translation_matrix[2][2] = 1.0;
    translation_matrix[3][3] = 1.0;

    gl_Position = mvp_matrix * vec4(in_pos, 1.0);
}