
const MAX_DIR_LIGHTS : i32 = 5;

struct DirectionalLight {
    color: vec4f,
    direction: vec4f,
}


struct FrameUniforms {
    projectionMatrix: mat4x4f,
    viewMatrix : mat4x4f,
    combinedMatrix : mat4x4f,
    cameraPosition : vec4f,
    directionalLights : array<DirectionalLight, MAX_DIR_LIGHTS>,
    numDirectionalLights: i32,
    ambientLightLevel : f32,
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
    @location(0) position: vec4f,
    @location(1) color: vec4f,
    @location(2) uv: vec2f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) uv : vec2f,
    @location(1) color: vec3f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   let worldPosition =  uModel.modelMatrix * vec4f(in.position.xyz, 1.0);
   let pos =  uFrame.projectionMatrix * uFrame.viewMatrix * worldPosition;

   out.position = pos;
   out.uv = in.uv;
   out.color = in.color.rgb;

   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {

    //color = N*0.5 + 0.5;
    //color = encodedN;
    let color = vec3f(1.0,0.0,0.0);
    return vec4f(color, 1.0);
}