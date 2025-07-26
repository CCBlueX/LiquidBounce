#version 410 core

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 fragTexCoord;
out vec4 fragColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    fragTexCoord = UV0;
    fragColor = Color;
}
