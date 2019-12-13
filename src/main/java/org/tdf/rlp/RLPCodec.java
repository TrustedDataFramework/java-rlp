package org.tdf.rlp;

import lombok.NonNull;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.tdf.rlp.RLPConstants.*;

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
        T o;

        try {
            Constructor<T> con = clazz.getDeclaredConstructor();
            con.setAccessible(true);
            o = con.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(clazz + " should has a no arguments constructor");
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
        return o;
    }

    // rlp primitives encoding/decoding
    public static byte[] encodeBoolean(boolean b) {
        return RLPItem.fromBoolean(b).getEncoded();
    }

    public static boolean decodeBoolean(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asBoolean();
    }

    public static byte[] encodeByte(byte b) {
        return RLPItem.fromByte(b).getEncoded();
    }

    public static byte[] encodeShort(short s) {
        return RLPItem.fromShort(s).getEncoded();
    }

    public static byte[] encodeInt(int n) {
        return RLPItem.fromInt(n).getEncoded();
    }

    public static byte[] encodeBigInteger(BigInteger bigInteger) {
        return RLPItem.fromBigInteger(bigInteger).getEncoded();
    }

    public static byte[] encodeString(String s) {
        return RLPItem.fromString(s).getEncoded();
    }

    public static int decodeInt(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asInt();
    }

    public static short decodeShort(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asShort();
    }

    public static long decodeLong(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asLong();
    }

    public static String decodeString(byte[] encoded) {
        return RLPElement.fromEncoded(encoded).asString();
    }

    public static byte[] encode(Object o){
        return RLPElement.readRLPTree(o).getEncoded();
    }

    // rlp list encode
    public static byte[] encodeBytes(byte[] srcData) {
        // [0x80]
        if (srcData == null || srcData.length == 0) {
            return new byte[]{(byte) OFFSET_SHORT_ITEM};
            // [0x00]
        }
        if (srcData.length == 1 && (srcData[0] & 0xFF) < OFFSET_SHORT_ITEM) {
            return srcData;
            // [0x80, 0xb7], 0 - 55 bytes
        }
        if (srcData.length < SIZE_THRESHOLD) {
            // length = 8X
            byte length = (byte) (OFFSET_SHORT_ITEM + srcData.length);
            byte[] data = Arrays.copyOf(srcData, srcData.length + 1);
            System.arraycopy(data, 0, data, 1, srcData.length);
            data[0] = length;

            return data;
            // [0xb8, 0xbf], 56+ bytes
        }
        // length of length = BX
        // prefix = [BX, [length]]
        int tmpLength = srcData.length;
        byte lengthOfLength = 0;
        while (tmpLength != 0) {
            ++lengthOfLength;
            tmpLength = tmpLength >> 8;
        }

        // set length Of length at first byte
        byte[] data = new byte[1 + lengthOfLength + srcData.length];
        data[0] = (byte) (OFFSET_LONG_ITEM + lengthOfLength);

        // copy length after first byte
        tmpLength = srcData.length;
        for (int i = lengthOfLength; i > 0; --i) {
            data[i] = (byte) (tmpLength & 0xFF);
            tmpLength = tmpLength >> 8;
        }

        // at last copy the number bytes after its length
        System.arraycopy(srcData, 0, data, 1 + lengthOfLength, srcData.length);

        return data;
    }

    public static byte[] encodeElements(@NonNull Collection<byte[]> elements) {
        int totalLength = 0;
        for (byte[] element1 : elements) {
            totalLength += element1.length;
        }

        byte[] data;
        int copyPos;
        if (totalLength < SIZE_THRESHOLD) {

            data = new byte[1 + totalLength];
            data[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
            copyPos = 1;
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = totalLength;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            tmpLength = totalLength;
            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            data = new byte[1 + lenBytes.length + totalLength];
            data[0] = (byte) (OFFSET_LONG_LIST + byteNum);
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

            copyPos = lenBytes.length + 1;
        }
        for (byte[] element : elements) {
            System.arraycopy(element, 0, data, copyPos, element.length);
            copyPos += element.length;
        }
        return data;
    }

    static RLPElement encodeCollection(Collection col, Comparator ordering){
        return RLPElement.readRLPTree(col.stream().sorted(ordering).collect(Collectors.toList()));
    }
}
