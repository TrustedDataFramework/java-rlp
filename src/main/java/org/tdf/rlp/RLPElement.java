package org.tdf.rlp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The RLP encoding function takes in an item. An item is defined as follows
 * <p>
 * A string (ie. byte array) is an item
 * A list of items is an item
 * RLP could encode tree-like object
 * RLP cannot determine difference between null reference and emtpy byte array
 * RLP cannot determine whether a box type is null or zero, e.g. Byte, Short, Integer, Long
 */
public interface RLPElement {
    boolean isList();

    RLPList getAsList();

    RLPItem getAsItem();

    boolean isNull();

    byte[] getEncoded();

    static RLPElement fromEncoded(byte[] data) {
        return RLPReader.fromEncoded(data);
    }

    // encode any object as a rlp element
    static RLPElement encode(Object t) {
        if (t == null) return RLPItem.NULL;
        if (t instanceof RLPElement) return (RLPElement) t;
        RLPEncoder encoder = RLPUtils.getAnnotatedRLPEncoder(t.getClass());
        if (encoder != null) {
            return encoder.encode(t);
        }
        if (t instanceof BigInteger) return RLPItem.fromBigInteger((BigInteger) t);
        if (t instanceof byte[]) return RLPItem.fromBytes((byte[]) t);
        if (t instanceof String) return RLPItem.fromString((String) t);
        // terminals
        if (t.getClass().equals(byte.class) || t instanceof Byte) {
            return RLPItem.fromByte((byte) t);
        }
        if (t instanceof Short || t.getClass().equals(short.class)) {
            return RLPItem.fromShort((short) t);
        }
        if (t instanceof Integer || t.getClass().equals(int.class)) {
            return RLPItem.fromInt((int) t);
        }
        if (t instanceof Long || t.getClass().equals(long.class)) {
            return RLPItem.fromLong((long) t);
        }
        if (t.getClass().isArray()) {
            RLPList list = RLPList.createEmpty(Array.getLength(t));
            for (int i = 0; i < Array.getLength(t); i++) {
                list.add(encode(Array.get(t, i)));
            }
            return list;
        }
        if (t instanceof Collection) {
            RLPList list = RLPList.createEmpty(((Collection) t).size());
            for (Object o : ((Collection) t)) {
                list.add(encode(o));
            }
            return list;
        }
        // peek fields reflection
        List<Field> fields = RLPUtils.getRLPFields(t.getClass());
        if (fields.size() == 0) throw new RuntimeException(t.getClass() + " is not supported not RLP annotation found");
        return new RLPList(fields.stream().map(f -> {
            f.setAccessible(true);
            RLPEncoder fieldEncoder = RLPUtils.getAnnotatedRLPEncoder(f);
            if (fieldEncoder != null) {
                try {
                    return fieldEncoder.encode(f.get(t));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                return encode(f.get(t));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));
    }
}
