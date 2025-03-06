// roundedRectangles.wgsl

struct Uniforms {
    projectionMatrix: mat4x4f,
};


@group(0) @binding(0) var<uniform> uniforms: Uniforms;

struct VertexInput {
    @location(0) position: vec2f,
    @location(1) color: vec4f,
    @location(2) center: vec2f,
    @location(3) size: vec2f,
    @location(4) radius: vec2f,

};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) color: vec4f,
    @location(2) center: vec2f,
    @location(3) size: vec2f,
    @location(4) radius: vec2f,
};


@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   out.position =  uniforms.projectionMatrix * vec4f(in.position, 0.0, 1.0);
   out.center = in.center;
   out.size = in.size;
   out.radius = in.radius;
   out.color = in.color;

   return out;
}

fn rectSDF( vectorFromCentre:vec2f, halfSize:vec2f, radius:f32 ) -> f32 {
    let distance = length( max( abs(vectorFromCentre) - halfSize , vec2f(0)) ) - radius ;
    return distance;
}


@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    let r = in.radius.x;
    let edgeSoftness = 0.002;

    var rectHalfSize = in.size/2.0 - vec2f(r+10.0);
    //rectHalfSize.y *= -1;

    let distance = rectSDF(in.position.xy - in.center.xy, rectHalfSize, r);

    let alpha = 1.0 - smoothstep(-edgeSoftness, edgeSoftness, distance/in.size.x);

    var color = in.color;
    color.a = alpha;
    return vec4f(color);
}