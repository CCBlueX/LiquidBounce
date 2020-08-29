#version 130
uniform float offset;
uniform vec2 strength;

void main() {
    vec2 tmpvar_1 = (gl_FragCoord.xy * strength);
    vec4 tmpvar_2 = vec4(clamp ((abs(
    ((fract((vec3(
    (float(mod (((tmpvar_1.x + tmpvar_1.y) + offset), 1.0)))
    ) + vec3(1.0, 0.6666667, 0.3333333))) * 6.0) - vec3(3.0, 3.0, 3.0))
    ) - vec3(1.0, 1.0, 1.0)), 0.0, 1.0), 1.0);
    gl_FragColor = tmpvar_2;
}

