#version 410 core

layout (location = 0) in vec4 Position;
layout (location = 1) in vec2 UV0;

out vec2 fragTexCoord;

void main() {
    vec4 outPos = vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);
    fragTexCoord = UV0;
}
