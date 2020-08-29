// Base.java

package net.ccbluex.liquidbounce.utils.lzma.LZMA;

public class Base {
    public static final int kNumStates = 12;
    public static final int kNumPosSlotBits = 6;
    public static final int kNumLenToPosStatesBits = 2; // it's for speed optimization
    public static final int kNumLenToPosStates = 1 << kNumLenToPosStatesBits;
    public static final int kMatchMinLen = 2;
    public static final int kNumAlignBits = 4;
    public static final int kStartPosModelIndex = 4;
    public static final int kEndPosModelIndex = 14;
    public static final int kNumFullDistances = 1 << (kEndPosModelIndex / 2);
    public static final int kNumLitContextBitsMax = 8;
    public static final int kNumPosStatesBitsMax = 4;
    public static final int kNumPosStatesMax = (1 << kNumPosStatesBitsMax);
    public static final int kNumLowLenBits = 3;
    public static final int kNumMidLenBits = 3;
    public static final int kNumHighLenBits = 8;
    public static final int kNumLowLenSymbols = 1 << kNumLowLenBits;
    public static final int kNumMidLenSymbols = 1 << kNumMidLenBits;

    public static int StateInit() {
        return 0;
    }

    public static int StateUpdateChar(int index) {
        if (index < 4)
            return 0;
        if (index < 10)
            return index - 3;
        return index - 6;
    }

    public static int StateUpdateMatch(int index) {
        return (index < 7 ? 7 : 10);
    }

    public static int StateUpdateRep(int index) {
        return (index < 7 ? 8 : 11);
    }

    public static int StateUpdateShortRep(int index) {
        return (index < 7 ? 9 : 11);
    }

    public static boolean StateIsCharState(int index) {
        return index < 7;
    }

    public static int GetLenToPosState(int len) {
        len -= kMatchMinLen;
        if (len < kNumLenToPosStates)
            return len;
        return kNumLenToPosStates - 1;
    }
}
