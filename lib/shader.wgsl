
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

//   let ratio = 640.0 / 480.0; // The width and height of the target surface

   //out.position = uMyUniforms.projectionMatrix * uMyUniforms.viewMatrix * uMyUniforms.modelMatrix * vec4f(in.position, 1.0);

   let angle = uMyUniforms.time; // you can multiply it go rotate faster
   let alpha = cos(angle);
   let beta = sin(angle);
   var pos = vec4f(in.position, 1.0);

   //pos.x = cos(uMyUniforms.time);

   pos =  uMyUniforms.projectionMatrix * uMyUniforms.viewMatrix * uMyUniforms.modelMatrix * pos;
 //  var position = pos;

//   var position = vec4f(
//       pos.x,
//       alpha * pos.y + beta * pos.z,
//       alpha * pos.z - beta * pos.y,
//       1.0
//   );

//    pos.x /= pos.z;
//    pos.y /= pos.z;

   out.position = vec4f(pos.x, pos.y , pos.z * 0.5 + 0.5, 1.0);
   out.color = in.color;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let color = in.color * uMyUniforms.color.rgb;
    //let linear_color = pow(in.color, vec3f(2.2));
    return vec4f(color, 1.0);
}