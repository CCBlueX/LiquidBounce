// Very simple pass-through shader, we don't need to
// manipulate the vertex positions in any way

#version 420 core

layout(location = 0) in vec3 VertexPosition;
layout(location = 0) out vec2 TextureCoords;

float map(float value, float min1, float max1, float min2, float max2) {
    return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
}

void main() {
    gl_Position = vec4(VertexPosition, 1.0);

    TextureCoords = vec2(map(VertexPosition.x, -1.0, 1.0, 0.0, 1.0), map(VertexPosition.y, -1.0, 1.0, 0.0, 1.0));
}
