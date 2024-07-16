#version 300 es
precision highp float;

uniform float noiseFactor;
uniform float time;
uniform float scaleFactor;
uniform sampler2D textureSampler;
uniform vec2 resolution;
uniform vec2 offset;
out vec4 fragColor;

#include "../common/noise.glsl"
#include "../common/utility.glsl"
#include "../common/constants.glsl"

void main() {
    vec2 fragCoord = computeSine(time * 0.2) * 0.01 + 0.5 + gl_FragCoord.xy / resolution.xy;
    vec2 textureCoord = fragCoord * 0.77 + offset;
    textureCoord.x *= resolution.x / resolution.y;

    float aspectRatio = gl_FragCoord.x / max(resolution.x, resolution.y);
    float weightSum = 0.0;
    vec3 colorSum = vec3(0.0);
    vec2 initialCoord = textureCoord;

    for (int i = 0; i < 8; ++i) {
        bool isEven = i % 2 == 0;
        vec2 noiseCoords = noiseFunction(textureCoord, aspectRatio, fragCoord, noiseFactor);
        float weight = float(i) / 8.0;
        vec3 sampledColor = texture(
        textureSampler,
        -time * 0.01 + 0.8 * textureCoord * rotationMatrix(-time * 0.2) +
        vec2(scaleFactor * 0.3 * (float(isEven) * 2.0 - 1.0) * inverseRotationMatrix(time * 0.2 + 1.5))
        ).xyz;

        colorSum += sampledColor;
        weightSum += 4.0 * weight * (1.0 - weight);
        textureCoord += 0.01 * noiseCoords * (computeSine(time) * 4.8 + 0.8);
        initialCoord += 0.05 * noiseCoords;
    }

    colorSum /= weightSum;
    colorSum *= 1.6;
    colorSum = 1.0 - exp(-colorSum);
    colorSum = pow(colorSum, vec3(2.2));
    colorSum *= 0.8;

    fragColor = vec4(colorSum, 1.0);
}
