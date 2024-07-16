#version 300 es
precision highp float;

float computeSine(float value) {
    return sin(value);
}

float computeShiftedSine(float value) {
    return sin(value + acos(0.0));
}

mat2 rotationMatrix(float angle) {
    return mat2(
    computeShiftedSine(angle), -computeSine(angle),
    computeSine(angle), computeShiftedSine(angle)
    );
}

mat2 inverseRotationMatrix(float angle) {
    return mat2(
    computeSine(angle), -computeShiftedSine(angle),
    computeShiftedSine(angle), computeSine(angle)
    );
}

vec2 noiseFunction(vec2 coords, float frequency, vec2 offset, float noiseFactor) {
    float noiseValue = perlinNoise(coords * vec2(1.0) * 3.5 + 0.1) * 4.2831 * mix(0.2, 1.0, noiseFactor);
    coords += vec2(-1.0, 0.5);
    noiseValue -= frequency;
    return vec2(computeShiftedSine(noiseValue), computeSine(noiseValue));
}
