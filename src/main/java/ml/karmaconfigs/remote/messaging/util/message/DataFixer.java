package ml.karmaconfigs.remote.messaging.util.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class DataFixer {

    public static byte[] fixByteData(final byte[] data) {
        List<Byte> byteList = new ArrayList<>();

        for (byte b : data) {
            if (b > 0) {
                byteList.add(b);
            }
        }

        byte[] array = new byte[byteList.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = byteList.get(i);
        }

        return array;
    }

    public static ByteBuffer fixBuffer(final ByteBuffer original) {
        byte[] tmp = original.array();
        return ByteBuffer.wrap(DataFixer.fixByteData(tmp));
    }
}
