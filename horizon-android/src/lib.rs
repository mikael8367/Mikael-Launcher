use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{JNI_FALSE, JNI_TRUE, jboolean, jint, jlong, jstring};
use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, AtomicI64, Ordering};
use std::sync::{Mutex, OnceLock};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
struct Session {
    width: u32,
    height: u32,
}

static NEXT_HANDLE: AtomicI64 = AtomicI64::new(1);
static SESSIONS: OnceLock<Mutex<HashMap<i64, Session>>> = OnceLock::new();
static GPU_VALIDATED: AtomicBool = AtomicBool::new(false);
static GPU_BACKEND: OnceLock<Mutex<String>> = OnceLock::new();

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
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let active_sessions = sessions().lock().map(|items| items.len()).unwrap_or(0);
    let status = format!(
        r#"{{"library":"loaded","stage":"android-surface-prototype","renderer":"wgpu-surface-clear","activeSessions":{active_sessions},"minecraftClient":false}}"#
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
    let gpu_validated = GPU_VALIDATED.load(Ordering::Relaxed);
    let backend = GPU_BACKEND
        .get()
        .and_then(|value| value.lock().ok().map(|value| value.clone()))
        .unwrap_or_else(|| "not-tested".to_string());
    let capabilities = format!(
        r#"{{"abi":"arm64-v8a","jni":true,"nativeActivity":true,"winit":true,"wgpu":true,"graphicsBackend":"{backend}","gpuValidated":{gpu_validated},"touchInput":false,"minecraftRuntime":false}}"#
    );

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

#[cfg(target_os = "android")]
mod android_surface {
    use std::sync::Arc;
    use std::time::{Duration, Instant};
    use winit::{
        application::ApplicationHandler,
        event::WindowEvent,
        event_loop::{ActiveEventLoop, ControlFlow, EventLoop},
        platform::android::{EventLoopBuilderExtAndroid, activity::AndroidApp},
        window::{Window, WindowId},
    };

    struct GpuState {
        _instance: wgpu::Instance,
        surface: wgpu::Surface<'static>,
        device: wgpu::Device,
        queue: wgpu::Queue,
        config: wgpu::SurfaceConfiguration,
    }

    #[derive(Default)]
    struct HorizonSurfaceApp {
        window: Option<Arc<Window>>,
        gpu: Option<GpuState>,
        startup_error: Option<String>,
        next_frame: Option<Instant>,
    }

    impl HorizonSurfaceApp {
        fn initialize(&mut self, event_loop: &ActiveEventLoop) -> Result<(), String> {
            let window = Arc::new(
                event_loop
                    .create_window(
                        Window::default_attributes().with_title("Horizon Rust • Android renderer"),
                    )
                    .map_err(|error| format!("Falha ao criar janela Android: {error}"))?,
            );
            let size = window.inner_size();
            let instance = wgpu::Instance::new(wgpu::InstanceDescriptor::default());
            let surface = instance
                .create_surface(window.clone())
                .map_err(|error| format!("Falha ao criar superfície Winit: {error}"))?;
            let adapter =
                pollster::block_on(instance.request_adapter(&wgpu::RequestAdapterOptions {
                    power_preference: wgpu::PowerPreference::HighPerformance,
                    compatible_surface: Some(&surface),
                    force_fallback_adapter: false,
                }))
                .map_err(|error| format!("Nenhum adaptador gráfico compatível: {error}"))?;
            let (device, queue) =
                pollster::block_on(adapter.request_device(&wgpu::DeviceDescriptor::default()))
                    .map_err(|error| format!("Falha ao criar dispositivo gráfico: {error}"))?;
            let adapter_backend = format!("{:?}", adapter.get_info().backend);
            let config = surface
                .get_default_config(&adapter, size.width.max(1), size.height.max(1))
                .ok_or_else(|| {
                    "A GPU não oferece configuração de apresentação para esta superfície."
                        .to_string()
                })?;
            surface.configure(&device, &config);
            GPU_VALIDATED.store(true, Ordering::Relaxed);
            let backend_store = GPU_BACKEND.get_or_init(|| Mutex::new(String::new()));
            if let Ok(mut value) = backend_store.lock() {
                *value = adapter_backend;
            }

            self.window = Some(window.clone());
            self.gpu = Some(GpuState {
                _instance: instance,
                surface,
                device,
                queue,
                config,
            });
            self.next_frame = Some(Instant::now());
            Ok(())
        }

        fn draw(&mut self) {
            let Some(gpu) = &self.gpu else {
                return;
            };
            match gpu.surface.get_current_texture() {
                wgpu::CurrentSurfaceTexture::Success(frame)
                | wgpu::CurrentSurfaceTexture::Suboptimal(frame) => {
                    let view = frame
                        .texture
                        .create_view(&wgpu::TextureViewDescriptor::default());
                    let mut encoder =
                        gpu.device
                            .create_command_encoder(&wgpu::CommandEncoderDescriptor {
                                label: Some("horizon-android-surface-probe"),
                            });
                    {
                        let _pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                            label: Some("horizon-android-clear-pass"),
                            color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                                view: &view,
                                depth_slice: None,
                                resolve_target: None,
                                ops: wgpu::Operations {
                                    load: wgpu::LoadOp::Clear(wgpu::Color {
                                        r: 0.025,
                                        g: 0.12,
                                        b: 0.09,
                                        a: 1.0,
                                    }),
                                    store: wgpu::StoreOp::Store,
                                },
                            })],
                            depth_stencil_attachment: None,
                            timestamp_writes: None,
                            occlusion_query_set: None,
                        });
                    }
                    gpu.queue.submit(Some(encoder.finish()));
                    frame.present();
                }
                wgpu::CurrentSurfaceTexture::Outdated | wgpu::CurrentSurfaceTexture::Lost => {
                    gpu.surface.configure(&gpu.device, &gpu.config);
                }
                wgpu::CurrentSurfaceTexture::Timeout
                | wgpu::CurrentSurfaceTexture::Occluded
                | wgpu::CurrentSurfaceTexture::Validation => {}
            }
        }
    }

    impl ApplicationHandler for HorizonSurfaceApp {
        fn resumed(&mut self, event_loop: &ActiveEventLoop) {
            if self.window.is_some() {
                return;
            }
            if let Err(error) = self.initialize(event_loop) {
                self.startup_error = Some(error);
                event_loop.exit();
            }
        }

        fn about_to_wait(&mut self, event_loop: &ActiveEventLoop) {
            let now = Instant::now();
            let next_frame = self.next_frame.get_or_insert(now);
            if now >= *next_frame {
                if let Some(window) = self.window.as_ref() {
                    window.request_redraw();
                }
                *next_frame = now + Duration::from_millis(33);
            }
            event_loop.set_control_flow(ControlFlow::WaitUntil(*next_frame));
        }

        fn window_event(
            &mut self,
            event_loop: &ActiveEventLoop,
            _window_id: WindowId,
            event: WindowEvent,
        ) {
            match event {
                WindowEvent::CloseRequested => event_loop.exit(),
                WindowEvent::Resized(size) => {
                    if size.width > 0 && size.height > 0 {
                        if let Some(gpu) = self.gpu.as_mut() {
                            gpu.config.width = size.width;
                            gpu.config.height = size.height;
                            gpu.surface.configure(&gpu.device, &gpu.config);
                        }
                    }
                }
                WindowEvent::RedrawRequested => self.draw(),
                _ => {}
            }
        }
    }

    pub fn run(app: AndroidApp) {
        let mut builder = EventLoop::<()>::builder();
        builder.with_android_app(app);
        let event_loop = match builder.build() {
            Ok(event_loop) => event_loop,
            Err(error) => {
                eprintln!("Horizon Android: não foi possível iniciar o event loop: {error}");
                return;
            }
        };
        event_loop.set_control_flow(ControlFlow::Wait);
        let mut application = HorizonSurfaceApp::default();
        if let Err(error) = event_loop.run_app(&mut application) {
            eprintln!("Horizon Android: erro no event loop: {error}");
        }
        if let Some(error) = application.startup_error {
            eprintln!("Horizon Android: {error}");
        }
    }
}

#[cfg(target_os = "android")]
#[unsafe(no_mangle)]
pub fn android_main(app: winit::platform::android::activity::AndroidApp) {
    android_surface::run(app);
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
