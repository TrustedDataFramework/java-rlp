package org.tdf.rlp;

import lombok.NonNull;

import java.time.LocalDate;
import java.util.Arrays;

public class LocalDateEncoder implements RLPEncoder<LocalDate> {
    @Override
    public RLPElement encode(@NonNull LocalDate localDate) {
        return RLPElement.readRLPTree(
                Arrays.asList(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth())
        );
    }
}