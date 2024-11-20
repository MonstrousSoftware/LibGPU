
struct FrameUniforms {
    projectionMatrix: mat4x4f,
    viewMatrix : mat4x4f,
    combinedMatrix : mat4x4f,
    cameraPosition : vec3f,
};

struct MaterialUniforms {
    baseColor: vec4f,
};

struct ModelUniforms {
    modelMatrix: mat4x4f,
};

// The memory location of the uniform is given by a pair of a *bind group* and a *binding*

@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;
@group(1) @binding(0) var<uniform> uMaterial: MaterialUniforms;
@group(1) @binding(1) var texture: texture_2d<f32>;
@group(1) @binding(2) var textureSampler: sampler;
@group(2) @binding(0) var<uniform> uModel: ModelUniforms;


struct VertexInput {
    @location(0) position: vec3f,
    @location(1) normal: vec3f,
    @location(2) uv: vec2f,
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
   out.normal = (uModel.modelMatrix * vec4f(in.normal, 0.0)).xyz;

   let worldPosition =  uModel.modelMatrix * vec4f(in.position, 1.0);
   let pos =  uFrame.projectionMatrix * uFrame.viewMatrix * worldPosition;
   let cameraPosition = uFrame.cameraPosition;

   out.position = pos;
   out.uv = in.uv;
   out.color = uMaterial.baseColor.rgb;
   out.viewDirection = cameraPosition - worldPosition.xyz;
   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let kD = 0.9;
    let kS = 0.9;
    let hardness = 16.0;
    let ambient = 0.1;


    let V = normalize(in.viewDirection);
    let normal = normalize(in.normal);
    let lightColor = vec3f(1.0, 1.0, 1.0);
    let lightDirection = vec3f(0.2, 0.9, 0.0);

    let baseColor = textureSample(texture, textureSampler, in.uv).rgb * in.color;

    var color = vec3f(0.0);
    // for each light

        let L = lightDirection;
        let N = normal;
        let R = reflect(-L, N);

        let diffuse = max(0.0, dot(L, N)) * lightColor;

        let RoV = max(0.0, dot(R, V));

        let specular = pow(RoV, hardness);

        color += baseColor * kD * diffuse + kS * specular;

    color += baseColor * ambient;

    //color = uMaterial.baseColor.rgb;

    return vec4f(color, 1.0);
}