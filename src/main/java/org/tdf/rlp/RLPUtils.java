package org.tdf.rlp;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

final class RLPUtils {
    static RLPEncoder getAnnotatedRLPEncoder(AnnotatedElement element) {
        if (!element.isAnnotationPresent(RLPEncoding.class)) {
            return null;
        }
        Class<? extends RLPEncoder> encoder = element.getAnnotation(RLPEncoding.class).value();
        if (encoder == RLPEncoder.None.class) {
            return null;
        }
        return newInstance(encoder);
    }

    static RLPDecoder getAnnotatedRLPDecoder(AnnotatedElement element) {
        if (!element.isAnnotationPresent(RLPDecoding.class)) {
            return null;
        }
        Class<? extends RLPDecoder> decoder = element.getAnnotation(RLPDecoding.class).value();
        if (decoder == RLPDecoder.None.class) {
            return null;
        }
        return newInstance(decoder);
    }

    static List<Field> getRLPFields(Class clazz) {
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(RLP.class))
                .sorted(Comparator.comparingInt(x -> x.getAnnotation(RLP.class).value())).collect(Collectors.toList());
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getAnnotation(RLP.class).value() != i)
                throw new RuntimeException(String.format("field %s of class %s should have RLP(%d)", fields.get(i), clazz, i));
        }
        return fields;
    }

    static Comparator getKeyOrdering(Field field) {
        if (!field.isAnnotationPresent(RLPEncoding.class)) {
            return null;
        }
        Class<? extends Comparator> clazz = field.getAnnotation(RLPEncoding.class).keyOrdering();
        if (clazz == RLPEncoding.None.class) return null;
        if(!Map.class.isAssignableFrom(field.getType()) && !Set.class.isAssignableFrom(field.getType()))
            throw new RuntimeException("@RLPEncoding.keyOrdering() is used on Map or Set other than "
                    + field.getName() + " "
                    + field.getType().getName());
        return newInstance(clazz);
    }

    static boolean isContainer(Class clazz){
        return Map.class.isAssignableFrom(clazz) || Collection.class.isAssignableFrom(clazz);
    }

    static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> con = clazz.getDeclaredConstructor();
            con.setAccessible(true);
            return con.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(clazz + " should has an no-argument constructor");
        }
    }
}
