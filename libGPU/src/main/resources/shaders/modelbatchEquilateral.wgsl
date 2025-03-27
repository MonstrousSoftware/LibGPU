// Based on Learn OpenGL - IBL

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

    lightCombinedMatrix: mat4x4f,
    lightPosition: vec3f,

};

struct MaterialUniforms {
    metallicFactor: f32,
    roughnessFactor: f32,
    baseColorFactor: vec4f,
};

struct ModelUniforms {
    modelMatrix: mat4x4f,
};

// Group 0 - Frame
// Group 1 - Material
// Group 2 - Instance

// Frame
@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;




// Material
@group(1) @binding(0) var<uniform> material: MaterialUniforms;
@group(1) @binding(1) var albedoTexture: texture_2d<f32>;
@group(1) @binding(2) var textureSampler: sampler;
@group(1) @binding(3) var emissiveTexture: texture_2d<f32>;

@group(1) @binding(5) var metallicRoughnessTexture: texture_2d<f32>;

// Instance
@group(2) @binding(0) var<storage, read> instances: array<ModelUniforms>;



struct VertexInput {
    @location(0) position: vec3f,
#ifdef TEXTURE_COORDINATE
    @location(1) uv: vec2f,
#endif
};

struct VertexOutput {
    @builtin(position)  position: vec4f,
    @location(1)        localPos: vec4f,
};

@vertex
fn vs_main(in: VertexInput, @builtin(instance_index) instance: u32) -> VertexOutput {
   var out: VertexOutput;

   out.localPos = vec4f(in.position, 1.0);
   out.position =  uFrame.combinedMatrix * vec4f(in.position, 1.0);
   return out;
}


const invAtan:vec2f = vec2f(-0.1591, 0.3183);

fn sampleSphericalMap(v:vec3f) -> vec2f {
    var uv:vec2f = vec2f(atan2(v.z, v.x), asin(-v.y));
    uv *= invAtan;
    uv += 0.5;
    return uv;
}


@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {

    let uv:vec2f = sampleSphericalMap(normalize(in.localPos.xyz));
    let color = textureSample(albedoTexture, textureSampler, uv).rgb;

    return vec4f(color, 1);
}