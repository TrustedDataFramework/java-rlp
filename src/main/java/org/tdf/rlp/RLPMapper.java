package org.tdf.rlp;

public class RLPMapper {
    private RLPContext context = RLPContext.EMPTY;

    public RLPElement readRLPTree(Object o) {
        return RLPElement.readRLPTree(o, context);
    }

    public byte[] encode(Object o) {
        return RLPCodec.encode(o, context);
    }

    public <T> T decode(RLPElement el, Class<T> clazz) {
        return RLPCodec.decode(el, clazz, context);
    }

    public <T> T decode(byte[] data, Class<T> clazz) {
        return RLPCodec.decode(data, clazz, context);
    }

    public RLPMapper withContext(RLPContext context) {
        RLPMapper ret = new RLPMapper();
        ret.context = context;
        return ret;
    }
}
