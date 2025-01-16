
const MAX_DIR_LIGHTS : i32 = 5;
const MAX_POINT_LIGHTS : i32 = 5;

struct DirectionalLight {
    color: vec4f,
    direction: vec4f,
    intensity: vec4f,   // temp
}

struct PointLight {
    color: vec4f,
    position: vec4f,
    intensity: vec4f,
}


struct FrameUniforms {
    projectionMatrix: mat4x4f,
    viewMatrix : mat4x4f,
    combinedMatrix : mat4x4f,
    cameraPosition : vec4f,
    ambientLightLevel : f32,

    directionalLights : array<DirectionalLight, MAX_DIR_LIGHTS>,
    numDirectionalLights: i32,

    pointLights : array<PointLight, MAX_POINT_LIGHTS>,
    numPointLights: i32,

};

struct MaterialUniforms {
    metallicFactor: f32,
    roughnessFactor: f32,
    baseColorFactor: vec4f,
};

struct ModelUniforms {
    modelMatrix: mat4x4f,
};

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*

@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;

@group(1) @binding(0) var<uniform> material: MaterialUniforms;
@group(1) @binding(1) var albedoTexture: texture_2d<f32>;
@group(1) @binding(2) var textureSampler: sampler;
@group(1) @binding(3) var emissiveTexture: texture_2d<f32>;
#ifdef NORMAL_MAP
@group(1) @binding(4) var normalTexture: texture_2d<f32>;
#endif
@group(1) @binding(5) var metallicRoughnessTexture: texture_2d<f32>;


@group(2) @binding(0) var<storage, read> instances: array<ModelUniforms>;


struct VertexInput {
    @location(0) position: vec3f,
    @location(1) uv: vec2f,
    @location(2) normal: vec3f,
#ifdef NORMAL_MAP
    @location(3) tangent: vec3f,
    @location(4) bitangent: vec3f,
#endif
};

struct VertexOutput {
    @builtin(position) position: vec4f,
#ifdef NORMAL_MAP
    @location(0) tangent: vec3f,
    @location(1) bitangent: vec3f,
#endif
    @location(2) normal: vec3f,
    @location(3) uv : vec2f,
    @location(4) viewDirection : vec3f,
    @location(5) cameraPosition: vec3f,
    @location(6) worldPosition: vec3f,
};

@vertex
fn vs_main(in: VertexInput, @builtin(instance_index) instance: u32) -> VertexOutput {
   var out: VertexOutput;

   out.normal = (instances[instance].modelMatrix * vec4f(in.normal, 0.0)).xyz;
#ifdef NORMAL_MAP
   out.tangent = (instances[instance].modelMatrix * vec4f(in.tangent, 0.0)).xyz;
   out.bitangent = (instances[instance].modelMatrix * vec4f(in.bitangent, 0.0)).xyz;
#endif

   let worldPosition =  instances[instance].modelMatrix * vec4f(in.position, 1.0);
   //let worldPosition =  instances[instance].modelMatrix * vec4f(in.position, 1.0);
   let pos =  uFrame.projectionMatrix * uFrame.viewMatrix * worldPosition;
   let cameraPosition = uFrame.cameraPosition.xyz;

   out.position = pos;
   out.uv = in.uv;
   out.viewDirection = cameraPosition.xyz - worldPosition.xyz;
   out.cameraPosition = cameraPosition.xyz;
   out.worldPosition = worldPosition.xyz;
   return out;
}

const PI = 3.14159265359;


// Normal distribution function
fn D_GGX(NdotH: f32, roughness: f32) -> f32{
    let alpha : f32 = roughness * roughness;
    let alpha2 : f32 = alpha * alpha;
    let denom : f32 = (NdotH * NdotH) * (alpha2 - 1.0) + 1.0;
    return alpha2/(PI * denom * denom);
}

fn G_SchlickSmith_GGX(NdotL : f32, NdotV : f32, roughness : f32) -> f32 {
//    let r : f32 = (roughness + 1.0);
//    let k : f32 = (r*r)/8.0;

    let alpha: f32 = roughness * roughness;
    let k: f32 = alpha / 2.0;

    let GL : f32 = NdotL / (NdotL * (1.0 - k) + k);
    let GV : f32 = NdotV / (NdotV * (1.0 - k) + k);
    return GL * GV;
}


fn F_Schlick(cosTheta : f32, metallic : f32, baseColor : vec3f ) -> vec3f {
    let F0 : vec3f = mix(vec3(0.04), baseColor, metallic);
    let F : vec3f = F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
    return F;
}

fn BRDF( L : vec3f, V:vec3f, N: vec3f, roughness:f32, metallic:f32, baseColor: vec3f) -> vec3f {
    let H = normalize(V+L);
    let NdotV : f32 = clamp(dot(N, V), 0.0, 1.0);
    let NdotL : f32 = clamp(dot(N, L), 0.001, 1.0);
    let LdotH : f32 = clamp(dot(L, H), 0.0, 1.0);
    let NdotH : f32 = clamp(dot(N, H), 0.0, 1.0);

    // calculate terms for microfacet shading model
    let D :f32      = D_GGX(NdotH, roughness);
    let G :f32      = G_SchlickSmith_GGX(NdotL, NdotV, roughness);
    let F :vec3f    = F_Schlick(NdotV, metallic, baseColor);

    let kS = F;
    let kD = (vec3f(1.0) - kS) * (1.0 - metallic);

    let specular : vec3f = D * F * G / (4.0 * max(NdotL, 0.0001) * max(NdotV, 0.0001));

    let diffuse : vec3f = kD * baseColor / PI;

    let Lo : vec3f = diffuse + specular;
    return Lo;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {


    let V = normalize(in.viewDirection);

    let baseColor = textureSample(albedoTexture, textureSampler, in.uv).rgb * material.baseColorFactor.rgb;


#ifdef NORMAL_MAP
    let normalMapStrength = 0.8;

    let encodedN = textureSample(normalTexture, textureSampler, in.uv).rgb;
    let localN = encodedN * 2.0 - 1.0;
    // The TBN matrix converts directions from the local space to the world space
    let localToWorld = mat3x3f(
        normalize(in.tangent),
        normalize(in.bitangent),
        normalize(in.normal),
    );
    let worldN = localToWorld * localN;
    let N = mix(in.normal, worldN, normalMapStrength);
#else
    let N = normalize(in.normal);
#endif

    // metallic is coded in the blue channel and roughness in the green channel
    let mrSample = textureSample(metallicRoughnessTexture, textureSampler, in.uv).rgb;

    let roughness : f32 = mrSample.g * material.roughnessFactor;
    let metallic : f32 = mrSample.b * material.metallicFactor;

     //var color = vec3f(0.0);
    // todo some of this could go to vertex shader?

    var radiance = vec3f(0.0);
    // for each directional light
    for (var i: i32 = 0; i < uFrame.numDirectionalLights; i++) {
        let light = uFrame.directionalLights[i];

        let L = -normalize(light.direction.xyz);       // L is vector towards light
        let irradiance = max(dot(L, N), 0.0)* light.intensity.x;
        if(irradiance > 0.0) {
            radiance += BRDF(L, V, N, roughness, metallic, baseColor) * irradiance *  light.color.rgb;
        }
    }

    // for each point light
    for (var i: i32 = 0; i < uFrame.numPointLights; i++) {
        let light:PointLight = uFrame.pointLights[i];
        var lightVector =  light.position.xyz - in.worldPosition.xyz;    // vector towards light source
        let distance:f32 = length(lightVector);
        let L = normalize(lightVector);
        let attenuation = light.intensity.x/(1.0+distance*distance);        // todo: constant, linear and quadratic params

        let irradiance = attenuation * max(dot(L, N), 0.0);
        if(irradiance > 0.0) {
            radiance += BRDF(L, V, N, roughness, metallic, baseColor) * irradiance *  light.color.rgb;
        }
    }

    let ambient : vec3f = baseColor * uFrame.ambientLightLevel;
    let emissiveColor = textureSample(emissiveTexture, textureSampler, in.uv).rgb;

    var color  = radiance + ambient + emissiveColor;

    //color = vec3f(roughness);
    //color = Lo;
    //color = N*0.5 + 0.5;
    //color = encodedN;
    //color = N;
    //color = uFrame.pointLights[0].color.rgb;
    //color = baseColor;
    return vec4f(color, 1.0);
}