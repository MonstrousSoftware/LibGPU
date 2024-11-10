
struct MyUniforms {
    projectionMatrix: mat4x4f,
    viewMatrix : mat4x4f,
    modelMatrix: mat4x4f,
    time: f32,
    color: vec4f,
};

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*
@group(0) @binding(0) var<uniform> uMyUniforms: MyUniforms;

struct VertexInput {
    @location(0) position: vec3f,
    @location(1) color: vec3f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec3f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   var pos = vec4f(in.position, 1.0);
   pos =  uMyUniforms.projectionMatrix * uMyUniforms.viewMatrix * uMyUniforms.modelMatrix * pos;

   out.position = pos;
   out.color = in.color;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let color = in.color * uMyUniforms.color.rgb;
    //let linear_color = pow(in.color, vec3f(2.2));
    return vec4f(color, 1.0);
}