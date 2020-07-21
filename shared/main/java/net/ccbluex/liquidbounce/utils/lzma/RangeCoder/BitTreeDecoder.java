package net.ccbluex.liquidbounce.utils.lzma.RangeCoder;

public class BitTreeDecoder {
    short[] Models;
    int NumBitLevels;

    public BitTreeDecoder(int numBitLevels) {
        NumBitLevels = numBitLevels;
        Models = new short[1 << numBitLevels];
    }

    public static int ReverseDecode(short[] Models, int startIndex,
                                    RangeDecoder rangeDecoder, int NumBitLevels) throws java.io.IOException {
        int m = 1;
        int symbol = 0;
        for (int bitIndex = 0; bitIndex < NumBitLevels; bitIndex++) {
            int bit = rangeDecoder.DecodeBit(Models, startIndex + m);
            m <<= 1;
            m += bit;
            symbol |= (bit << bitIndex);
        }
        return symbol;
    }

    public void Init() {
        RangeDecoder.InitBitModels(Models);
    }

    public int Decode(RangeDecoder rangeDecoder) throws java.io.IOException {
        int m = 1;
        for (int bitIndex = NumBitLevels; bitIndex != 0; bitIndex--)
            m = (m << 1) + rangeDecoder.DecodeBit(Models, m);
        return m - (1 << NumBitLevels);
    }

    public int ReverseDecode(RangeDecoder rangeDecoder) throws java.io.IOException {
        int m = 1;
        int symbol = 0;
        for (int bitIndex = 0; bitIndex < NumBitLevels; bitIndex++) {
            int bit = rangeDecoder.DecodeBit(Models, m);
            m <<= 1;
            m += bit;
            symbol |= (bit << bitIndex);
        }
        return symbol;
    }
}
