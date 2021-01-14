use std::borrow::Cow;
use std::ffi::CStr;
use std::fmt::{Display, Formatter};
use std::result::Result;

use jni::*;
use jni::objects::{JByteBuffer, JClass, JObject, JString};
use jni::sys::JNI_GetCreatedJavaVMs;
use sciter::windowless::{handle_message, KEY_EVENTS, KEYBOARD_STATES, KeyboardEvent, Message, MOUSE_BUTTONS, MOUSE_EVENTS, MouseEvent, PaintLayer, RenderEvent};

#[derive(Debug)]
struct SciterWrapperError {
    error_text: String
}

impl Display for SciterWrapperError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.error_text)
    }
}
impl std::error::Error for SciterWrapperError {
}

fn get_window_handle_from_long(long: u64) -> sciter::types::HWINDOW {
    unsafe { long as *const u8 as sciter::types::HWINDOW }
}

fn get_element_handle_from_long(long: u64) -> sciter::HELEMENT {
    unsafe { long as *const u8 as sciter::HELEMENT }
}

fn throw_sciter_exception(env: &JNIEnv, msg: String) {
    env.throw_new("net/ccbluex/liquidbounce/sciter/natives/SciterException", msg).unwrap()
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_init0(env: JNIEnv, _class: JClass, window_handle: u64, jni_library_location: JString) {
    match init(&env, jni_library_location, window_handle) {
        Result::Err(e) => { throw_sciter_exception(&env, e.to_string()) }
        Result::Ok(_) => {}
    }
}

#[inline]
fn fix_modifier(mods: u32) -> u32 {
    (mods & (!0x3)) | ((mods & 0x1) << 1) | ((mods & 0x2) >> 1)
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_draw0(env: JNIEnv, _class: JClass, window_handle: u64) {
    handle_message(get_window_handle_from_long(window_handle), Message::Paint(PaintLayer { element: get_element_handle_from_long(window_handle), is_foreground: true }));
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_heartbit0(env: JNIEnv, _class: JClass, window_handle: u64, time_delta: u32) {
   handle_message(get_window_handle_from_long(window_handle), Message::Heartbit { milliseconds: time_delta });
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_setResolution0(env: JNIEnv, _class: JClass, window_handle: u64, ppi: u32) {
    handle_message(get_window_handle_from_long(window_handle), Message::Resolution { ppi });
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_setSize0(env: JNIEnv, _class: JClass, window_handle: u64, width: u32, height: u32) {
    handle_message(get_window_handle_from_long(window_handle), Message::Size { width, height });
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_destroy0(env: JNIEnv, _class: JClass, window_handle: u64) {
    handle_message(get_window_handle_from_long(window_handle), Message::Destroy);
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_loadHTML0(env: JNIEnv, _class: JClass, window_handle: u64, html: JString, uri: JString) {
    match load_html(&env, window_handle, html, uri) {
        Result::Err(e) => { throw_sciter_exception(&env, e.to_string()) }
        Result::Ok(_) => {}
    }
}

#[inline]
fn is_null(obj: &JObject) -> bool {
    obj.is_null()
}

fn load_html(env: &JNIEnv, window_handle: u64, html: JString, uri: JString) -> Result<(), Box<dyn std::error::Error>> {
    let instance = sciter::Host::attach(get_window_handle_from_long(window_handle));

    let html_bytes = env.get_string(html)?;

    if is_null(&uri) {
        instance.load_html(html_bytes.to_bytes(), None);
    } else {
        let str: String = env.get_string(uri)?.into();

        instance.load_html(html_bytes.to_bytes(), Some(&str));
    }

    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_render0(env: JNIEnv, _class: JClass, window_handle: u64, framebuffer_pointer: u64, framebuffer_size: u32) {
    let on_render = move |bitmap_area: &sciter::types::RECT, bitmap_data: &[u8]|
        {
            let output: &mut [u8] = unsafe { std::slice::from_raw_parts_mut(framebuffer_pointer as *mut u8, framebuffer_size as usize) };

            if bitmap_data.len() != framebuffer_size as usize {
                eprintln!("Sciter returned an invalid sized framebuffer");
                return;
            }

            output.copy_from_slice(bitmap_data);

            // brga_to_rgba(input, output);
        };


    let cb = RenderEvent {
        layer: None,
        callback: Box::new(on_render),
    };

    handle_message(get_window_handle_from_long(window_handle), Message::RenderTo(cb));
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_mouseEvent0(env: JNIEnv, _class: JClass, window_handle: u64, x: i32, y: i32, button: u32) {
    let event = MouseEvent {
        event: MOUSE_EVENTS::MOUSE_MOVE,
        button: match button {
            0 => MOUSE_BUTTONS::NONE,
            1 => MOUSE_BUTTONS::MAIN,
            2 => MOUSE_BUTTONS::PROP,
            3 => MOUSE_BUTTONS::MIDDLE,
            _ => {
                throw_sciter_exception(&env, String::from("Invalid mouse button"));
                return;
            }
        },
        modifiers: KEYBOARD_STATES::from(0),
        pos: sciter::types::POINT {
            x,
            y,
        },
    };

    handle_message(get_window_handle_from_long(window_handle), Message::Mouse(event));
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_mouseButtonEvent0(env: JNIEnv, _class: JClass, window_handle: u64, x: i32, y: i32, keyboard_state: u32, button: u32, released: bool) {
    let event = MouseEvent {
        event: if released { MOUSE_EVENTS::MOUSE_UP } else { MOUSE_EVENTS::MOUSE_DOWN },
        button: match button {
            0 => MOUSE_BUTTONS::NONE,
            1 => MOUSE_BUTTONS::MAIN,
            2 => MOUSE_BUTTONS::PROP,
            3 => MOUSE_BUTTONS::MIDDLE,
            _ => {
                throw_sciter_exception(&env, String::from("Invalid mouse button"));
                return;
            }
        },
        modifiers: KEYBOARD_STATES::from(fix_modifier(keyboard_state)),
        pos: sciter::types::POINT {
            x,
            y,
        },
    };

    handle_message(get_window_handle_from_long(window_handle), Message::Mouse(event));
}

#[no_mangle]
pub extern "system" fn Java_net_ccbluex_liquidbounce_sciter_natives_SciterNative_keyEvent0(env: JNIEnv, _class: JClass, window_handle: u64, scancode: u32, keyboard_state: u32, event_type: u32) {
    let event = KeyboardEvent {
        event: match event_type {
            0 => KEY_EVENTS::KEY_DOWN,
            1 => KEY_EVENTS::KEY_UP,
            2 => KEY_EVENTS::KEY_CHAR,
            _ => {
                throw_sciter_exception(&env, String::from("Invalid event type"));
                return;
            }
        },
        code: scancode,
        modifiers: KEYBOARD_STATES::from(fix_modifier(keyboard_state)),
    };

    handle_message(get_window_handle_from_long(window_handle), Message::Focus { enter: true });
    handle_message(get_window_handle_from_long(window_handle), Message::Keyboard(event));
}

fn init(env: &JNIEnv, jni_library_location: JString, window_handle: u64) -> Result<(), Box<dyn std::error::Error>> {
    let library_location: String = env.get_string(jni_library_location)?.into();

    if let Err(_) = sciter::set_options(sciter::RuntimeOptions::LibraryPath(library_location.as_ref())) {
        return Err(Box::new(SciterWrapperError { error_text: String::from("Sciter lite failed to load.") }));
    }

    // let window_handle = wnd.raw_window_handle();

    // configure Sciter
    sciter::set_options(sciter::RuntimeOptions::UxTheming(true)).unwrap();
    sciter::set_options(sciter::RuntimeOptions::DebugMode(true)).unwrap();
    sciter::set_options(sciter::RuntimeOptions::ScriptFeatures(0xFF)).unwrap();
    //
    // // create an engine instance with an opaque pointer as an identifier
    use sciter::windowless::{Message, handle_message};

    let scwnd = get_window_handle_from_long(window_handle);

    handle_message(scwnd, Message::Create { backend: sciter::types::GFX_LAYER::SKIA_OPENGL, transparent: true });

    return Ok(());
}