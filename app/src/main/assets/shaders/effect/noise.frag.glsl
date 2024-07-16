#version 300 es
precision highp float;

uniform sampler2D src;
in vec2 f_v_coord;
out vec4 fragColor;

#include "../common/noise.glsl"

void main() {
    vec2 tex_coord = f_v_coord;
    float dither = (1.0 / 255.0) * gradientNoise(gl_FragCoord.xy) - (0.5 / 255.0);
    vec4 color = texture(src, tex_coord);
    color += dither;
    fragColor = color;
}
