/* Particle shader
*/

struct Uniforms {
    projectionMatrix: mat4x4f,
};

struct Particle {
    @location(0) position: vec4f,
    @location(1) velocity : vec4f,
    @location(2) color: vec4f,
};

struct Particles {
  particles : array<Particle>,
}


@group(0) @binding(0) var<uniform> uniforms: Uniforms;
@group(0) @binding(1) var<storage> dataIn : Particles;
@group(0) @binding(2) var<storage, read_write> dataOut : Particles;


struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) velocity : vec4f,
    @location(2) color: vec4f,
};


@vertex
fn vs_main(@builtin(vertex_index) vertexIndex : u32) -> VertexOutput {
   var out: VertexOutput;

   let particle:Particle = dataIn.particles[vertexIndex];

   //out.position = vec4f(0.5, 0.5, 0, 1);// uniforms.projectionMatrix * particle.position;
   out.position = uniforms.projectionMatrix * particle.position;
   out.color = particle.color;

   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    return vec4f(in.color);
}

@compute @workgroup_size(8, 8)
fn updateParticles(@builtin(global_invocation_id) id: vec3<u32>) {

   var particle:Particle = dataIn.particles[id.x];
   particle.position += particle.velocity;
  //let pos  = uniforms.projectionMatrix * particle.position;
   particle.velocity.y = select(particle.velocity.y, -particle.velocity.y, particle.position.y < 1 && particle.position.y > 0);

   particle.color.r = length(particle.velocity);


   dataOut.particles[id.x] = particle;

}

