use jni::objects::JClass;
use jni::sys::{jboolean, jint, jlong, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::collections::HashMap;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Mutex, OnceLock};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
struct Session {
    width: u32,
    height: u32,
}

static NEXT_HANDLE: AtomicI64 = AtomicI64::new(1);
static SESSIONS: OnceLock<Mutex<HashMap<i64, Session>>> = OnceLock::new();

fn sessions() -> &'static Mutex<HashMap<i64, Session>> {
    SESSIONS.get_or_init(|| Mutex::new(HashMap::new()))
}

fn create_session(width: i32, height: i32) -> i64 {
    if width <= 0 || height <= 0 {
        return 0;
    }

    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    let Ok(mut active) = sessions().lock() else {
        return 0;
    };

    active.insert(
        handle,
        Session {
            width: width as u32,
            height: height as u32,
        },
    );
    handle
}

fn destroy_session(handle: i64) -> bool {
    if handle <= 0 {
        return false;
    }

    sessions()
        .lock()
        .map(|mut active| active.remove(&handle).is_some())
        .unwrap_or(false)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_mikael_launcher_engine_horizon_HorizonNativeBridge_nativeGetStatus(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let active_sessions = sessions().lock().map(|items| items.len()).unwrap_or(0);
    let status = format!(
        r#"{{"library":"loaded","stage":"jni-prototype","renderer":"not-integrated","activeSessions":{active_sessions}}}"#
    );

    env.new_string(status)
        .map(|value| value.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_mikael_launcher_engine_horizon_HorizonNativeBridge_nativeGetCapabilities(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let capabilities = r#"{"abi":"arm64-v8a","jni":true,"vulkan":false,"gles":false,"touchInput":false,"minecraftRuntime":false}"#;

    env.new_string(capabilities)
        .map(|value| value.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_mikael_launcher_engine_horizon_HorizonNativeBridge_nativeCreateSession(
    _env: JNIEnv,
    _class: JClass,
    width: jint,
    height: jint,
) -> jlong {
    create_session(width, height) as jlong
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_mikael_launcher_engine_horizon_HorizonNativeBridge_nativeDestroySession(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    if destroy_session(handle as i64) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn session_requires_positive_surface_dimensions() {
        assert_eq!(create_session(0, 480), 0);
        assert_eq!(create_session(640, -1), 0);
    }

    #[test]
    fn session_can_be_created_and_destroyed_once() {
        let handle = create_session(1280, 720);
        assert!(handle > 0);
        assert!(destroy_session(handle));
        assert!(!destroy_session(handle));
    }

    #[test]
    fn invalid_handles_are_rejected() {
        assert!(!destroy_session(0));
        assert!(!destroy_session(-5));
    }
}
