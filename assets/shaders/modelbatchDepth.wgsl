// depth pass -

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

@group(1) @binding(5) var metallicRoughnessTexture: texture_2d<f32>;


@group(2) @binding(0) var<storage, read> instances: array<ModelUniforms>;


struct VertexInput {
    @location(0) position: vec3f,
    @location(1) uv: vec2f,
    @location(2) normal: vec3f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
};

@vertex
fn vs_main(in: VertexInput, @builtin(instance_index) instance: u32) -> VertexOutput {
   var out: VertexOutput;

   let worldPosition =  instances[instance].modelMatrix * vec4f(in.position, 1.0);
   out.position = uFrame.combinedMatrix * worldPosition;

   return out;
}


@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {

    return vec4f(1.0);
}