package com.monstrous.graphics.g3d;

import com.monstrous.math.Quaternion;
import com.monstrous.math.Vector3;

public class AnimationController {
    public ModelInstance instance;
    public Animation animation;
    public AnimationDesc animationDesc;

    private Vector3 tmpTra;
    private Quaternion tmpQ;
    private Vector3 tmpScl;


    /** keeps animation state */
    public static class AnimationDesc {
        public float time;
        public int loopCount;
        public float speed;
        public float duration;

        public AnimationDesc() {
            time = 0f;
            loopCount = 1;
        }

        public void update(float deltaTime) {
            time += deltaTime * speed;
            if (time > duration) {
                time -= duration;
                loopCount--;
            }
            if (time < 0) {   // in case speed < 0
                time += duration;
                loopCount--;
            }
        }

    }

    public AnimationController(ModelInstance instance) {
        this.instance = instance;
        animationDesc = new AnimationDesc();

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
                if (anim.name.contentEquals(animationId)) {
                    animation = anim;
                    animationDesc.time = 0;
                    animationDesc.loopCount = loopCount;
                    animationDesc.speed = speed;
                    animationDesc.duration = animation.duration;
                    return;
                }
            }
            System.out.println("Animation not found:"+animationId);
        }
        animation = null;
    }

    public void update(float deltaTime){
        if(animation == null)
            return;

        animationDesc.update(deltaTime);


        for(NodeAnimation nodeAnim: animation.nodeAnimations){
            if(nodeAnim.rotation != null){
                NodeKeyframe<Quaternion> prevKey = nodeAnim.rotation.get(0);
                for( NodeKeyframe<Quaternion> keyFrame: nodeAnim.rotation){
                    if(prevKey.keyTime <= animationDesc.time && keyFrame.keyTime > animationDesc.time){
                        float fraction = animationDesc.time - prevKey.keyTime/(keyFrame.keyTime- prevKey.keyTime);
                        tmpQ.set(prevKey.value).slerp(keyFrame.value, fraction);
                        break;
                    }
                    else
                        prevKey = keyFrame;
                }
            }

            nodeAnim.node.isAnimated = true;
            nodeAnim.node.localTransform.set(tmpTra, tmpQ, tmpScl);
        }
        // todo local copy per instance, multiple root nodes
        instance.model.getNodes().get(0).updateMatrices(true);

        if(animationDesc.loopCount == 0)
            animation = null;
    }
}
