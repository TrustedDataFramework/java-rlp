package org.tdf.rlp;

import java.util.Collections;

public interface RLPContext {
    RLPContext EMPTY = new RLPContextImpl(Collections.emptyMap(), Collections.emptyMap());

    static RLPContext newInstance() {
        return new RLPContextImpl();
    }

    <T> RLPContext withEncoder(Class<T> clazz, RLPEncoder<? super T> encoder);

    <T> RLPContext withDecoder(Class<T> clazz, RLPDecoder<? extends T> decoder);

    <T> RLPEncoder<T> getEncoder(Class<T> clazz);

    <T> RLPDecoder<T> getDecoder(Class<T> clazz);
}
