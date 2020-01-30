package org.tdf.rlp;

import lombok.NonNull;

public interface RLPEncoder<T> {
    /**
     *
     * @param o non-null object
     * @return encoded result
     */
    RLPElement encode(@NonNull T o);

    class None implements RLPEncoder<Object> {
        @Override
        public RLPElement encode(Object o) {
            return null;
        }
    }
}
