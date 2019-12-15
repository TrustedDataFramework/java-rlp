package org.tdf.rlp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.tdf.rlp.RLPItem.NULL;
import static org.tdf.rlp.RLPItem.ONE;

/**
 * The RLP encoding function takes in an item. An item is defined as follows
 * <p>
 * A string (ie. byte array) is an item
 * A list of items is an item
 * RLP could encode tree-like object
 * RLP cannot determine difference between null reference and emtpy byte array
 * RLP cannot determine whether a box type is null or zero, e.g. Byte, Short, Integer, Long, BigInteger
 */
public interface RLPElement {
    boolean isRLPList();

    boolean isRLPItem();

    RLPList asRLPList();

    RLPItem asRLPItem();

    boolean isNull();

    byte[] getEncoded();

    byte[] asBytes();

    byte asByte();

    short asShort();

    int asInt();

    long asLong();

    int size();

    RLPElement get(int index);

    boolean add(RLPElement element);

    RLPElement set(int index, RLPElement element);

    BigInteger asBigInteger();

    String asString();

    boolean asBoolean();

    default <T> T as(Class<T> clazz) {
        return RLPCodec.decode(this, clazz);
    }

    static RLPElement fromEncoded(byte[] data) {
        return fromEncoded(data, true);
    }

    static RLPElement fromEncoded(byte[] data, boolean lazy) {
        return RLPParser.fromEncoded(data, lazy);
    }

    // convert any object as a rlp tree
    static RLPElement readRLPTree(Object t) {
        if (t == null) return NULL;
        if (t instanceof Boolean || t.getClass() == boolean.class) {
            return ((Boolean) t) ? ONE : NULL;
        }
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
        if (t instanceof Map) {
            return RLPCodec.encodeMap((Map) t, null);
        }
        if (t.getClass().isArray()) {
            RLPList list = RLPList.createEmpty(Array.getLength(t));
            for (int i = 0; i < Array.getLength(t); i++) {
                list.add(readRLPTree(Array.get(t, i)));
            }
            return list;
        }
        if (t instanceof Collection) {
            return RLPCodec.encodeCollection((Collection) t, null);
        }
        // peek fields reflection
        List<Field> fields = RLPUtils.getRLPFields(t.getClass());
        if (fields.size() == 0)
            throw new RuntimeException(t.getClass() + " is not supported, no @RLP annotation found");
        return new RLPList(fields.stream().map(f -> {
            f.setAccessible(true);
            try {
                if (f.get(t) == null) return NULL;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            RLPEncoder fieldEncoder = RLPUtils.getAnnotatedRLPEncoder(f);
            if (fieldEncoder != null) {
                try {
                    return fieldEncoder.encode(f.get(t));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Comparator comparator = RLPUtils.getContentOrdering(f);
            if (Collection.class.isAssignableFrom(f.getType())) {
                try {
                    return RLPCodec.encodeCollection((Collection) f.get(t), comparator);
                } catch (Exception e) {
                    throw new RuntimeException("get field " + f + " failed " + e.getCause());
                }
            }
            comparator = RLPUtils.getKeyOrdering(f);
            if (Map.class.isAssignableFrom(f.getType())) {
                try {
                    Map m = (Map) f.get(t);
                    return RLPCodec.encodeMap(m, comparator);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                return readRLPTree(f.get(t));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));
    }


}
