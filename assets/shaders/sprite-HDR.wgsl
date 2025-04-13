
struct Uniforms {
    projectionMatrix: mat4x4f,
};


@group(0) @binding(0) var<uniform> uniforms: Uniforms;
@group(0) @binding(1) var texture: texture_2d<f32>;
@group(0) @binding(2) var textureSampler: sampler;


struct VertexInput {
    @location(0) position: vec2f,
#ifdef TEXTURE_COORDINATE
    @location(1) uv: vec2f,
#endif
#ifdef COLOR
    @location(2) color: vec4f,
#endif
};

struct VertexOutput {
    @builtin(position) position: vec4f,
#ifdef TEXTURE_COORDINATE
    @location(0) uv : vec2f,
#endif
    @location(1) color: vec4f,
};


@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   var pos =  uniforms.projectionMatrix * vec4f(in.position, 0.0, 1.0);
   out.position = pos;
#ifdef TEXTURE_COORDINATE
   out.uv = in.uv;
#endif

#ifdef COLOR
   let color:vec4f = in.color;
#else
   let color:vec4f = vec4f(1,1,1,1);   // white
#endif
   out.color = color;

   return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {

#ifdef TEXTURE_COORDINATE
    let color =  textureSample(texture, textureSampler, in.uv);
#else
    let color = in.color;
#endif

    // map HDR to LDR
    var toneColor = aces_tone_map(color.rgb);

    return vec4f(toneColor.rgb, color.a);
}


// Maps HDR values to linear values
// Based on http://www.oscars.org/science-technology/sci-tech-projects/aces
fn aces_tone_map(hdr: vec3<f32>) -> vec3<f32> {
    let m1 = mat3x3(
        0.59719, 0.07600, 0.02840,
        0.35458, 0.90834, 0.13383,
        0.04823, 0.01566, 0.83777,
    );
    let m2 = mat3x3(
        1.60475, -0.10208, -0.00327,
        -0.53108,  1.10813, -0.07276,
        -0.07367, -0.00605,  1.07602,
    );
    let v = m1 * hdr;
    let a = v * (v + 0.0245786) - 0.000090537;
    let b = v * (0.983729 * v + 0.4329510) + 0.238081;
    return clamp(m2 * (a / b), vec3(0.0), vec3(1.0));
}