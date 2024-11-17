
struct Uniforms {
    projectionMatrix: mat4x4f,
    viewMatrix : mat4x4f,
    modelMatrix: mat4x4f,
    color: vec4f,
    cameraPosition : vec3f,
};

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*

@group(0) @binding(0) var<uniform> uUniforms: Uniforms;
@group(0) @binding(1) var texture: texture_2d<f32>;
@group(0) @binding(2) var normalTexture: texture_2d<f32>;
@group(0) @binding(3) var textureSampler: sampler;


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
    @location(3) viewDirection : vec3f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;
   out.normal = (uUniforms.modelMatrix * vec4f(in.normal, 0.0)).xyz;

   let worldPosition =  uUniforms.modelMatrix * vec4f(in.position, 1.0);
   let pos =  uUniforms.projectionMatrix * uUniforms.viewMatrix * worldPosition;
   let cameraPosition = uUniforms.cameraPosition;

   out.position = pos;
   out.uv = in.uv;
   out.color = in.color;
   out.viewDirection = cameraPosition - worldPosition.xyz;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let kD = 1.0;
    let kS = 0.9;
    let hardness = 16.0;
    let ambient = 0.0;
    let normalMapStrength = 0.5;

    let V = normalize(in.viewDirection);
    //let N = normalize(in.normal);
    let lightColor = vec3f(1.0, 1.0, 1.0);
    let lightDirection = vec3f(0.2, 0.9, 0.0);



    let baseColor = textureSample(texture, textureSampler, in.uv).rgb;
    let encodedN = textureSample(normalTexture, textureSampler, in.uv).rgb;
    let N = normalize(mix(in.normal, encodedN - 0.5, normalMapStrength));
    var color = vec3f(0.0);
    // for each light

        let L = lightDirection;
        let R = reflect(-L, N);

        let diffuse = max(0.0, dot(L, N)) * lightColor;

        let RoV = max(0.0, dot(R, V));

        let specular = pow(RoV, hardness);

        color += baseColor * kD * diffuse + kS * specular;

    color += baseColor * ambient;

    color = N*0.5 + 0.5;
    return vec4f(color, 1.0);
}