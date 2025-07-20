#version 410 core

in vec3 Position;
in vec2 UV0;

out vec2 fragTexCoord;

void main() {
    gl_Position = vec4((vec4(Position.xy, 0.0, 1.0)).xy, 0.2, 1.0);
    fragTexCoord = UV0;
}
