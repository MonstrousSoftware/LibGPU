package com.monstrous.graphics;

import java.util.ArrayList;

// pool to allow reuse of Renderables with having to allocate/free them
//
public class RenderablePool {
    private final ArrayList<Renderable> pool;

    public RenderablePool(){
        this(1000);
    }

    public RenderablePool(int initialCapacity) {
        pool = new ArrayList<>(initialCapacity);
    }

    public Renderable obtain(){
        if(pool.isEmpty())
            return new Renderable();
        return pool.removeLast();
    }

    public void free(Renderable renderable){
        pool.add(renderable);
    }
}
