
struct DirectionalLight {
    color: vec4f,
    direction: vec3f,
}


struct FrameUniforms {
    projectionMatrix: mat4x4f,
    viewMatrix : mat4x4f,
    combinedMatrix : mat4x4f,
    cameraPosition : vec3f,
    directionalLight : DirectionalLight,
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

@group(2) @binding(0) var<uniform> uModel: ModelUniforms;


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
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   out.normal = (uModel.modelMatrix * vec4f(in.normal, 0.0)).xyz;
#ifdef NORMAL_MAP
   out.tangent = (uModel.modelMatrix * vec4f(in.tangent, 0.0)).xyz;
   out.bitangent = (uModel.modelMatrix * vec4f(in.bitangent, 0.0)).xyz;
#endif

   let worldPosition =  uModel.modelMatrix * vec4f(in.position, 1.0);
   let pos =  uFrame.projectionMatrix * uFrame.viewMatrix * worldPosition;
   let cameraPosition = uFrame.cameraPosition;

   out.position = pos;
   out.uv = in.uv;
   out.color = uMaterial.baseColor.rgb;
   out.viewDirection = cameraPosition - worldPosition.xyz;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let kD = 1.0;
    let kS = 0.1;
    let hardness = 1.0;
    let ambient = 0.1;
    let normalMapStrength = 1.0;

    let V = normalize(in.viewDirection);

    let lightColor = uFrame.directionalLight.color.rgb; //vec3f(1.0, 1.0, 1.0);
    let lightDirection = -1*uFrame.directionalLight.direction.xyz; //rvec3f(0.0, 1.0, 0.0);

    let baseColor = textureSample(albedoTexture, textureSampler, in.uv).rgb;// * in.color;
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
    // for each light

        let L = lightDirection;
        let R = reflect(-L, N);

        let diffuse = max(0.0, dot(L, N)) * lightColor;

        let RoV = max(0.0, dot(R, V));

        let specular = pow(RoV, hardness);

        color += baseColor * kD * diffuse + kS * specular;

    color += baseColor * ambient;

    color += emissiveColor;

    //color = N*0.5 + 0.5;
    //color = encodedN;
    //color = baseColor;
    return vec4f(color, 1.0);
}