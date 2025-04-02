
struct MyUniforms {
    projectionMatrix: mat4x4f,
};

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*
@group(0) @binding(0) var<uniform> uMyUniforms: MyUniforms;

struct VertexInput {
    @location(0) position: vec2f,
    @location(5) color: vec4f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) color: vec4f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   var pos =  uMyUniforms.projectionMatrix * vec4f(in.position, 0.0, 1.0);
   out.position = pos;
   out.color = in.color;

   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {

    let color = in.color;
    return vec4f(color);
}