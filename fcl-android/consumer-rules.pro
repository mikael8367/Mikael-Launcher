# Keep JNI bridge and callbacks referenced by native FCL code.
-keep class com.tungsten.fclauncher.bridge.** { *; }
-keep class com.tungsten.fcl.activity.JVMActivity { *; }
