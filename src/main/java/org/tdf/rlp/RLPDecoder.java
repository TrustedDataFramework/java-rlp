package org.tdf.rlp;

import lombok.NonNull;

public interface RLPDecoder<T> {
    /**
     *
     * @param element the element may equals to RLPItem.NULL
     * @return
     */
    T decode(@NonNull RLPElement element);

    class None implements RLPDecoder<Object> {
        @Override
        public Object decode(RLPElement element) {
            return null;
        }
    }
}
