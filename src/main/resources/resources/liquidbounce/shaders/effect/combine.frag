#version 330
#extension GL_ARB_separate_shader_objects : require

// Styled glow of COUNT modes in straight alpha, the first one on top
uniform sampler2D In0Sampler;
uniform sampler2D In1Sampler;
#if COUNT > 2
uniform sampler2D In2Sampler;
#endif
#if COUNT > 3
uniform sampler2D In3Sampler;
#endif

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec4 over(vec4 top, vec4 below) {
    float alpha = top.a + below.a * (1.0 - top.a);
    if (alpha <= 0.0) {
        return vec4(0.0);
    }
    return vec4((top.rgb * top.a + below.rgb * below.a * (1.0 - top.a)) / alpha, alpha);
}

void main() {
#if COUNT > 3
    vec4 color = over(texture(In2Sampler, texCoord), texture(In3Sampler, texCoord));
#elif COUNT > 2
    vec4 color = texture(In2Sampler, texCoord);
#else
    vec4 color = vec4(0.0);
#endif
    color = over(texture(In1Sampler, texCoord), color);
    fragColor = over(texture(In0Sampler, texCoord), color);
}
