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

import com.monstrous.webgpu.WGPUBackendType;

public class ApplicationConfiguration {
    public int width;
    public int height;
    public String title;
    public boolean vsyncEnabled;
    public int numSamples;
    public WGPUBackendType backend;
    public boolean enableGPUtiming;     // enable for GPU performance measurements
    public boolean noWindow;    // run without a window, e.g. for a compute shader app

    public ApplicationConfiguration() {
        // set to defaults
        width = 640;
        height = 480;
        title = "Application";
        vsyncEnabled = true;
        numSamples = 1;
        backend = WGPUBackendType.D3D12;
        enableGPUtiming = false;
        noWindow = false;
    }

    public void setSize(int w, int h){
        this.width = w;
        this.height = h;
    }
}
