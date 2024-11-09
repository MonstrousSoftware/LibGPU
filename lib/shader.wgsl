
struct MyUniforms {
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

   let ratio = 640.0 / 480.0; // The width and height of the target surface

   let angle = uMyUniforms.time; // you can multiply it go rotate faster
   let alpha = cos(angle);
   let beta = sin(angle);
   var position = vec3f(
       in.position.x,
       alpha * in.position.y + beta * in.position.z,
       alpha * in.position.z - beta * in.position.y,
   );
   out.position = vec4f(position.x, position.y * ratio, 0.0, 1.0);
   // We now move the scene depending on the time!
   //var offset = vec2f(-0.6875, -0.463);
   //offset += 0.3 * vec2f(cos(uMyUniforms.time), sin(uMyUniforms.time));
   //out.position = vec4f(in.position.x + offset.x, (in.position.y + offset.y) * ratio, in.position.z, 1.0);

   //out.position = vec4f(in.position.x, in.position.y, in.position.z, 1.0);

   out.color = in.color;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let color = in.color * uMyUniforms.color.rgb;
    //let linear_color = pow(in.color, vec3f(2.2));
    return vec4f(color, 1.0);
}