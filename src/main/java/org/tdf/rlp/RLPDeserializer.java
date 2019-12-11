package org.tdf.rlp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RLPDeserializer {

    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        RLPElement element = RLPElement.fromEncoded(data);
        return deserialize(element, clazz);
    }

    public static <T> List<T> deserializeList(byte[] data, Class<T> elementType) {
        RLPElement element = RLPElement.fromEncoded(data);
        return deserializeList(element.getAsList(), 1, elementType);
    }

    private static List deserializeList(RLPList list, int level, Class<?> elementType) {
        if (level == 0) throw new RuntimeException("level should be positive");
        if (level > 1) {
            List res = new ArrayList(list.size());
            for (int i = 0; i < list.size(); i++) {
                res.add(deserializeList(list.get(i).getAsList(), level - 1, elementType));
            }
            return res;
        }
        if (elementType == RLPElement.class) return list;
        if (elementType == RLPItem.class) {
            return list.stream().map(x -> x.getAsItem()).collect(Collectors.toList());
        }
        List res = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            res.add(deserialize(list.get(i), elementType));
        }
        return res;
    }

    public static <T> T deserialize(RLPElement element, Class<T> clazz) {
        if (clazz == RLPElement.class) return (T) element;
        if (clazz == RLPList.class) return (T) element.getAsList();
        if (clazz == RLPItem.class) return (T) element.getAsItem();
        RLPDecoder decoder = RLPUtils.getAnnotatedRLPDecoder(clazz);
        if (decoder != null) return (T) decoder.decode(element);
        // non null terminals
        if (clazz == Byte.class || clazz == byte.class) {
            return (T) Byte.valueOf(element.getAsItem().getByte());
        }
        if (clazz == Short.class || clazz == short.class) {
            return (T) Short.valueOf(element.getAsItem().getShort());
        }
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(element.getAsItem().getInt());
        }
        if (clazz == Long.class || clazz == long.class) {
            return (T) Long.valueOf(element.getAsItem().getLong());
        }
        if (clazz == byte[].class) {
            return (T) element.getAsItem().get();
        }
        // String is non-null, since we cannot differ between null empty string and null
        if (clazz == String.class) {
            return (T) element.getAsItem().getString();
        }
        // big integer is non-null, since we cannot differ between zero and null
        if (clazz == BigInteger.class) {
            return (T) element.getAsItem().getBigInteger();
        }
        if (element.isNull()) return null;
        if (clazz.isArray()) {
            Class elementType = clazz.getComponentType();
            Object res = Array.newInstance(clazz.getComponentType(), element.getAsList().size());
            for (int i = 0; i < element.getAsList().size(); i++) {
                Array.set(res, i, deserialize(element.getAsList().get(i), elementType));
            }
            return (T) res;
        }
        // cannot determine generic type at runtime
        if (clazz == List.class) {
            return (T) deserializeList(element.getAsList(), 1, RLPElement.class);
        }
        Object o;
        try {
            o = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<Field> fields = RLPUtils.getRLPFields(clazz);
        if (fields.size() == 0) throw new RuntimeException(clazz + " is not supported not RLP annotation found");
        for (int i = 0; i < fields.size(); i++) {
            RLPElement el = element.getAsList().get(i);
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
                    f.set(o, deserialize(el, f.getType()));
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
                f.set(o, deserializeList(el.getAsList(), resolved.level, resolved.type));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return (T) o;
    }
}
