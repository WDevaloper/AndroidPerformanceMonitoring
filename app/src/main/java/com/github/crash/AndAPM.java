package com.github.crash;


//Java <- apm_bridge.cpp -> C++
public class AndAPM {
    static {
        System.loadLibrary("apm");
    }

    long nativeHandle;

    void init() {
        nativeHandle = nativeInit();
    }

    void start() {
        nativeStart(nativeHandle);
    }

    void stop() {
        nativeStop(nativeHandle);
    }

    void destroy() {
        nativeDestroy(nativeHandle);
        nativeHandle = 0;
    }

    private static native long nativeInit();

    private native void nativeStart(long nativeHandle);

    private native void nativeStop(long nativeHandle);

    private native void nativeDestroy(long nativeHandle);

}
