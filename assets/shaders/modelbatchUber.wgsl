
const MAX_DIR_LIGHTS : i32 = 5;
const MAX_POINT_LIGHTS : i32 = 5;

struct DirectionalLight {
    color: vec4f,
    direction: vec4f,
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
    baseColor: vec4f,
};

struct ModelUniforms {
    modelMatrix: mat4x4f,
};

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*

@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;

@group(1) @binding(0) var<uniform> uMaterial: MaterialUniforms;
@group(1) @binding(1) var albedoTexture: texture_2d<f32>;
@group(1) @binding(2) var textureSampler: sampler;
@group(1) @binding(3) var emissiveTexture: texture_2d<f32>;
#ifdef NORMAL_MAP
@group(1) @binding(4) var normalTexture: texture_2d<f32>;
#endif


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
    @location(5) color: vec3f,
    @location(6) cameraPosition: vec3f,
    @location(7) worldPosition: vec3f,
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
   out.color = uMaterial.baseColor.rgb;
   out.viewDirection = cameraPosition.xyz - worldPosition.xyz;
   out.cameraPosition = cameraPosition.xyz;
   out.worldPosition = worldPosition.xyz;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let kD = 0.6;
    let kS = 0.8;
    let hardness = 8.0;
    let normalMapStrength = 1.0;

    let V = normalize(in.viewDirection);

    let baseColor = textureSample(albedoTexture, textureSampler, in.uv).rgb * in.color;
    let emissiveColor = textureSample(emissiveTexture, textureSampler, in.uv).rgb;

#ifdef NORMAL_MAP
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

    var color = vec3f(0.0);
    // todo some of this could go to vertex shader?

    // for each directional light
    for (var i: i32 = 0; i < uFrame.numDirectionalLights; i++) {
        let light = uFrame.directionalLights[i];

        let lightColor = light.color.rgb;
        let lightDirection = -1*light.direction.xyz;

        let L = lightDirection;

        let diffuse = max(0.0, dot(L, N)) * lightColor;

        // Blinn-Phong
        let H = normalize(L+V); // half-way vector
        let NdotH = max(0.0, dot(N, H));
        let specular = pow(NdotH, hardness);

        color += baseColor * kD * diffuse + kS * specular * light.color.rgb;
    }
    // point lights
    for (var i: i32 = 0; i < uFrame.numPointLights; i++) {
            let light:PointLight = uFrame.pointLights[i];

            let lightColor = light.color.rgb;
            var lightDirection =  light.position.xyz - in.worldPosition.xyz;    // vector towards light source
            let distance:f32 = length(lightDirection);
            lightDirection = normalize(lightDirection);
            let attenuation = light.intensity.x/(1.0+distance*distance);        // todo: constant, linear and quadratic params

            let L = lightDirection;

            let diffuse = max(0.0, dot(L, N)) * lightColor * attenuation;

            // Phong
//            let R = reflect(-L, N);
//            let RoV = max(0.0, dot(R, V));      // angle between reflected light vector R and view vector V
//            let specular = pow(RoV, hardness);

            // Blinn-Phong
            let H = normalize(L+V); // half-way vector
            let NdotH = max(0.0, dot(N, H));
            let specular = pow(NdotH, hardness);

            color += baseColor * kD * diffuse + kS * specular * light.color.rgb;
    }

    color += baseColor * uFrame.ambientLightLevel;

    color += emissiveColor;

    //color = N*0.5 + 0.5;
    //color = encodedN;
    //color = baseColor;
    //color = uFrame.pointLights[0].color.rgb;
    return vec4f(color, 1.0);
}