package com.monstrous.wgpu;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUColor extends WgpuJavaStruct {
    public final Struct.Double r = new Struct.Double();
    public final Struct.Double g = new Struct.Double();
    public final Struct.Double b = new Struct.Double();
    public final Struct.Double a = new Struct.Double();

    public void set(double r, double g, double b, double a){
        this.r.set(r);
        this.g.set(g);
        this.b.set(b);
        this.a.set(a);
    }
}
