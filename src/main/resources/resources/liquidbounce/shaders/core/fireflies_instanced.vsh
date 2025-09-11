#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

in vec3 InstancePos;
in float InstanceSize;
in vec4 InstanceColor;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord;
out vec4 vertexColor;

void main() {
    vec3 scaled = Position * InstanceSize + InstancePos;
    gl_Position = ProjMat * ModelViewMat * vec4(scaled, 1.0);

    texCoord = UV0;
    vertexColor = Color * InstanceColor;
}
