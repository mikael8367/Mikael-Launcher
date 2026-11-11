package com.mikael.launcher

import android.app.NativeActivity

/**
 * Separate Android NativeActivity used only by the Horizon Winit/wgpu surface probe.
 * It is not the Minecraft gameplay client yet.
 */
class HorizonNativeActivity : NativeActivity()
