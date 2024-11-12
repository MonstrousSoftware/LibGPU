
struct MyUniforms {
    projectionMatrix: mat4x4f,
};

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*
@group(0) @binding(0) var<uniform> uMyUniforms: MyUniforms;
@group(0) @binding(1) var texture: texture_2d<f32>;
@group(0) @binding(2) var textureSampler: sampler;


struct VertexInput {
    @location(0) position: vec2f,
    //@location(1) color: vec3f,
    @location(1) uv: vec2f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
    //@location(0) color: vec3f,
    @location(0) uv : vec2f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   var pos =  uMyUniforms.projectionMatrix * vec4f(in.position, 0.0, 1.0);
   out.position = pos;
   out.uv = in.uv;
   //out.color = in.color;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    // todo ignore in.color for now

    let color = textureSample(texture, textureSampler, in.uv).rgb;
    return vec4f(color, 1.0);
}