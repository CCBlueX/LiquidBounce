#version 410 core

in vec3 Position;
in vec2 UV0;

out vec2 fragTexCoord;

void main() {
    gl_Position = vec4(Position, 1.0);
    fragTexCoord = UV0;
}
