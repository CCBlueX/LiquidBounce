package net.janrupf.ujr.example.glfw.web;

import net.janrupf.ujr.api.event.UlKeyCode;
import net.janrupf.ujr.api.event.UlKeyEventModifiers;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;

/**
 * Helper to translate GLFW keyboard events to Ultralight keyboard events.
 */
public class KeyboardTranslator {
    /**
     * Translates GLFW key modifiers to Ultralight key modifiers.
     *
     * @param mods the GLFW key modifiers bitfield
     * @return the Ultralight key modifiers
     */
    public static EnumSet<UlKeyEventModifiers> glfwModifiersToUltralight(int mods) {
        EnumSet<UlKeyEventModifiers> result = EnumSet.noneOf(UlKeyEventModifiers.class);

        if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) {
            result.add(UlKeyEventModifiers.SHIFT);
        }

        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            result.add(UlKeyEventModifiers.CTRL);
        }

        if ((mods & GLFW.GLFW_MOD_ALT) != 0) {
            result.add(UlKeyEventModifiers.ALT);
        }

        if ((mods & GLFW.GLFW_MOD_SUPER) != 0) {
            result.add(UlKeyEventModifiers.META);
        }

        return result;
    }

    /**
     * Translates GLFW key codes to Ultralight key codes.
     *
     * @param glfwKeyCode the GLFW key code
     * @return the Ultralight key code, or {@link UlKeyCode#UNKNOWN} if the key code is not supported
     */
    public static int glfwKeyToUltralight(int glfwKeyCode) {
        switch (glfwKeyCode) {
            case GLFW.GLFW_KEY_SPACE:
                return UlKeyCode.SPACE;
            case GLFW.GLFW_KEY_APOSTROPHE:
                return UlKeyCode.OEM_7;
            case GLFW.GLFW_KEY_COMMA:
                return UlKeyCode.OEM_COMMA;
            case GLFW.GLFW_KEY_MINUS:
                return UlKeyCode.OEM_MINUS;
            case GLFW.GLFW_KEY_PERIOD:
                return UlKeyCode.OEM_PERIOD;
            case GLFW.GLFW_KEY_SLASH:
                return UlKeyCode.OEM_2;
            case GLFW.GLFW_KEY_0:
                return UlKeyCode.NUMBER_0;
            case GLFW.GLFW_KEY_1:
                return UlKeyCode.NUMBER_1;
            case GLFW.GLFW_KEY_2:
                return UlKeyCode.NUMBER_2;
            case GLFW.GLFW_KEY_3:
                return UlKeyCode.NUMBER_3;
            case GLFW.GLFW_KEY_4:
                return UlKeyCode.NUMBER_4;
            case GLFW.GLFW_KEY_5:
                return UlKeyCode.NUMBER_5;
            case GLFW.GLFW_KEY_6:
                return UlKeyCode.NUMBER_6;
            case GLFW.GLFW_KEY_7:
                return UlKeyCode.NUMBER_7;
            case GLFW.GLFW_KEY_8:
                return UlKeyCode.NUMBER_8;
            case GLFW.GLFW_KEY_9:
                return UlKeyCode.NUMBER_9;
            case GLFW.GLFW_KEY_SEMICOLON:
                return UlKeyCode.OEM_1;
            case GLFW.GLFW_KEY_EQUAL:
            case GLFW.GLFW_KEY_KP_EQUAL:
                return UlKeyCode.OEM_PLUS;
            case GLFW.GLFW_KEY_A:
                return UlKeyCode.A;
            case GLFW.GLFW_KEY_B:
                return UlKeyCode.B;
            case GLFW.GLFW_KEY_C:
                return UlKeyCode.C;
            case GLFW.GLFW_KEY_D:
                return UlKeyCode.D;
            case GLFW.GLFW_KEY_E:
                return UlKeyCode.E;
            case GLFW.GLFW_KEY_F:
                return UlKeyCode.F;
            case GLFW.GLFW_KEY_G:
                return UlKeyCode.G;
            case GLFW.GLFW_KEY_H:
                return UlKeyCode.H;
            case GLFW.GLFW_KEY_I:
                return UlKeyCode.I;
            case GLFW.GLFW_KEY_J:
                return UlKeyCode.J;
            case GLFW.GLFW_KEY_K:
                return UlKeyCode.K;
            case GLFW.GLFW_KEY_L:
                return UlKeyCode.L;
            case GLFW.GLFW_KEY_M:
                return UlKeyCode.M;
            case GLFW.GLFW_KEY_N:
                return UlKeyCode.N;
            case GLFW.GLFW_KEY_O:
                return UlKeyCode.O;
            case GLFW.GLFW_KEY_P:
                return UlKeyCode.P;
            case GLFW.GLFW_KEY_Q:
                return UlKeyCode.Q;
            case GLFW.GLFW_KEY_R:
                return UlKeyCode.R;
            case GLFW.GLFW_KEY_S:
                return UlKeyCode.S;
            case GLFW.GLFW_KEY_T:
                return UlKeyCode.T;
            case GLFW.GLFW_KEY_U:
                return UlKeyCode.U;
            case GLFW.GLFW_KEY_V:
                return UlKeyCode.V;
            case GLFW.GLFW_KEY_W:
                return UlKeyCode.W;
            case GLFW.GLFW_KEY_X:
                return UlKeyCode.X;
            case GLFW.GLFW_KEY_Y:
                return UlKeyCode.Y;
            case GLFW.GLFW_KEY_Z:
                return UlKeyCode.Z;
            case GLFW.GLFW_KEY_LEFT_BRACKET:
                return UlKeyCode.OEM_4;
            case GLFW.GLFW_KEY_BACKSLASH:
                return UlKeyCode.OEM_5;
            case GLFW.GLFW_KEY_RIGHT_BRACKET:
                return UlKeyCode.OEM_6;
            case GLFW.GLFW_KEY_GRAVE_ACCENT:
                return UlKeyCode.OEM_3;
            case GLFW.GLFW_KEY_ESCAPE:
                return UlKeyCode.ESCAPE;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                return UlKeyCode.RETURN;
            case GLFW.GLFW_KEY_TAB:
                return UlKeyCode.TAB;
            case GLFW.GLFW_KEY_BACKSPACE:
                return UlKeyCode.BACK;
            case GLFW.GLFW_KEY_INSERT:
                return UlKeyCode.INSERT;
            case GLFW.GLFW_KEY_DELETE:
                return UlKeyCode.DELETE;
            case GLFW.GLFW_KEY_RIGHT:
                return UlKeyCode.RIGHT;
            case GLFW.GLFW_KEY_LEFT:
                return UlKeyCode.LEFT;
            case GLFW.GLFW_KEY_DOWN:
                return UlKeyCode.DOWN;
            case GLFW.GLFW_KEY_UP:
                return UlKeyCode.UP;
            case GLFW.GLFW_KEY_PAGE_UP:
                return UlKeyCode.PRIOR;
            case GLFW.GLFW_KEY_PAGE_DOWN:
                return UlKeyCode.NEXT;
            case GLFW.GLFW_KEY_HOME:
                return UlKeyCode.HOME;
            case GLFW.GLFW_KEY_END:
                return UlKeyCode.END;
            case GLFW.GLFW_KEY_CAPS_LOCK:
                return UlKeyCode.CAPITAL;
            case GLFW.GLFW_KEY_SCROLL_LOCK:
                return UlKeyCode.SCROLL;
            case GLFW.GLFW_KEY_NUM_LOCK:
                return UlKeyCode.NUMLOCK;
            case GLFW.GLFW_KEY_PRINT_SCREEN:
                return UlKeyCode.SNAPSHOT;
            case GLFW.GLFW_KEY_PAUSE:
                return UlKeyCode.PAUSE;
            case GLFW.GLFW_KEY_F1:
                return UlKeyCode.F1;
            case GLFW.GLFW_KEY_F2:
                return UlKeyCode.F2;
            case GLFW.GLFW_KEY_F3:
                return UlKeyCode.F3;
            case GLFW.GLFW_KEY_F4:
                return UlKeyCode.F4;
            case GLFW.GLFW_KEY_F5:
                return UlKeyCode.F5;
            case GLFW.GLFW_KEY_F6:
                return UlKeyCode.F6;
            case GLFW.GLFW_KEY_F7:
                return UlKeyCode.F7;
            case GLFW.GLFW_KEY_F8:
                return UlKeyCode.F8;
            case GLFW.GLFW_KEY_F9:
                return UlKeyCode.F9;
            case GLFW.GLFW_KEY_F10:
                return UlKeyCode.F10;
            case GLFW.GLFW_KEY_F11:
                return UlKeyCode.F11;
            case GLFW.GLFW_KEY_F12:
                return UlKeyCode.F12;
            case GLFW.GLFW_KEY_F13:
                return UlKeyCode.F13;
            case GLFW.GLFW_KEY_F14:
                return UlKeyCode.F14;
            case GLFW.GLFW_KEY_F15:
                return UlKeyCode.F15;
            case GLFW.GLFW_KEY_F16:
                return UlKeyCode.F16;
            case GLFW.GLFW_KEY_F17:
                return UlKeyCode.F17;
            case GLFW.GLFW_KEY_F18:
                return UlKeyCode.F18;
            case GLFW.GLFW_KEY_F19:
                return UlKeyCode.F19;
            case GLFW.GLFW_KEY_F20:
                return UlKeyCode.F20;
            case GLFW.GLFW_KEY_F21:
                return UlKeyCode.F21;
            case GLFW.GLFW_KEY_F22:
                return UlKeyCode.F22;
            case GLFW.GLFW_KEY_F23:
                return UlKeyCode.F23;
            case GLFW.GLFW_KEY_F24:
                return UlKeyCode.F24;
            case GLFW.GLFW_KEY_KP_0:
                return UlKeyCode.NUMPAD0;
            case GLFW.GLFW_KEY_KP_1:
                return UlKeyCode.NUMPAD1;
            case GLFW.GLFW_KEY_KP_2:
                return UlKeyCode.NUMPAD2;
            case GLFW.GLFW_KEY_KP_3:
                return UlKeyCode.NUMPAD3;
            case GLFW.GLFW_KEY_KP_4:
                return UlKeyCode.NUMPAD4;
            case GLFW.GLFW_KEY_KP_5:
                return UlKeyCode.NUMPAD5;
            case GLFW.GLFW_KEY_KP_6:
                return UlKeyCode.NUMPAD6;
            case GLFW.GLFW_KEY_KP_7:
                return UlKeyCode.NUMPAD7;
            case GLFW.GLFW_KEY_KP_8:
                return UlKeyCode.NUMPAD8;
            case GLFW.GLFW_KEY_KP_9:
                return UlKeyCode.NUMPAD9;
            case GLFW.GLFW_KEY_KP_DECIMAL:
                return UlKeyCode.DECIMAL;
            case GLFW.GLFW_KEY_KP_DIVIDE:
                return UlKeyCode.DIVIDE;
            case GLFW.GLFW_KEY_KP_MULTIPLY:
                return UlKeyCode.MULTIPLY;
            case GLFW.GLFW_KEY_KP_SUBTRACT:
                return UlKeyCode.SUBTRACT;
            case GLFW.GLFW_KEY_KP_ADD:
                return UlKeyCode.ADD;
            case GLFW.GLFW_KEY_LEFT_SHIFT:
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                return UlKeyCode.SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL:
            case GLFW.GLFW_KEY_RIGHT_CONTROL:
                return UlKeyCode.CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT:
            case GLFW.GLFW_KEY_RIGHT_ALT:
                return UlKeyCode.MENU;
            case GLFW.GLFW_KEY_LEFT_SUPER:
                return UlKeyCode.LWIN;
            case GLFW.GLFW_KEY_RIGHT_SUPER:
                return UlKeyCode.RWIN;
            default:
                return UlKeyCode.UNKNOWN;
        }
    }
}
