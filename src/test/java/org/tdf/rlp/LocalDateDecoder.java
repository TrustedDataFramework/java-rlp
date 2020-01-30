package org.tdf.rlp;

import lombok.NonNull;

import java.time.LocalDate;

public class LocalDateDecoder implements RLPDecoder<LocalDate> {
    @Override
    public LocalDate decode(@NonNull RLPElement rlpElement) {
        if(rlpElement.isNull()) return null;
        int[] data = rlpElement.as(int[].class);
        return LocalDate.of(data[0], data[1], data[2]);
    }
}
