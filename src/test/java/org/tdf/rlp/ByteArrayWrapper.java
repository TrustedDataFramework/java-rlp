package org.tdf.rlp;

import java.io.Serializable;
import java.util.Arrays;

/**
 * wrap byte array as immutable
 */
public class ByteArrayWrapper implements Comparable<ByteArrayWrapper>, Serializable {

    private static final long serialVersionUID = 7120319357455987329L;
    private final byte[] data;
    private int hashCode = 0;

    public ByteArrayWrapper(byte[] data) {
        if (data == null)
            throw new NullPointerException("Data must not be null");
        this.data = data;
        this.hashCode = Arrays.hashCode(data);
    }

    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper))
            return false;
        byte[] otherData = ((ByteArrayWrapper) other).getData();
        return FastByteComparisons.compareTo(
                data, 0, data.length,
                otherData, 0, otherData.length) == 0;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(ByteArrayWrapper o) {
        return FastByteComparisons.compareTo(
                data, 0, data.length,
                o.getData(), 0, o.getData().length);
    }

    public byte[] getData() {
        return data;
    }
}
