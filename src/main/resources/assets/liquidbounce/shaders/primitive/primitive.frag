#version 150

in vec4 vertex_color;
out vec4 frag_color;

void main() {
    frag_color = vertex_color;
}