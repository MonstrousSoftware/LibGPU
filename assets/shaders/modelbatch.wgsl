
struct Uniforms {
    projectionMatrix: mat4x4f,
    viewMatrix : mat4x4f,
    modelMatrix: mat4x4f,
    color: vec4f,
};

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*

@group(0) @binding(0) var<uniform> uUniforms: Uniforms;
@group(0) @binding(1) var texture: texture_2d<f32>;
@group(0) @binding(2) var textureSampler: sampler;


struct VertexInput {
    @location(0) position: vec3f,
    @location(1) normal: vec3f,
    @location(2) color: vec3f,
    @location(3) uv: vec2f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec3f,
    @location(1) normal: vec3f,
    @location(2) uv : vec2f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;
   out.normal = (uUniforms.modelMatrix * vec4f(in.normal, 0.0)).xyz;

   var pos = vec4f(in.position, 1.0);
   pos =  uUniforms.projectionMatrix * uUniforms.viewMatrix * uUniforms.modelMatrix * pos;

   out.position = pos;
   out.uv = in.uv;
   out.color = in.color;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let kD = 1.0;
    let kS = 0.5;

    let normal = normalize(in.normal);
    let lightColor = vec3f(1.0, 1.0, 1.0);
 //   let lightColor2 = vec3f(0.6, 0.9, 1.0);
    let lightDirection = vec3f(0.5, 0.5, 0.0);
//    let lightDirection2 = vec3f(0.2, 0.4, 0.3);
//    let shading1 = max(0.0, dot(lightDirection1, normal));
//    let shading2 = max(0.0, dot(lightDirection2, normal));


    let baseColor = textureSample(texture, textureSampler, in.uv).rgb;

    var color = vec3f(0.0);
    // for each light

        let diffuse = max(0.0, dot(lightDirection, normal)) * lightColor;
        let specular = 0.0;
        let shading = diffuse + specular;


        color += baseColor * kD * diffuse + kS * specular;

    return vec4f(color, 1.0);
}