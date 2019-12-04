package org.tdf.rlp;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.tdf.rlp.RLPConstants.*;

/**
 * immutable rlp item
 */
public final class RLPItem implements RLPElement {
    // cannot be null
    private final byte[] data;

    private Long longNumber;

    private byte[] encoded;

    private int offset;

    private int limit;

    public byte[] get() {
        return Arrays.copyOfRange(data, offset, limit);
    }

    public static RLPItem fromByte(byte b) {
        return fromLong(Byte.toUnsignedLong(b));
    }

    public static RLPItem fromShort(short s) {
        return fromLong(Short.toUnsignedLong(s));
    }

    public static RLPItem fromInt(int i) {
        return fromLong(Integer.toUnsignedLong(i));
    }

    public static RLPItem fromLong(long l) {
        int leadingZeroBytes = Long.numberOfLeadingZeros(l) / Byte.SIZE;

        return fromBytes(concat(
                // trim zero bytes
                Arrays.copyOfRange(ByteBuffer.allocate(Long.BYTES).putLong(l).array(), leadingZeroBytes, Long.BYTES)
        ));
    }

    public static RLPItem fromString(String s) {
        return fromBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    public static RLPItem fromBytes(byte[] data) {
        if (data == null || data.length == 0) return NULL;
        return new RLPItem(data, 0, data.length);
    }

    public static RLPItem fromBigInteger(BigInteger bigInteger) {
        if (bigInteger.compareTo(BigInteger.ZERO) < 0) throw new RuntimeException("negative numbers are not allowed");
        if (bigInteger.equals(BigInteger.ZERO)) return NULL;
        return fromBytes(asUnsignedByteArray(bigInteger));
    }

    private static byte[] asUnsignedByteArray(
            BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];

            System.arraycopy(bytes, 1, tmp, 0, tmp.length);

            return tmp;
        }
        return bytes;
    }

    public static final RLPItem NULL = new RLPItem(new byte[0], 0, 0);

    RLPItem(byte[] data, int offset, int limit) {
        this.data = data;
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public RLPList getAsList() {
        throw new RuntimeException("not a rlp list");
    }

    @Override
    public RLPItem getAsItem() {
        return this;
    }

    public boolean isNull() {
        return data.length == 0 || limit == offset;
    }

    public byte getByte() {
        if (Long.compareUnsigned(getLong(), 0xffL) > 0) throw new RuntimeException("invalid byte, overflow");
        return (byte) getLong();
    }

    public short getShort() {
        if (Long.compareUnsigned(getLong(), 0xffff) > 0) throw new RuntimeException("invalid short, overflow");
        return (short) getLong();
    }

    public int getInt() {
        if (Long.compareUnsigned(getLong(), 0xffffffff) > 0) throw new RuntimeException("invalid int, overflow");
        return (int) getLong();
    }

    public long getLong() {
        // numbers are ont starts with zero byte
        byte[] data = get();
        if (data.length > 0 && data[0] == 0) throw new RuntimeException("not a number");
        if (longNumber != null) return longNumber;
        if (isNull()) {
            longNumber = 0L;
            return longNumber;
        }
        ;
        if (data.length > Long.BYTES) throw new RuntimeException("not a number");
        longNumber = ByteBuffer.wrap(concat(new byte[Long.BYTES - data.length], data)).getLong();
        return longNumber;
    }

    public BigInteger getBigInteger() {
        byte[] data = get();
        if (data[0] == 0) throw new RuntimeException("not a number");
        if (isNull()) return BigInteger.ZERO;
        return new BigInteger(1, data);
    }

    public String getString() {
        return new String(get(), StandardCharsets.UTF_8);
    }

    public byte[] getEncoded() {
        if (encoded == null) encoded = encodeElement(get());
        return encoded;
    }

    public static byte[] encodeElement(byte[] srcData) {
        // [0x80]
        if (srcData == null || srcData.length == 0) {
            return new byte[]{(byte) OFFSET_SHORT_ITEM};
            // [0x00]
        }
        if (srcData.length == 1 && (srcData[0] & 0xFF) < OFFSET_SHORT_ITEM) {
            return srcData;
            // [0x80, 0xb7], 0 - 55 bytes
        }
        if (srcData.length < SIZE_THRESHOLD) {
            // length = 8X
            byte length = (byte) (OFFSET_SHORT_ITEM + srcData.length);
            byte[] data = Arrays.copyOf(srcData, srcData.length + 1);
            System.arraycopy(data, 0, data, 1, srcData.length);
            data[0] = length;

            return data;
            // [0xb8, 0xbf], 56+ bytes
        }
        // length of length = BX
        // prefix = [BX, [length]]
        int tmpLength = srcData.length;
        byte lengthOfLength = 0;
        while (tmpLength != 0) {
            ++lengthOfLength;
            tmpLength = tmpLength >> 8;
        }

        // set length Of length at first byte
        byte[] data = new byte[1 + lengthOfLength + srcData.length];
        data[0] = (byte) (OFFSET_LONG_ITEM + lengthOfLength);

        // copy length after first byte
        tmpLength = srcData.length;
        for (int i = lengthOfLength; i > 0; --i) {
            data[i] = (byte) (tmpLength & 0xFF);
            tmpLength = tmpLength >> 8;
        }

        // at last copy the number bytes after its length
        System.arraycopy(srcData, 0, data, 1 + lengthOfLength, srcData.length);

        return data;
    }

    public static byte[] encodeByte(byte b) {
        return fromByte(b).getEncoded();
    }

    public static byte[] encodeShort(short s) {
        return fromShort(s).getEncoded();
    }

    public static byte[] encodeInt(int n) {
        return fromInt(n).getEncoded();
    }

    public static byte[] encodeBigInteger(BigInteger bigInteger) {
        return fromBigInteger(bigInteger).getEncoded();
    }

    public static byte[] encodeString(String s) {
        return fromString(s).getEncoded();
    }

    public static int decodeInt(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).getAsItem().getInt();
    }

    public static short decodeShort(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).getAsItem().getShort();
    }

    public static long decodeLong(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).getAsItem().getLong();
    }

    public static String decodeString(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).getAsItem().getString();
    }

    /**
     * Returns the values from each provided array combined into a single array. For example, {@code
     * concat(new byte[] {a, b}, new byte[] {}, new byte[] {c}} returns the array {@code {a, b, c}}.
     *
     * @param arrays zero or more {@code byte} arrays
     * @return a single array containing all the values from the source arrays, in order
     */
    private static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }
}
