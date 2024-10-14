@REM compile c source and create a DLL in the working directory
@REM gcc -I"%JAVA_HOME%include" -I"%JAVA_HOME%include\win32" native.c -shared -o ../../../native.dll -Wl,--add-stdcall-alias -L../../../wgpu_native.dll


g++  native.c -I"%JAVA_HOME%include" -I"%JAVA_HOME%include\win32" -I.\include wgpu_native.dll glfw3.dll -shared -o native.dll

g++  nativec.c -I"%JAVA_HOME%include" -I"%JAVA_HOME%include\win32" -I.\include wgpu_native.dll glfw3.dll -shared -o nativec.dll

