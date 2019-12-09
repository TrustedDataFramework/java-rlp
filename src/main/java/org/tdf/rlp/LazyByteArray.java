package org.tdf.rlp;

import java.util.Arrays;

// reduce byte array copy by lazy loading
public class LazyByteArray {
    private byte[] data;
    private int offset;
    private int limit;

    static LazyByteArray EMPTY = new LazyByteArray(new byte[0]);

    public LazyByteArray(byte[] data){
        this.data = data;
        this.limit = data.length;
    }

    public LazyByteArray(byte[] data, int offset, int limit) {
        this.data = data;
        this.offset = offset;
        this.limit = limit;
    }

    public byte[] get(){
        if(offset == 0 && limit == data.length) return data;
        data = Arrays.copyOfRange(data, offset, limit);
        offset = 0;
        limit = data.length;
        return data;
    }

    public int size(){
        return limit - offset;
    }
}
