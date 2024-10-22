
#include <stdio.h>


// Include WebGPU header
#include "webgpu/webgpu.h"


#include "org_example_Main.h"

//#define nullptr ((void*)0)

/*
 * Class:     org_example_Main
 * Method:    add
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_example_Main_add
  (JNIEnv * env, jclass clazz, jint a, jint b){
    return (jint)( (int) a + (int)b );
  }

/*
 * Class:     org_example_Main
 * Method:    WGPUCreateInstance
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_example_Main_WGPUCreateInstance
  (JNIEnv * env, jclass clazz){
        printf("creating instance\n");

        WGPUInstance instance = wgpuCreateInstance(nullptr);
        return (jlong)instance;
  }

/*
 * Class:     org_example_Main
 * Method:    WGPUInstanceRelease
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_example_Main_WGPUInstanceRelease
  (JNIEnv * env, jclass clazz, jlong instance){
        printf("release instance %ld \n", instance);

        wgpuInstanceRelease((WGPUInstance)instance);
  }
