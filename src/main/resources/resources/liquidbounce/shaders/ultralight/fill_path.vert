#version 330
#extension GL_ARB_separate_shader_objects : require

// Ported from the GLSL shaders of Ultralight's AppCore (https://github.com/ultralight-ux/AppCore, tag v1.4.0b),
// which are licensed under the GNU Lesser General Public License 2.1.
//
// Colour textures hold BGRA pixels, which Ultralight produces, so they are swizzled when sampled and written.

layout(std140) uniform UltralightState {
    vec4 State;
    mat4 Transform;
    vec4 Scalar4[2];
    vec4 Vector[8];
    uint ClipSize;
    mat4 Clip[8];
};

layout(location = 0) in vec2 in_Position;
layout(location = 1) in vec4 in_Color;
layout(location = 2) in vec2 in_TexCoord;

layout(location = 0) out vec4 ex_Color;
layout(location = 1) out vec2 ex_ObjectCoord;

void main() {
  ex_ObjectCoord = in_TexCoord;
  gl_Position = Transform * vec4(in_Position, 0.0, 1.0);
  ex_Color = in_Color;
}
