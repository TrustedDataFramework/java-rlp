package org.tdf.rlp;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    static Comparator getContentOrdering(AnnotatedElement element) {
        if (!element.isAnnotationPresent(RLPEncoding.class)) {
            return null;
        }
        Class<? extends Comparator> clazz = element.getAnnotation(RLPEncoding.class).contentOrdering();
        if (clazz == RLPEncoding.None.class) return null;
        return newInstance(clazz);
    }

    static Comparator getKeyOrdering(AnnotatedElement element) {
        if (!element.isAnnotationPresent(RLPEncoding.class)) {
            return null;
        }
        Class<? extends Comparator> clazz = element.getAnnotation(RLPEncoding.class).keyOrdering();
        if (clazz == RLPEncoding.None.class) return null;
        return newInstance(clazz);
    }

    static Class getGenericTypeRecursively(Class clazz, final int index) {
        Optional<ParameterizedType> o = Arrays.stream(clazz.getGenericInterfaces())
                .filter(x -> x instanceof ParameterizedType)
                .map(x -> (ParameterizedType) x)
                .filter(x -> index < x.getActualTypeArguments().length
                        && x.getActualTypeArguments()[index] instanceof Class)
                .findFirst();

        if (o.isPresent()) return index < o.get().getActualTypeArguments().length ?
                (Class) o.get().getActualTypeArguments()[index] : null;
        Type type = clazz.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) type).getActualTypeArguments();
            if(index < types.length && types[index] instanceof Class) return (Class) types[index];
        }
        if (clazz.getSuperclass() == null) return null;
        return getGenericTypeRecursively(clazz.getSuperclass(), index);
    }

    static <T> T newInstance(Class<T> clazz){
        try{
            Constructor<T> con = clazz.getDeclaredConstructor();
            con.setAccessible(true);
            return con.newInstance();
        }catch (Exception e){
            throw new RuntimeException(clazz + " should has an no-argument constructor");
        }
    }
}
