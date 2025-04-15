package com.monstrous.graphics.g3d;

import com.monstrous.math.Matrix4;
import com.monstrous.math.Quaternion;
import com.monstrous.math.Vector3;

public class AnimationController {
    public ModelInstance instance;
    public AnimationDesc animationDesc;

    private Vector3 tmpTra;
    private Quaternion tmpQ;
    private Vector3 tmpScl;


    /** keeps animation state */
    public static class AnimationDesc {
        public Animation animation;
        public float time;
        public int loopCount;
        public float speed;
        public float duration;

        public AnimationDesc(Animation animation, int loopCount, float duration, float speed) {
            this.animation = animation;
            time = 0f;
            this.loopCount = loopCount;
            this.duration = duration;
            this.speed = speed;
        }

        public void update(float deltaTime) {
            time += deltaTime * speed;

            while (time > duration) {   // todo use some float modulo...
                time -= duration;
                loopCount--;
            }
            while (time < 0) {   // in case speed < 0
                time += duration;
                loopCount--;
            }
        }

    }

    public AnimationController(ModelInstance instance) {
        this.instance = instance;

        tmpQ = new Quaternion().idt();
        tmpTra = new Vector3(0,0,0);
        tmpScl = new Vector3(1, 1, 1);
    }

    public void setAnimation(String animationId){
        setAnimation(animationId, 1);
    }

    public void setAnimation(String animationId, int loopCount){
        setAnimation(animationId, loopCount, 1.0f);
    }

    public void setAnimation(String animationId, int loopCount, float speed){
        if(animationId != null) {
            for (Animation anim : instance.model.getAnimations()) {
                if (anim.name == null || anim.name.contentEquals(animationId)) {
                    animationDesc = new AnimationDesc(anim, loopCount, anim.duration, speed);
                    return;
                }
            }
            System.out.println("Animation not found:"+animationId);
        }
        animationDesc = null;
    }

    /** use first, possibly anonymous animation */
    public void setAnimation( int loopCount, float speed){
        Animation anim = instance.model.getAnimations().get(0);
        animationDesc = new AnimationDesc(anim, loopCount, anim.duration, speed);
    }

    public AnimationDesc update(float deltaTime){
        if(animationDesc == null)
            return null;
        animationDesc.update(deltaTime);
        if(animationDesc.time > animationDesc.duration)
            throw new RuntimeException("Animation time out of bounds");

        // clear isAnimated flag because the time may be outside the animation duration of a node
        for(NodeAnimation nodeAnim: animationDesc.animation.nodeAnimations){
            nodeAnim.node.isAnimated = false;   // set to true if time is within an animation range
        }

        Node prevNode = null;

        // todo what values do we use before the first keyframe and after the last? (todo perhaps interpolate between last and first?)
        for(NodeAnimation nodeAnim: animationDesc.animation.nodeAnimations){

            // assume the animations are in node order (1), aggregate the different operators to apply at the same time
            // and in the correct operator order: translate, rotate then scale
            // (1): perhaps the data structure should group animations per node
            if(nodeAnim.node != prevNode) {
                if(prevNode != null)
                    updateNodeTransform(prevNode, tmpTra, tmpQ, tmpScl);
                tmpQ.idt();
                tmpScl.set(1, 1, 1);
                tmpTra.set(0, 0, 0);
                prevNode = nodeAnim.node;
            }

            if(nodeAnim.rotation != null) {
                NodeKeyframe<Quaternion> prevKey = nodeAnim.rotation.get(0);
                for (NodeKeyframe<Quaternion> keyFrame : nodeAnim.rotation) {
                    if (prevKey.keyTime <= animationDesc.time && keyFrame.keyTime > animationDesc.time) {
                        float fraction = (animationDesc.time - prevKey.keyTime) / (keyFrame.keyTime - prevKey.keyTime);
                        tmpQ.set(prevKey.value).slerp(keyFrame.value, fraction);
                        break;
                    } else
                        prevKey = keyFrame;
                }
            }
            if(nodeAnim.translation != null){
                NodeKeyframe<Vector3> prevKey = nodeAnim.translation.get(0);
                for( NodeKeyframe<Vector3> keyFrame: nodeAnim.translation){
                    if(prevKey.keyTime <= animationDesc.time && keyFrame.keyTime > animationDesc.time){
                        float fraction = (animationDesc.time - prevKey.keyTime)/(keyFrame.keyTime- prevKey.keyTime);
                        tmpTra.set(prevKey.value).lerp(keyFrame.value, fraction);//
                        break;
                    }
                    else
                        prevKey = keyFrame;
                }
            }
            if(nodeAnim.scaling != null){
                NodeKeyframe<Vector3> prevKey = nodeAnim.scaling.get(0);
                for( NodeKeyframe<Vector3> keyFrame: nodeAnim.scaling){
                    if(prevKey.keyTime <= animationDesc.time && keyFrame.keyTime > animationDesc.time){
                        float fraction = animationDesc.time - prevKey.keyTime/(keyFrame.keyTime- prevKey.keyTime);
                        tmpScl.set(prevKey.value).lerp(keyFrame.value, fraction);
                        break;
                    }
                    else
                        prevKey = keyFrame;
                }
            }


        }
        if(prevNode != null)
            updateNodeTransform(prevNode, tmpTra, tmpQ, tmpScl);
// ThinMatrix OpenGL skel anim #3: https://www.youtube.com/watch?v=cieheqt7eqc

//        int jointId = 0;
//        for(Node joint : instance.model.joints){
//            joint.globalTransform.mul(instance.model.inverseBoneTransforms.get(jointId));
//            //joint.globalTransform.set(instance.model.inverseBoneTransforms.get(jointId));
//            jointId++;
//        }

        // todo local copy per instance, now all instances will animate in sync

        if(animationDesc.loopCount == 0)
            animationDesc = null;
        return animationDesc;
    }



    private void updateNodeTransform(Node node, Vector3 tra, Quaternion rot, Vector3 scl){
        node.localTransform.idt().translate(tra).rotate(rot).scale(scl);
        node.isAnimated = true;
        node.updateMatrices(true); // update this node and its children
    }
}
