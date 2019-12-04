package org.tdf.rlp;

public interface RLPDecoder<T> {
    T decode(RLPElement element);

    class None implements RLPDecoder{
        @Override
        public Object decode(RLPElement element) {
            return null;
        }
    }
}
