"struct VertexInput {\n" +
            "    @location(0) position: vec2f,\n" +
            "    @location(1) color: vec3f,\n" +
            "};\n" +
            "\nstruct VertexOutput {\n" +
            "    @builtin(position) position: vec4f,\n" +
            "    @location(0) color: vec3f,\n" +
            "};\n\n" +

            "@vertex\n" +
            "fn vs_main(in: VertexInput) -> VertexOutput {\n" +
            "   var out: VertexOutput;\n" +
            "   let ratio = 640.0 / 480.0; // The width and height of the target surface\n"+
            "   out.position = vec4f(in.position.x, in.position.y * ratio, 0.0, 1.0);\n"+
            "   out.color = in.color;\n" +
            "   return out;\n" +
            "}\n" +
            "\n" +
            "@fragment\n" +
            "fn fs_main(in : VertexOutput) -> @location(0) vec4f {\n" +
            "    return vec4f(in.color, 1.0);\n" +
            "}"