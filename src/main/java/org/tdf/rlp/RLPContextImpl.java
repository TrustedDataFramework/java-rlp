package org.tdf.rlp;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
class RLPContextImpl implements RLPContext {
    private Map<Class, RLPEncoder> encoders = new HashMap<>();
    private Map<Class, RLPDecoder> decoders = new HashMap<>();

    @Override
    public <T> RLPContext withEncoder(Class<T> clazz, RLPEncoder<? super T> encoder) {
        RLPContextImpl ret = new RLPContextImpl(new HashMap<>(encoders), decoders);
        ret.encoders.put(clazz, encoder);
        return ret;
    }

    @Override
    public <T> RLPContext withDecoder(Class<T> clazz, RLPDecoder<? extends T> decoder) {
        RLPContextImpl ret = new RLPContextImpl(encoders, new HashMap<>(decoders));
        ret.decoders.put(clazz, decoder);
        return ret;
    }

    @Override
    public <T> RLPEncoder<T> getEncoder(Class<T> clazz) {
        return encoders.get(clazz);
    }

    @Override
    public <T> RLPDecoder<T> getDecoder(Class<T> clazz) {
        return decoders.get(clazz);
    }
}
