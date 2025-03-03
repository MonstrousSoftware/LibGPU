/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.monstrous;

import com.monstrous.webgpu.WGPUSupportedLimits;
import com.monstrous.webgpu.WGPUTextureFormat;
import com.monstrous.webgpu.WebGPU_JNI;
import jnr.ffi.Pointer;

public class LibGPU {
    public static Application app;
    public static Input input;
    public static Graphics graphics;
    public static WebGPU_JNI webGPU;

    // put the following under wgpu?
    public static Pointer instance;
    public static Pointer surface;
    public static WGPUTextureFormat surfaceFormat = WGPUTextureFormat.Undefined;
    public static WGPUSupportedLimits supportedLimits;
    public static Pointer device;
    public static Pointer queue;
}
