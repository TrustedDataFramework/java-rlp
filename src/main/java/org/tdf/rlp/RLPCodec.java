package org.tdf.rlp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class RLPCodec {

    public static <T> T decode(byte[] data, Class<T> clazz) {
        RLPElement element = RLPElement.fromEncoded(data);
        return decode(element, clazz);
    }

    static <T> List<T> decodeList(RLPElement element, Class<T> elementType){
        return decodeList(element.asRLPList(), 1, elementType);
    }

    static <T> List<T> decodeList(byte[] data, Class<T> elementType) {
        RLPElement element = RLPElement.fromEncoded(data);
        return decodeList(element.asRLPList(), 1, elementType);
    }

    private static List decodeList(RLPList list, int level, Class<?> elementType) {
        if (level == 0) throw new RuntimeException("level should be positive");
        if (level > 1) {
            List res = new ArrayList(list.size());
            for (int i = 0; i < list.size(); i++) {
                res.add(decodeList(list.get(i).asRLPList(), level - 1, elementType));
            }
            return res;
        }
        if (elementType == RLPElement.class) return list;
        if (elementType == RLPItem.class) {
            return list.stream().map(x -> x.asRLPItem()).collect(Collectors.toList());
        }
        List res = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            res.add(decode(list.get(i), elementType));
        }
        return res;
    }

    public static <T> T decode(RLPElement element, Class<T> clazz) {
        if (clazz == RLPElement.class) return (T) element;
        if (clazz == RLPList.class) return (T) element.asRLPList();
        if (clazz == RLPItem.class) return (T) element.asRLPItem();
        if(clazz == boolean.class || clazz == Boolean.class) return (T) Boolean.valueOf(element.asBoolean());
        RLPDecoder decoder = RLPUtils.getAnnotatedRLPDecoder(clazz);
        if (decoder != null) return (T) decoder.decode(element);
        // non null terminals
        if (clazz == Byte.class || clazz == byte.class) {
            return (T) Byte.valueOf(element.asByte());
        }
        if (clazz == Short.class || clazz == short.class) {
            return (T) Short.valueOf(element.asShort());
        }
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(element.asInt());
        }
        if (clazz == Long.class || clazz == long.class) {
            return (T) Long.valueOf(element.asLong());
        }
        if (clazz == byte[].class) {
            return (T) element.asBytes();
        }
        // String is non-null, since we cannot differ between null empty string and null
        if (clazz == String.class) {
            return (T) element.asString();
        }
        // big integer is non-null, since we cannot differ between zero and null
        if (clazz == BigInteger.class) {
            return (T) element.asBigInteger();
        }
        if (element.isNull()) return null;
        if (clazz.isArray()) {
            Class elementType = clazz.getComponentType();
            Object res = Array.newInstance(clazz.getComponentType(), element.asRLPList().size());
            for (int i = 0; i < element.asRLPList().size(); i++) {
                Array.set(res, i, decode(element.asRLPList().get(i), elementType));
            }
            return (T) res;
        }
        // cannot determine generic type at runtime
        if (clazz == List.class) {
            return (T) element.asRLPList();
        }
        Object o;
        try{
            clazz.getConstructor();
        }catch (Exception e){
            throw new RuntimeException(clazz + " should has a no arguments constructor");
        }
        try {
            o = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<Field> fields = RLPUtils.getRLPFields(clazz);
        if (fields.size() == 0) throw new RuntimeException(clazz + " is not supported not RLP annotation found");
        for (int i = 0; i < fields.size(); i++) {
            RLPElement el = element.asRLPList().get(i);
            Field f = fields.get(i);
            f.setAccessible(true);
            RLPDecoder fieldDecoder = RLPUtils.getAnnotatedRLPDecoder(f);
            if (fieldDecoder != null) {
                try {
                    f.set(o, fieldDecoder.decode(el));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            if (!f.getType().equals(List.class)) {
                try {
                    f.set(o, decode(el, f.getType()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            try {
                if (el.isNull()) {
                    continue;
                }
                RLPUtils.Resolved resolved = RLPUtils.resolveFieldType(f);
                f.set(o, decodeList(el.asRLPList(), resolved.level, resolved.type));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return (T) o;
    }

    static byte[] encodeBoolean(boolean b) {
        return RLPItem.fromBoolean(b).getEncoded();
    }

    static boolean decodeBoolean(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asBoolean();
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
        return RLPElement.fromEncoded(encoded).asInt();
    }

    static short decodeShort(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asShort();
    }

    static long decodeLong(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asLong();
    }

    static String decodeString(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asString();
    }

    static byte[] encode(Object o){
        return RLPElement.readRLPTree(o).getEncoded();
    }
}
