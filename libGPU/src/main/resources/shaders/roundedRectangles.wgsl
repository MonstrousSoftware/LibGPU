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
   out.center = in.center; //uniforms.projectionMatrix * vec4f(in.center, 0.0, 1.0);
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
    let shadowSoftness = 0.02;

    var rectHalfSize = in.size/2.0 - vec2f(20+r);
    let distance = rectSDF(in.position.xy - in.center.xy, rectHalfSize, r);

    var alpha = 1.0 - smoothstep(0, edgeSoftness, distance/length(in.size));

    // add a drop shadow
    let shadowOffset:vec2f = vec2f(-5,-10);
    let shadowDistance = rectSDF(shadowOffset + in.position.xy - in.center.xy, rectHalfSize, r);
    var shadowAlpha = 1.0 - smoothstep(-shadowSoftness, shadowSoftness, shadowDistance/length(in.size));
    let shadowColor:vec4f = vec4f(vec3f(0.5), 1.0);

    var color = vec4f(in.color.rgb, alpha);
    color = mix(color, shadowColor, shadowAlpha-alpha);
    color.a = alpha+0.5*shadowAlpha;

    return vec4f(color);
}