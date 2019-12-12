package org.tdf.rlp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
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

    BigInteger asBigInteger();

    String asString();

    boolean asBoolean();

    static RLPElement fromEncoded(byte[] data) {
        return RLPParser.fromEncoded(data);
    }

    // encode any object as a rlp element
    static RLPElement encodeAsRLPElement(Object t) {
        if (t == null) return NULL;
        if(t instanceof Boolean || t.getClass() == boolean.class){
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
        if (t.getClass().isArray()) {
            RLPList list = RLPList.createEmpty(Array.getLength(t));
            for (int i = 0; i < Array.getLength(t); i++) {
                list.add(encodeAsRLPElement(Array.get(t, i)));
            }
            return list;
        }
        if (t instanceof Collection) {
            RLPList list = RLPList.createEmpty(((Collection) t).size());
            for (Object o : ((Collection) t)) {
                list.add(encodeAsRLPElement(o));
            }
            return list;
        }
        // peek fields reflection
        List<Field> fields = RLPUtils.getRLPFields(t.getClass());
        if (fields.size() == 0) throw new RuntimeException(t.getClass() + " is not supported, no @RLP annotation found");
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
                return encodeAsRLPElement(f.get(t));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));
    }

    static byte[] encode(Object o){
        return encodeAsRLPElement(o).getEncoded();
    }

    static byte[] encodeBoolean(boolean b) {
        return RLPItem.fromBoolean(b).getEncoded();
    }

    static boolean decodeBoolean(byte[] encoded) {
        return fromEncoded(encoded).asBoolean();
    }

    static byte[] encodeByte(byte b) {
        return RLPItem.fromByte(b).getEncoded();
    }

    static byte[] encodeShort(short s) {
        return RLPItem.fromShort(s).getEncoded();
    }

    static byte[] encodeInt(int n) {
        return RLPItem.fromInt(n).getEncoded();
    }

    static byte[] encodeBigInteger(BigInteger bigInteger) {
        return RLPItem.fromBigInteger(bigInteger).getEncoded();
    }

    static byte[] encodeString(String s) {
        return RLPItem.fromString(s).getEncoded();
    }

    static int decodeInt(byte[] encoded) {
        return fromEncoded(encoded).asInt();
    }

    static short decodeShort(byte[] encoded) {
        return fromEncoded(encoded).asShort();
    }

    static long decodeLong(byte[] encoded) {
        return fromEncoded(encoded).asLong();
    }

    static String decodeString(byte[] encoded) {
        return fromEncoded(encoded).asString();
    }
}
