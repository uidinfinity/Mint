#version 330 core

in vec2 texCoord;
in vec2 oneTexel;

uniform sampler2D InSampler;

layout(std140) uniform GlowConfig {
    int shapeMode;
    int glowStrength;
    float glowMultiplier;
    int glowQuality;
    float fillOpacity;
};

out vec4 color;

struct BlurResult {
    float strength;
    vec3 color;
};

BlurResult computeBlur() {
    int w = glowQuality * glowStrength;
    float alphaSum = 0.0;
    vec3 rgbSum = vec3(0.0);

    float stepSize = float(glowQuality);
    float wFloat = float(w);

    for (float x = -wFloat; x <= wFloat; x += stepSize) {
        for (float y = -wFloat; y <= wFloat; y += stepSize) {
            vec2 offset = oneTexel * vec2(x, y);
            vec4 sample = texture(InSampler, texCoord + offset);
            float a = sample.a;

            alphaSum += a;
            rgbSum += sample.rgb * a;
        }
    }

    float normalization = float(((glowStrength * glowStrength) + glowStrength) * 4);
    float blurStrength = clamp(alphaSum / normalization, 0.0, 1.0) * glowMultiplier;

    vec3 blurColor = (alphaSum > 0.0) ? (rgbSum / alphaSum) : vec3(0.0);

    return BlurResult(blurStrength, blurColor);
}

void main() {
    vec4 center = texture(InSampler, texCoord);

    if (center.a > 0.0) {
        if (shapeMode == 1) discard;

        vec4 finalColor = vec4(center.rgb, center.a * fillOpacity);

        if (glowStrength > 0 && glowMultiplier > 0.0) {
            BlurResult blur = computeBlur();
            finalColor = mix(finalColor, vec4(center.rgb, 1.0), glowMultiplier - blur.strength);
        }

        color = finalColor;
    } else {
        if (shapeMode == 0 || glowStrength == 0) discard;

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                if (dx == 0 && dy == 0) continue;
                vec4 neighbor = texture(InSampler, texCoord + oneTexel * vec2(dx, dy));
                if (neighbor.a > 0.0) {
                    color = vec4(neighbor.rgb, 1.0);
                    return;
                }
            }
        }

        BlurResult blur = computeBlur();
        if (blur.strength == 0.0) discard;

        color = vec4(blur.color, blur.strength);
    }
}