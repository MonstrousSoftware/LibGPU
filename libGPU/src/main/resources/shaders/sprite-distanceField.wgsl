// sprite-distanceField.wgsl
//
// Used for SDF font rendering
//
struct MyUniforms {
    projectionMatrix: mat4x4f,
};

// this should maybe be a uniform, but we want to stay compatible with sprite.wgsl
const smoothing : f32 = 1/16.0;

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*
@group(0) @binding(0) var<uniform> uMyUniforms: MyUniforms;
@group(0) @binding(1) var texture: texture_2d<f32>;
@group(0) @binding(2) var textureSampler: sampler;


struct VertexInput {
    @location(0) position: vec2f,
    @location(1) uv: vec2f,
    @location(2) color: vec4f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) uv : vec2f,
    @location(1) color: vec4f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   var pos =  uMyUniforms.projectionMatrix * vec4f(in.position, 0.0, 1.0);
   out.position = pos;
   out.uv = in.uv;
   out.color = in.color;

   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {

    let distance : f32 = textureSample(texture, textureSampler, in.uv).a;
    let alpha : f32 = smoothstep( 0.5 - smoothing, 0.5 + smoothing, distance);

    return vec4f(in.color.rgb, alpha * in.color.a);
}