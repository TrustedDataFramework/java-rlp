package org.tdf.rlp;


import lombok.NonNull;

import java.math.BigInteger;
import java.util.Arrays;

import static org.tdf.rlp.RLPConstants.*;

class RLPReader {
    private static int byteArrayToInt(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).intValue();
    }

    private byte[] raw;

    private int offset;

    private int limit;

    static RLPElement fromEncoded(@NonNull byte[] data) {
        RLPReader reader = new RLPReader(data);
        if (reader.estimateSize() != data.length) {
            throw new RuntimeException("invalid encoding");
        }
        return reader.readElement();
    }

    private RLPReader(byte[] data) {
        this.raw = data;
        this.limit = data.length;
    }

    private RLPReader(byte[] data, int offset, int limit) {
        this.raw = data;
        this.offset = offset;
        this.limit = limit;
    }

    private RLPReader readAsReader(int length) {
        if (offset + length > limit) throw new RuntimeException("read overflow");
        RLPReader reader = new RLPReader(raw, offset, offset + length);
        offset += length;
        return reader;
    }

    private int estimateSize() {
        int prefix = peek();
        if (prefix < OFFSET_SHORT_ITEM) {
            return 1;
        }
        if (prefix < OFFSET_LONG_ITEM) {
            return prefix - OFFSET_SHORT_ITEM + 1;
        }
        if (prefix < OFFSET_SHORT_LIST) {
            // skip
            return byteArrayToInt(Arrays.copyOfRange(raw, 1, 1 + prefix - OFFSET_SHORT_LIST)) + 1 + prefix - OFFSET_LONG_ITEM;
        }
        if (prefix <= OFFSET_LONG_LIST) {
            return prefix - OFFSET_SHORT_LIST + 1;
        }
        return byteArrayToInt(Arrays.copyOfRange(raw, 1, 1 + prefix - OFFSET_LONG_LIST)) + 1 + prefix - OFFSET_LONG_LIST;
    }

    private int read() {
        if (offset >= limit) throw new RuntimeException("read overflow");
        return Byte.toUnsignedInt(raw[offset++]);
    }

    private byte[] read(int n) {
        if (offset + n > limit) throw new RuntimeException("read overflow");
        byte[] res = Arrays.copyOfRange(raw, offset, offset + n);
        offset += n;
        return res;
    }

    private void skip(int n) {
        offset += n;
    }

    private int peek() {
        return Byte.toUnsignedInt(raw[offset]);
    }


    private boolean peekIsList() {
        return peek() >= OFFSET_SHORT_LIST;
    }

    private RLPList readList() {
        int prefix = read();
        RLPList list = RLPList.createEmpty();
        RLPReader reader;
        if (prefix <= OFFSET_LONG_LIST) {
            int len = prefix - OFFSET_SHORT_LIST; // length of length the encoded bytes
            // skip preifx
            if (len == 0) return list;
            reader = readAsReader(len);
        } else {
            int lenlen = prefix - OFFSET_LONG_LIST; // length of length the encoded list
            int lenlist = byteArrayToInt(read(lenlen)); // length of encoded bytes
            reader = readAsReader(lenlist);
        }
        while (reader.hasRemaining()) {
            list.add(reader.readElement());
        }
        return list;
    }


    private RLPElement readElement() {
        if (peekIsList()) return readList();
        return readItem();
    }

    private RLPItem readItem() {
        int prefix = read();
        if (prefix < OFFSET_SHORT_ITEM) {
            return RLPItem.fromBytes(new byte[]{(byte) prefix});
        }
        if (prefix < OFFSET_LONG_ITEM) {
            int length = prefix - OFFSET_SHORT_ITEM;
            if (length == 0) {
                return RLPItem.NULL;
            }
            RLPItem item = new RLPItem(raw, offset, offset + length);
            skip(length);
            return item;
        }
        int lengthBits = prefix - OFFSET_LONG_ITEM; // length of length the encoded bytes
        // skip
        int length = byteArrayToInt(read(lengthBits));
        RLPItem item = new RLPItem(raw, offset, offset + length);
        skip(length);
        return item;
    }

    private boolean hasRemaining() {
        return offset < limit;
    }
}
