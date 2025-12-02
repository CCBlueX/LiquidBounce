/**
 * Plain PosTex
 * No ProjMat/ModelViewMat
 */
#version 330 core

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;

void main() {
    gl_Position = vec4(Position, 1.0);
    texCoord0 = UV0;
}
