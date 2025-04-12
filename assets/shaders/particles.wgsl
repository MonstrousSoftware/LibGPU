/* Particle shader
*/

struct Uniforms {
    projectionMatrix: mat4x4f,
    screenSize: vec4f,  //xy
    quadScale: vec4f,   // xy
    deltaTime: f32,
};

struct Particle {
    @location(0) position: vec4f,
    @location(1) velocity : vec4f,
    @location(2) color: vec4f,
    @location(3) age: f32,      // time to live in seconds
    @location(4) scale: f32,    // scale factor for particle size
};

struct Particles {
  particles : array<Particle>,
}


@group(0) @binding(0) var<uniform> uniforms: Uniforms;
@group(0) @binding(1) var<storage> dataIn : Particles;
@group(0) @binding(2) var<storage, read_write> dataOut : Particles;
@group(0) @binding(3) var texture: texture_2d<f32>;
@group(0) @binding(4) var textureSampler: sampler;


struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) velocity : vec4f,
    @location(2) color: vec4f,
    @location(3) uv: vec2f,
};

  const localPos = array<vec2f, 6> (
    // 1st triangle
    vec2f( 0.0,  0.0),  // center
    vec2f( 1.0,  0.0),  // right, center
    vec2f( 0.0,  1.0),  // center, top

    // 2nd triangle
    vec2f( 0.0,  1.0),  // center, top
    vec2f( 1.0,  0.0),  // right, center
    vec2f( 1.0,  1.0),  // right, top
  );

// use 6 vertex indices per particle

@vertex
fn vs_main(@builtin(vertex_index) vertexIndex : u32) -> VertexOutput {
   var out: VertexOutput;

   let localIx = vertexIndex % 6;
   let partIx = vertexIndex / 6;

   let particle:Particle = dataIn.particles[partIx];

   let scale:vec2f = particle.scale * uniforms.quadScale.xy;
   let pos =  uniforms.projectionMatrix * vec4f(particle.position.xy, 0.0, 1.0);
   out.position = pos + vec4f(localPos[localIx].x*scale.x, localPos[localIx].y*scale.y, 0, 0);
   out.color = particle.color;
   out.uv.x = localPos[localIx].x;
   out.uv.y = 1.0 - localPos[localIx].y;

   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let color =  in.color * textureSample(texture, textureSampler, in.uv);
    return vec4f(color);
}

@compute @workgroup_size(8, 8)
fn updateParticles(@builtin(global_invocation_id) id: vec3<u32>) {

   var particle:Particle = dataIn.particles[id.x];
   particle.position += particle.velocity * uniforms.deltaTime;
   particle.velocity.y -= 150.0 * uniforms.deltaTime;
   particle.age -= uniforms.deltaTime;


    if (particle.position.x < 0 || particle.position.x > uniforms.screenSize.x) {
        particle.velocity.x *= -1;
    }
    if (particle.position.y < 0 || particle.position.y > uniforms.screenSize.y) {
         particle.velocity.y *= -0.6;
    }
    particle.color.a = clamp(particle.age/2.0, 0.0, 1.0);
    particle.scale = clamp(particle.age/2.0, 0.0, 1.0);
   dataOut.particles[id.x] = particle;

}

