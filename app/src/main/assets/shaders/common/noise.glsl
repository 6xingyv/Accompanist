#version 300 es
precision highp float;

float gradientNoise(in vec2 uv) {
    return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
}

float generateNoise(float value) {
    return fract(sin(value) * 43758.5453);
}

float perlinNoise(vec2 coords) {
    vec2 integerPart = floor(coords);
    vec2 fractionalPart = fract(coords);
    fractionalPart = fractionalPart * fractionalPart * (3.0 - 2.0 * fractionalPart);
    float dotProduct = integerPart.x + integerPart.y * 57.0;
    return mix(
        mix(generateNoise(dotProduct), generateNoise(dotProduct + 1.0), fractionalPart.x),
        mix(generateNoise(dotProduct + 57.0), generateNoise(dotProduct + 58.0), fractionalPart.x),
        fractionalPart.y
    );
}
