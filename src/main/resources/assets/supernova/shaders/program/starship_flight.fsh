#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform mat4 InvProjection;
uniform vec4 CameraRotation;

uniform vec3 PlanetCenter;
uniform vec3 LandColor;
uniform vec3 WaterColor;
uniform vec3 AtmosphereColor;
uniform float PlanetSeed;
uniform float SeaLevel;
uniform float Clouds;

uniform vec3 DestinationCenter;
uniform vec3 DestinationLandColor;
uniform vec3 DestinationWaterColor;
uniform vec3 DestinationAtmosphereColor;
uniform float DestinationPlanetSeed;
uniform float DestinationSeaLevel;
uniform float DestinationClouds;

uniform vec3 EnginePosition;
uniform vec3 EngineDirection;
uniform vec3 CloudCameraPosition;
uniform vec2 CloudBounds;
uniform float CloudVisibility;
uniform float CloudTransitionHeight;
uniform float CloudExitProgress;
uniform float FlightTime;
uniform float Visibility;
uniform float WarpAmount;
uniform float ExhaustAmount;
uniform int FireCount;
uniform vec4 FireEngine0;
uniform vec4 FireDirection0;
uniform float OriginVisibility;
uniform float DestinationVisibility;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec3 point) {
    point = fract(point * 0.1031);
    point += dot(point, point.yzx + 33.33);
    return fract((point.x + point.y) * point.z);
}

float noise(vec3 point) {
    vec3 cell = floor(point);
    vec3 fraction = fract(point);
    fraction = fraction * fraction * (3.0 - 2.0 * fraction);

    float lower = mix(
        mix(hash(cell), hash(cell + vec3(1, 0, 0)), fraction.x),
        mix(hash(cell + vec3(0, 1, 0)), hash(cell + vec3(1, 1, 0)), fraction.x),
        fraction.y
    );
    float upper = mix(
        mix(hash(cell + vec3(0, 0, 1)), hash(cell + vec3(1, 0, 1)), fraction.x),
        mix(hash(cell + vec3(0, 1, 1)), hash(cell + vec3(1, 1, 1)), fraction.x),
        fraction.y
    );
    return mix(lower, upper, fraction.z);
}

float terrain(vec3 point) {
    float value = 0.0;
    float weight = 0.5;
    for (int octave = 0; octave < 5; octave++) {
        value += weight * noise(point);
        point = point * 2.03 + vec3(13.1, 7.7, 5.3);
        weight *= 0.5;
    }
    return value;
}

vec3 rotateCamera(vec3 direction) {
    return direction + 2.0 * cross(
        CameraRotation.xyz,
        cross(CameraRotation.xyz, direction) + CameraRotation.w * direction
    );
}

vec3 stars(vec3 ray) {
    float star = hash(floor(ray * 450.0));
    float point = smoothstep(0.9975, 1.0, star);
    float twinkle = 0.7 + 0.3 * sin(FlightTime * 0.5 + star * 100.0);
    return vec3(0.002, 0.004, 0.012) + vec3(0.7, 0.85, 1.0) * point * twinkle;
}

vec4 planetSurface(
    vec3 ray,
    vec3 center,
    vec3 landColor,
    vec3 waterColor,
    vec3 atmosphereColor,
    float seed,
    float seaLevel,
    float clouds
) {
    float radius = 7000.0;
    vec3 origin = -center;
    float alongRay = dot(origin, ray);
    float discriminant = alongRay * alongRay - dot(origin, origin) + radius * radius;
    float nearest = -alongRay - sqrt(max(0.0, discriminant));

    if (discriminant < 0.0 || nearest < 0.0) {
        float closest = length(origin + ray * max(0.0, -alongRay));
        float halo = exp(-max(0.0, closest - radius) / 45.0) * step(0.0, -alongRay);
        return vec4(atmosphereColor * 0.7, halo);
    }

    vec3 normal = normalize(ray * nearest - center);
    vec3 point = normal * 7.0 + vec3(seed);
    float height = terrain(point);
    float ridges = terrain(point * 12.0);
    float water = seaLevel > 0.0
        ? 1.0 - smoothstep(seaLevel - 0.015, seaLevel + 0.015, height)
        : 0.0;

    vec3 land = landColor * mix(0.48, 1.38, height) * mix(0.72, 1.1, ridges);
    float mountains = smoothstep(0.66, 0.82, height);
    land = mix(land, vec3(0.83, 0.86, 0.84), mountains * step(0.1, seaLevel));
    vec3 surface = mix(land, waterColor * mix(0.5, 1.1, ridges), water);

    float cloudNoise = terrain(point * 3.0 + vec3(FlightTime * 0.0008, 0.0, 0.0));
    float cloud = smoothstep(0.72 - clouds * 0.35, 0.88 - clouds * 0.25, cloudNoise) * clouds;
    surface = mix(surface, vec3(0.92, 0.95, 0.96), cloud);

    vec3 light = normalize(vec3(-0.5, 0.9, 0.25));
    float diffuse = 0.28 + 0.72 * max(0.0, dot(normal, light));
    float rim = pow(1.0 - max(0.0, dot(normal, -ray)), 3.0);
    return vec4(surface * diffuse + atmosphereColor * rim * 0.52, 1.0);
}

vec3 warpStreaks(vec3 ray) {
    vec3 forward = -EngineDirection;
    vec3 up = abs(forward.y) > 0.95 ? vec3(1, 0, 0) : vec3(0, 1, 0);
    vec3 right = normalize(cross(forward, up));
    up = cross(right, forward);

    float facing = dot(ray, forward);
    if (facing <= 0.01) {
        return vec3(0);
    }

    vec2 offset = vec2(dot(ray, right), dot(ray, up)) / facing;
    float radius = max(length(offset), 0.005);
    float angle = atan(offset.y, offset.x);
    float laneCoordinate = (angle + 3.141593) / 6.283185 * 420.0;
    float lane = floor(laneCoordinate);
    float seed = hash(vec3(lane, 17.0, 8.0));
    float distanceToLane = abs(fract(laneCoordinate) - 0.5);
    float laneWidth = max(fwidth(laneCoordinate) * 0.55, 0.015);
    float line = 1.0 - smoothstep(laneWidth, laneWidth * 2.0, distanceToLane);
    line *= min(1.0, 0.12 / max(fwidth(laneCoordinate), 0.001));

    float travel = fract(log(radius + 0.008) * 0.35 - FlightTime * (4.0 + seed * 5.0) + seed * 10.0);
    float tail = smoothstep(0.35, 0.55, travel);
    float head = 1.0 - smoothstep(0.94, 1.0, travel);
    float streak = tail * head * line * smoothstep(0.015, 0.08, radius);
    return vec3(0.65, 0.82, 1.0) * streak * mix(0.4, 1.8, seed);
}

vec3 engineHalos(vec3 ray, float geometryDistance) {
    float facing = dot(ray, EngineDirection);
    if (abs(facing) < 0.001) {
        return vec3(0);
    }

    vec3 glow = vec3(0);
    for (int ring = 0; ring < 8; ring++) {
        float phase = fract(float(ring) / 8.0 + FlightTime * 2.4);
        float distance = 0.3 + phase * 16.0;
        vec3 center = EnginePosition + EngineDirection * distance;
        float hit = dot(center, EngineDirection) / facing;
        if (hit <= 0.0 || hit >= geometryDistance) {
            continue;
        }

        float radius = 0.65 + distance * 0.16;
        float edge = abs(length(ray * hit - center) - radius);
        float brightness = exp(-edge * edge / 0.012) * (1.0 - phase);
        vec3 color = mix(vec3(0.05, 0.75, 1.0), vec3(0.65, 0.3, 1.0), phase);
        glow += color * brightness * 1.7;
    }
    return glow;
}

vec3 flightFire(vec3 ray, float geometryDistance, vec3 enginePosition, vec3 engineDirection, bool reentry) {
    float centerOffset = reentry ? 0.25 : 4.0;
    vec3 center = enginePosition + engineDirection * centerOffset;

    float radius = reentry ? 3.8 : 4.8;
    float alongRay = dot(center, ray);
    float discriminant = alongRay * alongRay - dot(center, center) + radius * radius;
    if (discriminant <= 0.0) {
        return vec3(0);
    }

    float halfLength = sqrt(discriminant);
    float start = max(0.0, alongRay - halfLength);
    float end = min(geometryDistance, alongRay + halfLength);
    if (end <= start) {
        return vec3(0);
    }

    vec3 up = abs(engineDirection.y) > 0.95 ? vec3(1, 0, 0) : vec3(0, 1, 0);
    vec3 right = normalize(cross(engineDirection, up));
    up = cross(right, engineDirection);

    float stepLength = (end - start) / 32.0;
    float transmittance = 1.0;
    vec3 glow = vec3(0);

    for (int sampleIndex = 0; sampleIndex < 32; sampleIndex++) {
        float distance = start + (float(sampleIndex) + 0.5) * stepLength;

        vec3 point = ray * distance - enginePosition;
        vec3 local = vec3(dot(point, right), dot(point, up), dot(point, engineDirection));

        float radial = length(local.xy);
        if (!reentry && (local.z <= 0.0 || local.z >= 8.0 || radial > 1.2)) {
            continue;
        }

        float flow = reentry ? -0.3 : 1.0;
        vec3 turbulencePoint = local * vec3(2.5, 2.5, 0.8) - vec3(0, 0, FlightTime * 12.0 * flow);
        float turbulence = noise(turbulencePoint) * 0.7 + noise(turbulencePoint * 2.1) * 0.3;

        float density;
        float heat;
        if (reentry) {
            float height = -local.z;
            float radialFraction = radial / 3.4;

            float shieldHeight = -0.8 + radialFraction * radialFraction * 1.2;
            float shieldEdge = (height - shieldHeight) * 3.5;
            float shield = exp(-shieldEdge * shieldEdge);

            float rimFade = 1.0 - smoothstep(0.8, 1.0, radialFraction);
            float hullFade = 1.0 - smoothstep(0.4, 1.0, height);

            density = shield * rimFade * hullFade * (0.1 + turbulence * 0.25);
            heat = clamp((1.0 - radialFraction * radialFraction) * 0.65 + turbulence * 0.25, 0.0, 1.0);
        } else {
            float lengthFraction = local.z / 8.0;
            float width = mix(0.55, 0.08, lengthFraction) + sin(lengthFraction * 3.141593) * 0.45;
            float core = 1.0 - radial / width;

            density = smoothstep(0.0, 0.6, core + (turbulence - 0.5) * 0.6);
            density *= smoothstep(0.0, 0.025, lengthFraction) * (1.0 - smoothstep(0.55, 1.0, lengthFraction));
            heat = clamp(core * (1.0 - lengthFraction) + turbulence * 0.25, 0.0, 1.0);
        }

        vec3 color;
        if (reentry) {
            color = mix(vec3(0.9, 0.12, 0.015), vec3(1.25, 0.34, 0.06), heat);
        } else {
            color = mix(vec3(2.8, 0.15, 0.015), vec3(4.0, 2.8, 1.1), heat);
            float nozzle = (1.0 - smoothstep(0.0, 1.2, local.z)) * heat;
            color = mix(color, vec3(1.0, 2.0, 4.0), nozzle);
        }

        float opacity = 1.0 - exp(-density * stepLength * 1.6);
        glow += color * opacity * transmittance;
        transmittance *= 1.0 - opacity;
        if (transmittance < 0.01) {
            break;
        }
    }

    return glow;
}

float cloudDensity(vec3 position) {
    float height = (position.y - CloudBounds.x) / (CloudBounds.y - CloudBounds.x);
    float profile = smoothstep(0.0, 0.2, height) * (1.0 - smoothstep(0.8, 1.0, height));
    if (profile <= 0.001) {
        return 0.0;
    }

    vec3 wind = vec3(FlightTime * 0.025, 0.0, FlightTime * 0.012);
    vec3 point = vec3(position.x, position.y - CloudBounds.x, position.z);
    float coverage = noise(vec3(point.x * 0.009, 17.0, point.z * 0.009) + wind * 0.3);
    float transitionCover = 1.0 - smoothstep(16.0, 48.0, abs(position.y - CloudTransitionHeight));
    float threshold = mix(0.62, 0.32, smoothstep(0.3, 0.75, coverage)) - transitionCover * 0.1;

    float billows = noise(point * 0.015 + wind) * 0.75;
    billows += noise(point * 0.04 + wind) * 0.25;

    float density = max(0.0, billows - threshold - (1.0 - profile) * 0.35) * profile * 2.0;

    if (CloudExitProgress > 0.0) {
        float depthFromTop = max(0.0, CloudBounds.y - position.y);
        float openingRadius = mix(-32.0, 800.0, CloudExitProgress) - depthFromTop * 0.65;
        openingRadius += (billows - 0.5) * 16.0;

        float openingMask = smoothstep(openingRadius - 24.0, openingRadius + 24.0, length(position.xz));
        density *= openingMask;
    }

    return density * (1.0 + transitionCover);
}

vec4 volumetricClouds(vec3 ray, float geometryDistance) {
    float start = 6.0;
    float end = min(512.0, geometryDistance);
    if (abs(ray.y) > 0.0001) {
        vec2 intersections = (CloudBounds - CloudCameraPosition.y) / ray.y;
        start = max(start, min(intersections.x, intersections.y));
        end = min(end, max(intersections.x, intersections.y));
    } else if (CloudCameraPosition.y <= CloudBounds.x || CloudCameraPosition.y >= CloudBounds.y) {
        return vec4(0);
    }
    if (end <= start) {
        return vec4(0);
    }

    float stepLength = (end - start) / 32.0;
    vec3 sunlight = normalize(vec3(-0.5, 0.9, 0.25));
    vec3 color = vec3(0);
    float transmittance = 1.0;

    for (int sampleIndex = 0; sampleIndex < 32; sampleIndex++) {
        float distance = start + (float(sampleIndex) + 0.5) * stepLength;
        vec3 position = CloudCameraPosition + ray * distance;
        float density = cloudDensity(position) * CloudVisibility;
        if (density <= 0.001) {
            continue;
        }

        float shadow = cloudDensity(position + sunlight * 18.0) * CloudVisibility;
        float light = exp(-shadow * 2.0);
        vec3 cloudColor = mix(vec3(0.42, 0.48, 0.58), vec3(0.98, 0.97, 0.94), light);
        float opacity = 1.0 - exp(-density * stepLength * 0.045);
        color += cloudColor * opacity * transmittance;
        transmittance *= 1.0 - opacity;
        if (transmittance < 0.01) {
            break;
        }
    }

    return vec4(color, 1.0 - transmittance);
}

void main() {
    vec2 uv = texCoord;
    vec4 original = texture(DiffuseSampler, uv);

    if (Visibility <= 0.001 && CloudVisibility <= 0.001 && FireCount == 0) {
        fragColor = original;
        return;
    }

    vec4 view = InvProjection * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    vec3 ray = rotateCamera(normalize(view.xyz / view.w));
    float depth = texture(DiffuseDepthSampler, uv).r;
    vec4 geometry = InvProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    float geometryDistance = depth >= 0.999999 ? 1e9 : length(geometry.xyz / geometry.w);
    vec3 scene = original.rgb;

    if (Visibility > 0.001) {
        float atmosphere = CloudVisibility * (1.0 - OriginVisibility);
        scene = mix(stars(ray), AtmosphereColor * 0.55, atmosphere);
        if (WarpAmount > 0.001) {
            scene += warpStreaks(ray) * WarpAmount;
        }

        if (OriginVisibility > 0.001) {
            vec4 planet = planetSurface(
                ray, PlanetCenter, LandColor, WaterColor, AtmosphereColor, PlanetSeed, SeaLevel, Clouds
            );
            scene = mix(scene, planet.rgb, planet.a * OriginVisibility);
        }

        if (DestinationVisibility > 0.001) {
            vec4 destination = planetSurface(
                ray, DestinationCenter, DestinationLandColor, DestinationWaterColor,
                DestinationAtmosphereColor, DestinationPlanetSeed, DestinationSeaLevel, DestinationClouds
            );
            scene = mix(scene, destination.rgb, destination.a * DestinationVisibility);
        }

        float preserve = 1.0 - smoothstep(48.0, 96.0, geometryDistance);
        scene = mix(scene, original.rgb, preserve);
        if (ExhaustAmount > 0.001) {
            scene += engineHalos(ray, geometryDistance) * ExhaustAmount;
        }
        scene = mix(original.rgb, scene, Visibility);
    }

    if (CloudVisibility > 0.001) {
        vec4 cloud = volumetricClouds(ray, geometryDistance);
        scene = scene * (1.0 - cloud.a) + cloud.rgb;
    }

    if (FireCount > 0) {
        vec4 engine = FireEngine0;
        vec4 direction = FireDirection0;
        if (engine.w > 0.001) {
            scene += flightFire(ray, geometryDistance, engine.xyz, direction.xyz, false) * engine.w;
        }
        if (direction.w > 0.001) {
            scene += flightFire(ray, geometryDistance, engine.xyz, direction.xyz, true) * direction.w;
        }
    }

    fragColor = vec4(scene, original.a);
}
