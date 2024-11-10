
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
    @location(1) normal: vec3f,
    @location(2) color: vec3f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec3f,
    @location(1) normal: vec3f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;
   out.normal = in.normal;

   var pos = vec4f(in.position, 1.0);
   pos =  uMyUniforms.projectionMatrix * uMyUniforms.viewMatrix * uMyUniforms.modelMatrix * pos;

    //let ratio = 640.0/480.0;
   // out.position = vec4f(in.position.x, in.position.y * ratio, in.position.z * 0.5 + 0.5, 1.0);
   out.position = pos;
   out.color = in.color;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    var color = in.normal * 0.5 + 0.5;
    //let color = in.color * uMyUniforms.color.rgb;
    //let linear_color = pow(in.color, vec3f(2.2));
    //color = vec3f(1.0, 0.0, 0.4);
    return vec4f(color, 1.0);
}