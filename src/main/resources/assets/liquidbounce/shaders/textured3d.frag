#version 150

in vec2 vertex_uv;
in vec4 vertex_color;
out vec4 frag_color;

uniform sampler2D current_texture;

void main() {
    frag_color = texture(current_texture, vertex_uv) * vertex_color;
}