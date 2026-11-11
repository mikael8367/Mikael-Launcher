package com.mikael.launcher.engine.horizon

/**
 * Thin JNI boundary for the experimental Horizon Rust Android bootstrap.
 *
 * This bridge reports JNI/session state and the result of the experimental Winit/WGPU
 * Android surface probe. It does not start Horizon gameplay or load Minecraft worlds.
 */
object HorizonNativeBridge {
    private const val LIBRARY_NAME = "horizon_android"

    @Volatile
    private var loadFailure: String? = null

    @Volatile
    private var loaded = false

    @Synchronized
    fun load(): Boolean {
        if (loaded) return true
        if (loadFailure != null) return false
        return try {
            System.loadLibrary(LIBRARY_NAME)
            loaded = true
            true
        } catch (error: UnsatisfiedLinkError) {
            loadFailure = error.message ?: "Biblioteca nativa incompatível ou ausente."
            false
        } catch (error: SecurityException) {
            loadFailure = error.message ?: "O Android bloqueou o carregamento da biblioteca."
            false
        }
    }

    fun status(): String? = if (load()) nativeGetStatus() else null

    fun capabilities(): String? = if (load()) nativeGetCapabilities() else null

    fun createSession(width: Int, height: Int): Long {
        require(width > 0 && height > 0) { "A superfície precisa ter dimensões positivas." }
        check(load()) { loadFailure ?: "Biblioteca Horizon indisponível." }
        return nativeCreateSession(width, height)
    }

    fun destroySession(handle: Long): Boolean {
        if (handle <= 0L || !load()) return false
        return nativeDestroySession(handle)
    }

    fun failureReason(): String? = loadFailure

    @JvmStatic private external fun nativeGetStatus(): String
    @JvmStatic private external fun nativeGetCapabilities(): String
    @JvmStatic private external fun nativeCreateSession(width: Int, height: Int): Long
    @JvmStatic private external fun nativeDestroySession(handle: Long): Boolean
}
