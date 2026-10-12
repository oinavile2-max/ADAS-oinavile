package git.my.camera.core;

/** JNI descriptors reproduced from the user-supplied APK; implementation is our adapter. */
public final class MyCamera {
    private static boolean loaded;
    public static synchronized String loadAndDescribe(android.content.Context context) {
        if (!loaded) { System.loadLibrary("mycamera"); loaded=true; }
        nativeInitApplicationContext(context.getApplicationContext());
        return nativeVersion();
    }
    public native void nativeAddPreview(long p0, int p1, int p2, android.view.Surface p3);
    public static native boolean nativeClearHalt(int p0, int p1);
    public native void nativeConnect(long p0, java.lang.String p1, int p2);
    public static native long nativeCreateUFD(NativeCameraCallback p0);
    public static native long nativeCreateUVC(NativeCameraCallback p0);
    public static native void nativeCropI420(byte[] p0, int p1, int p2, byte[] p3, int p4, int p5, int p6, int p7);
    public static native void nativeDestroy(long p0);
    public native void nativeDisconnect(long p0);
    public static native int nativeErrno();
    public static native java.lang.String nativeErrstr();
    public static native long nativeGetCurrentTimeMillis();
    public static native long nativeGetRunningTimeMillis();
    public static native void nativeInitApplicationContext(android.content.Context p0);
    public native boolean nativeIsConnected(long p0);
    public native boolean nativeIsDisconnected(long p0);
    public native boolean nativeIsInitialized(long p0);
    public native int nativePost(long p0, int p1, int p2, int p3, java.lang.String p4);
    public static native void nativePreviewAddPreview(long p0, int p1, android.view.Surface p2);
    public static native long nativePreviewCreate(NativePreviewCallback p0);
    public static native void nativePreviewDestroy(long p0);
    public static native void nativePreviewOnFrame(long p0, byte[] p1, int p2);
    public static native void nativePreviewRemovePreview(long p0, int p1);
    public native void nativeRemovePreview(long p0, int p1, int p2);
    public static native boolean nativeReset(int p0);
    public static native java.lang.String nativeVersion();
    public interface NativeCameraCallback {
        public java.lang.String event2String(int p0, int p1);
        public void onAStreamFrame(byte[] p0, int p1);
        public void onAStreamStarted(int p0, int p1, int p2, java.lang.String p3);
        public void onAStreamStopped();
        public void onCallback(int p0, int p1, java.lang.String p2);
        public void onLog(int p0, java.lang.String p1);
        public void onPostedCallback(int p0, int p1, int p2, int p3, java.lang.String p4);
        public void onSharedSrcClosed();
        public void onSharedSrcOpened(android.os.MemoryFile p0, int p1, int p2);
        public void onSharedSrcUpdated();
        public void onSharedYuvClosed();
        public void onSharedYuvOpened(android.os.MemoryFile p0, int p1, int p2);
        public void onSharedYuvUpdated();
        public void onVStreamFrame(byte[] p0, int p1);
        public void onVStreamStarted(int p0, int p1, int p2, java.lang.String p3);
        public void onVStreamStopped();
        public int umsdChunkSize();
        public java.lang.String umsdOpen(java.lang.String p0);
        public int umsdRead(byte[] p0, int p1, int p2, int p3);
        public void umsdSeek(int p0);
        public int umsdSize();
        public void umsdWrite(byte[] p0, int p1);
    }
    public interface NativePreviewCallback {
        public void onPreview(int p0, int p1);
    }
}
