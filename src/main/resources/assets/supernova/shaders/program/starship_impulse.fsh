#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 VanishingPoint;
uniform float Impulse;
uniform float Arrival;

in vec2 texCoord;
out vec4 fragColor;

const float TAU = 6.28318531;

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

float angularNoise(float angle, float cells) {
    float x = (angle / TAU + 0.5) * cells;
    float cell = floor(x);
    float blend = smoothstep(0.0, 1.0, fract(x));
    return mix(hash(mod(cell, cells)), hash(mod(cell + 1.0, cells)), blend);
}

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);
    if (Impulse <= 0.001 && Arrival <= 0.001) {
        fragColor = base;
        return;
    }

    vec2 size = vec2(textureSize(DiffuseSampler, 0));
    vec2 aspect = vec2(size.x / size.y, 1.0);
    vec2 center = clamp(VanishingPoint, vec2(-1.0), vec2(2.0));
    vec2 offset = (texCoord - center) * aspect;
    float dist = length(offset);
    vec2 uvDir = offset / max(dist, 0.0001) / aspect;

    float angle = atan(offset.y, offset.x);
    float streak = angularNoise(angle, 64.0) * 0.6 + angularNoise(angle, 160.0) * 0.4;

    float flash;
    float tunnel;
    float stretch;

    float ringTime;
    float ringFade;

    if (Impulse > 0.001) {
        float t = Impulse;

        flash = smoothstep(0.0, 0.04, t) * (1.0 - smoothstep(0.08, 0.23, t));
        tunnel = smoothstep(0.08, 0.23, t) * (1.0 - smoothstep(0.9, 1.0, t));
        stretch = smoothstep(0.55, 0.85, t) * (1.0 - smoothstep(0.92, 1.0, t));

        ringTime = clamp((t - 0.18) / 0.4, 0.0, 1.0);
        ringFade = smoothstep(0.17, 0.22, t) * (1.0 - smoothstep(0.49, 0.62, t));
    } else {
        float t = 1.0 - Arrival;

        flash = 1.0 - smoothstep(0.0, 0.2, t);
        tunnel = 1.0 - smoothstep(0.3, 1.0, t);
        stretch = 1.0 - smoothstep(0.0, 0.45, t);

        ringTime = clamp((t - 0.1) / 0.5, 0.0, 1.0);
        ringFade = smoothstep(0.08, 0.14, t) * (1.0 - smoothstep(0.45, 0.6, t));
    }

    float ringRadius = mix(0.05, 1.9, 1.0 - (1.0 - ringTime) * (1.0 - ringTime));
    float ringWidth = mix(0.025, 0.09, ringRadius / 1.9);

    float toRing = dist - ringRadius;
    float wave = exp(-toRing * toRing / (ringWidth * ringWidth)) * ringFade;

    float outerRing = dist - ringRadius * 1.22;
    float outerWave = exp(-outerRing * outerRing / (ringWidth * ringWidth * 1.6)) * ringFade * 0.55;
    float inside = 1.0 - smoothstep(-ringWidth, ringWidth, toRing);

    vec2 uv = texCoord - uvDir * (wave + outerWave) * 0.035;
    uv = center + (uv - center) * (1.0 - 0.35 * stretch);

    float aberration = (tunnel * 0.006 + stretch * 0.008 + (wave + outerWave) * 0.018) * dist;
    float smear = tunnel * (0.25 + 0.75 * streak) * smoothstep(0.0, 0.5, dist) * 0.35 + stretch * (0.5 + 0.5 * streak) * 0.9;

    float falloff = mix(2.0, 0.5, stretch);
    float jitter = hash(dot(texCoord * size, vec2(12.9898, 78.233))) / 23.0;

    vec3 color = vec3(0.0);
    float totalWeight = 0.0;
    for (int sampleIndex = 0; sampleIndex < 24; sampleIndex++) {
        float alongTrail = min(float(sampleIndex) / 23.0 + jitter, 1.0);
        float weight = exp(-alongTrail * falloff);
        vec2 sampleUv = mix(uv, center, alongTrail * smear);

        color.r += texture(DiffuseSampler, clamp(sampleUv + uvDir * aberration, vec2(0.001), vec2(0.999))).r * weight;
        color.g += texture(DiffuseSampler, clamp(sampleUv, vec2(0.001), vec2(0.999))).g * weight;
        color.b += texture(DiffuseSampler, clamp(sampleUv - uvDir * aberration, vec2(0.001), vec2(0.999))).b * weight;
        totalWeight += weight;
    }
    color /= totalWeight;

    float darkness = tunnel * (0.3 + 0.4 * inside) * smoothstep(0.05, 0.8, dist);
    color = mix(color, color * vec3(0.35, 0.45, 0.75) + vec3(0.01, 0.03, 0.08), darkness);

    vec3 ringColor = mix(vec3(0.25, 0.8, 1.0), vec3(0.6, 0.35, 1.0), smoothstep(-ringWidth, ringWidth, toRing));
    color += ringColor * wave * (0.6 + 0.4 * streak) * 1.4;
    color += vec3(0.45, 0.5, 1.0) * outerWave * (0.5 + 0.5 * streak);

    color = mix(color, vec3(0.80, 0.87, 0.95) + color * 0.25, flash * 0.85);
    fragColor = vec4(color, base.a);
}
