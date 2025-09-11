#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform sampler2D DiffuseSampler;

out vec4 fragColor;

void main() {
    vec4 tex = texture(DiffuseSampler, texCoord);
    fragColor = tex * vertexColor;
}
