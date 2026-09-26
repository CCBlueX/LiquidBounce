#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in float alphaFactor;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(ColorModulator.rgb, ColorModulator.a * alphaFactor);
}
