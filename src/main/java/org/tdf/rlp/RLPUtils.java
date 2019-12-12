package org.tdf.rlp;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
        try {
            return encoder.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static RLPDecoder getAnnotatedRLPDecoder(AnnotatedElement element) {
        if (!element.isAnnotationPresent(RLPDecoding.class)) {
            return null;
        }
        Class<? extends RLPDecoder> decoder = element.getAnnotation(RLPDecoding.class).value();
        if (decoder == RLPDecoder.None.class) {
            return null;
        }
        try {
            return decoder.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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


    static Resolved resolveFieldType(Field f) {
        if (f.getType() != List.class) {
            return new Resolved(0, f.getType());
        }
        Type generic = f.getGenericType();
        if (!(generic instanceof ParameterizedType)) {
            return new Resolved(1, RLPElement.class);
        }
        return resolve((ParameterizedType) generic, new Resolved(1, null));
    }

    private static Resolved resolve(ParameterizedType type, Resolved resolved) {
        Type[] types = type.getActualTypeArguments();
        Type t = types[0];
        if (t instanceof Class) {
            resolved.type = (Class) t;
            return resolved;
        }
        // type is nested;
        ParameterizedType nested = (ParameterizedType) t;
        resolved.level += 1;
        return resolve(nested, resolved);
    }

    static class Resolved {
        int level;
        Class type;

        public Resolved() {
        }

        public Resolved(int level, Class type) {
            this.level = level;
            this.type = type;
        }
    }
}
