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
layout(location = 3) in vec2 in_ObjCoord;
layout(location = 4) in vec4 in_Data0;
layout(location = 5) in vec4 in_Data1;
layout(location = 6) in vec4 in_Data2;
layout(location = 7) in vec4 in_Data3;
layout(location = 8) in vec4 in_Data4;
layout(location = 9) in vec4 in_Data5;
layout(location = 10) in vec4 in_Data6;

layout(location = 0) out vec4 ex_Color;
layout(location = 1) out vec2 ex_TexCoord;
layout(location = 2) out vec2 ex_ObjectCoord;
layout(location = 3) out vec4 ex_Data0;
layout(location = 4) out vec4 ex_Data1;
layout(location = 5) out vec4 ex_Data2;
layout(location = 6) out vec4 ex_Data3;
layout(location = 7) out vec4 ex_Data4;
layout(location = 8) out vec4 ex_Data5;
layout(location = 9) out vec4 ex_Data6;

void main() {
  ex_ObjectCoord = in_ObjCoord;
  gl_Position = Transform * vec4(in_Position, 0.0, 1.0);
  ex_Color = in_Color;
  ex_TexCoord = in_TexCoord;
  ex_Data0 = in_Data0;
  ex_Data1 = in_Data1;
  ex_Data2 = in_Data2;
  ex_Data3 = in_Data3;
  ex_Data4 = in_Data4;
  ex_Data5 = in_Data5;
  ex_Data6 = in_Data6;
}
