package org.tdf.rlp;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

final class RLPUtils {
    static Map<Class, List<Field>> FIELDS = new HashMap<>();
    static Map<Class, List<Container>> CONTAINERS = new HashMap<>();

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
        List<Field> fields = FIELDS.get(clazz);
        if(fields != null) return fields;

        final Field[] declaredFields = clazz.getDeclaredFields();
        List<Field> notIgnored = Arrays.stream(declaredFields)
                .filter(x -> {
                    if(x.isAnnotationPresent(RLPIgnored.class)) {
                        if(x.isAnnotationPresent(RLP.class))
                            throw new RuntimeException(x.getName() + " is both annotated with @RLP and @RLPIgnored");
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        List<Field> annotated = notIgnored.stream()
                .filter(x -> x.isAnnotationPresent(RLP.class))
                .sorted(Comparator.comparingInt(x -> x.getAnnotation(RLP.class).value()))
                .collect(Collectors.toList());
        if(annotated.size() == 0) {
            Map<Class, List<Field>> tmp = new HashMap<>(FIELDS);
            tmp.put(clazz, notIgnored);
            FIELDS = tmp;
            return notIgnored;
        }
        for (int i = 0; i < annotated.size(); i++) {
            if (annotated.get(i).getAnnotation(RLP.class).value() != i)
                throw new RuntimeException(String.format("field %s of class %s should have RLP(%d)", annotated.get(i), clazz, i));
        }
        Map<Class, List<Field>> tmp = new HashMap<>(FIELDS);
        tmp.put(clazz, annotated);
        FIELDS = tmp;
        return annotated;
    }

    static List<Container> getRLPContainers(Class clazz){
        List<Container> containers = CONTAINERS.get(clazz);
        if(containers != null) return containers;
        Map<Class, List<Container>> copied = new HashMap<>(CONTAINERS);
        List<Container> ret = getRLPFields(clazz).stream()
                .map(Container::fromField)
                .collect(Collectors.toList());
        copied.put(clazz, ret);
        return ret;
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
