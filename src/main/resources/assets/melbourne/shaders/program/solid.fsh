#version 330

#define Width 1
#define MaxSample 3.0
#define Divider 2.5

uniform sampler2D InSampler;

in vec2 texCoord;
in vec2 oneTexel;

layout(std140) uniform SolidConfig {
    int shapeMode;
    float fillOpacity;
};

out vec4 color;


void main() {
    vec4 center = texture(InSampler, texCoord);

    if (center.a != 0.0) {
        if (shapeMode == 1) discard;

        color = vec4(center.rgb, center.a * fillOpacity);
    } else {
        if (shapeMode == 0) discard;

        float alpha = 0.0;
        vec4 sampledColor = vec4(0.0);

        for (int x = -Width; x <= Width; ++x) {
            for (int y = -Width; y <= Width; ++y) {
                vec2 offset = vec2(x, y) * oneTexel;
                vec4 sample = texture(InSampler, texCoord + offset);

                if (sample.a != 0.0) {
                    sampledColor = sample;
                    float dist = distance(vec2(x, y), vec2(0));
                    alpha += max(0.0, (MaxSample - dist) / Divider);
                }
            }
        }

        alpha = pow(alpha, 1.1);
        color = vec4(sampledColor.rgb, alpha);
    }
}