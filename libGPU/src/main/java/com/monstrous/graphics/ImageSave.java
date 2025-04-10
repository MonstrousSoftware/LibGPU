package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.graphics.webgpu.Buffer;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

/** Temporary class to save image to file.
 * Do we add this to Texture? Introduce PixmapIO?
 */
public class ImageSave {

    public static int saveToPNG(String filename, byte[] pixels, int width, int height, int numComponents){
        Pointer data = JavaWebGPU.createByteArrayPointer(pixels);
        return JavaWebGPU.getUtils().write_png(filename, width, height, numComponents, data, width*numComponents);
    }

    public static int saveToPNG(String filename, Pointer data, int width, int height, int numComponents){
        return JavaWebGPU.getUtils().write_png(filename, width, height, numComponents, data, width*numComponents);
    }

    public static int saveToPNG(String filename, Texture texture, int mipLevel){

        int width = texture.getWidth()/(1 << mipLevel);
        int height = texture.getHeight()/(1 << mipLevel);
        int numComponents = Texture.numComponents(texture.getFormat());
        int bytesPerRow = width * numComponents;
        long bufferSize = (long) bytesPerRow * height;

        // create a buffer to hold the image data
        Buffer buffer = new Buffer("image save buffer", WGPUBufferUsage.MapRead|WGPUBufferUsage.CopyDst,  bufferSize);

        // create a command encoder
        WGPUCommandEncoderDescriptor encoderDesc = WGPUCommandEncoderDescriptor.createDirect();
        encoderDesc.setNextInChain();
        Pointer encoder = LibGPU.webGPU.wgpuDeviceCreateCommandEncoder(LibGPU.device, encoderDesc);

        // texture to copy
        WGPUImageCopyTexture copyTexture = WGPUImageCopyTexture.createDirect()
                .setTexture(texture.getHandle())
                .setMipLevel(mipLevel)
                .setAspect(WGPUTextureAspect.All);

        // copy destination buffer
        WGPUImageCopyBuffer copyBuffer = WGPUImageCopyBuffer.createDirect()
                .setBuffer(buffer.getHandle());
        copyBuffer.getLayout().setNextInChain();
        copyBuffer.getLayout().setBytesPerRow(bytesPerRow);
        copyBuffer.getLayout().setOffset(0);
        copyBuffer.getLayout().setRowsPerImage(height);

        // size of copy
        WGPUExtent3D extent = WGPUExtent3D.createDirect().setWidth(width).setHeight(height).setDepthOrArrayLayers(1);

        // copy texture to buffer
        LibGPU.webGPU.wgpuCommandEncoderCopyTextureToBuffer(encoder, copyTexture, copyBuffer, extent);

        // finish the encoder to give us command buffer
        WGPUCommandBufferDescriptor bufferDescriptor = WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        Pointer commandBuffer = LibGPU.webGPU.wgpuCommandEncoderFinish(encoder, bufferDescriptor);
        LibGPU.webGPU.wgpuCommandEncoderRelease(encoder);

        // feed the command buffer to the queue
        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = JavaWebGPU.createLongArrayPointer(buffers);
        LibGPU.webGPU.wgpuQueueSubmit(LibGPU.queue, 1, bufferPtr);

        // map buffer
        boolean[] done = { false };
        WGPUBufferMapCallback callback = (WGPUBufferMapAsyncStatus status, Pointer userdata) -> {
            if (status == WGPUBufferMapAsyncStatus.Success) {
                Pointer buf = LibGPU.webGPU.wgpuBufferGetConstMappedRange(buffer.getHandle(), 0, bufferSize);
                saveToPNG(filename, buf, width, height, numComponents);
                LibGPU.webGPU.wgpuBufferUnmap(buffer.getHandle());
            } else
                System.out.println("Buffer map async error: "+status);
            done[0] = true; // signal that the call back was executed
        };

        // note: there is a newer function for this and using this one will raise a warning: "Old MapAsync APIs are deprecated"
        // but it requires a struct containing a pointer to a callback function...
        LibGPU.webGPU.wgpuBufferMapAsync(buffer.getHandle(), WGPUMapMode.Read, 0, bufferSize, callback, null);


        // wait for mapping
        while(!done[0])
            LibGPU.webGPU.wgpuDeviceTick(LibGPU.device);   // Dawn

        // cleanup
        LibGPU.webGPU.wgpuCommandEncoderRelease(encoder);
        LibGPU.webGPU.wgpuCommandBufferRelease(commandBuffer);

        buffer.dispose();

        return 1;
    }
}
