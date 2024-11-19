package com.monstrous.graphics;

import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

public class Node {
    public Node parent;
    public Node children;
    public Node next;
    public Node previous;

    public Matrix4 localTransform;
    public Matrix4 globalTransform;
    public Vector3 translation;

    NodePart nodePart;

    public Node() {
        parent = null;
        children = null;
        next = null;
        previous = null;

        localTransform = new Matrix4();
        globalTransform = new Matrix4();
        translation = new Vector3(0,0,0);
        nodePart = null;
    }

    public void addChild(Node child){
        child.parent = this;
        if(children == null){
            children = child;
        } else {
            Node sibling = children;
            while (sibling.next != null)
                sibling = sibling.next;
            sibling.next = child;
            child.previous = sibling;
        }
    }


}
