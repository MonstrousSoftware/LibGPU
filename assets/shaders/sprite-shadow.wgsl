// demonstration of post-processing
// follows structure of sprite.wgsl
//
// TEST
//
struct Uniforms {
    projectionMatrix: mat4x4f,
};

@group(0) @binding(0) var<uniform> uniforms: Uniforms;
@group(0) @binding(1) var texture: texture_depth_2d;
@group(0) @binding(2) var textureSampler: sampler;


struct VertexInput {
    @location(0) position: vec2f,
    @location(1) uv: vec2f,
    @location(2) color: vec4f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) uv : vec2f,
    @location(1) color: vec4f,
};


@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   var pos =  uniforms.projectionMatrix * vec4f(in.position, 0.0, 1.0);
   out.position = pos;
   out.uv = in.uv;
   out.color = in.color;

   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {

    var color = in.color * textureSample(texture, textureSampler, in.uv);
    let grey:f32 = (color.r + color.g + color.b )/3.0;
    var col = vec3f(grey, grey, grey,);

        // vignette effect
    let dist = in.uv * (1.0 - in.uv.yx);
    let vigExtent = 45.0;
    var vig = dist.x*dist.y * vigExtent; // multiply with sth for intensity
    let vigPower = 0.45;
    vig = pow(vig, vigPower); // change pow for modifying the extent of the  vignette
    col = mix(col, col*vig, 0.9);

    return vec4f(col, color.a);
}