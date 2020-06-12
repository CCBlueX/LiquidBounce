#version 130

uniform float offset;
uniform vec2 strength;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float rand(vec2 co){
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 color_vector = gl_FragCoord.xy * strength;

    gl_FragColor = vec4(hsv2rgb(vec3(mod(color_vector.x + color_vector.y + offset, 1.0), 1.0, 1.0)), 1.0);
}