package com.monstrous.graphics.g3d;

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

    public AnimationDesc update(float deltaTime){
        if(animationDesc == null)
            return null;
        animationDesc.update(deltaTime);
        if(animationDesc.time > animationDesc.duration)
            throw new RuntimeException("Animation time out of bounds");
        tmpQ.idt();
        tmpScl.set(1,1,1);
        tmpTra.set(0,0,0);
        for(NodeAnimation nodeAnim: animationDesc.animation.nodeAnimations){
            if(nodeAnim.rotation != null){
                NodeKeyframe<Quaternion> prevKey = nodeAnim.rotation.get(0);
                for( NodeKeyframe<Quaternion> keyFrame: nodeAnim.rotation){
                    if(prevKey.keyTime <= animationDesc.time && keyFrame.keyTime > animationDesc.time){
                        float fraction = (animationDesc.time - prevKey.keyTime)/(keyFrame.keyTime- prevKey.keyTime);
                        tmpQ.set(prevKey.value).slerp(keyFrame.value, fraction);
                        break;
                    }
                    else
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

            nodeAnim.node.isAnimated = true;
            nodeAnim.node.localTransform.set(tmpTra, tmpQ, tmpScl);
            //System.out.println("tra: "+tmpTra.y);
        }
        // todo local copy per instance
        for(Node rootNode : instance.model.getNodes())
            rootNode.updateMatrices(true);


        if(animationDesc.loopCount == 0)
            animationDesc = null;
        return animationDesc;
    }
}
