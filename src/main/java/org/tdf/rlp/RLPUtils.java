package org.tdf.rlp;

import java.lang.reflect.*;
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

    static Comparator getContentOrdering(AnnotatedElement element) {
        if (!element.isAnnotationPresent(RLPEncoding.class)) {
            return null;
        }
        Class<? extends Comparator> clazz = element.getAnnotation(RLPEncoding.class).contentOrdering();
        if (clazz == RLPEncoding.None.class) return null;
        try {
            Constructor<? extends Comparator> con = clazz.getDeclaredConstructor();
            con.setAccessible(true);
            return con.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("new instance of " + clazz + " failed " + e.getMessage());
        }
    }

    static Comparator getKeyOrdering(AnnotatedElement element) {
        if (!element.isAnnotationPresent(RLPEncoding.class)) {
            return null;
        }
        Class<? extends Comparator> clazz = element.getAnnotation(RLPEncoding.class).keyOrdering();
        if (clazz == RLPEncoding.None.class) return null;
        try {
            Constructor<? extends Comparator> con = clazz.getDeclaredConstructor();
            con.setAccessible(true);
            return con.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("new instance of " + clazz + " failed " + e.getCause());
        }
    }

    public static Container<?> fromField(Field field) {
        Container<?> container = fromGeneric(field.getGenericType());
        Class clazz = null;
        if (field.isAnnotationPresent(RLPDecoding.class)) {
            clazz = field.getAnnotation(RLPDecoding.class).as();
        }

        if(clazz == null || clazz == Void.class) return container;
        if(container.getType() == ContainerType.RAW)
            throw new RuntimeException("@RLPDecoding.as is used on collection or map type");
        if(!field.getType().isAssignableFrom(clazz))
            throw new RuntimeException("cannot assign " + clazz + " as " + field.getType());
        if(container.getType() == ContainerType.COLLECTION){
            container.asCollection().collectionType = clazz;
        }
        if(container.getType() == ContainerType.MAP){
            container.asMap().mapType = clazz;
        }
        return container;
    }

    public static Container fromClass(Class clazz){
        if(Collection.class.isAssignableFrom(clazz)){
            CollectionContainer con = new CollectionContainer();
            con.contentType = new Raw(RLPElement.class);
            con.collectionType = clazz;
            return con;
        }
        if(Map.class.isAssignableFrom(clazz)){
            MapContainer con = new MapContainer();
            con.keyType = new Raw(RLPElement.class);
            con.valueType = new Raw(RLPElement.class);
            con.mapType = clazz;
            return con;
        }
        return new Raw(clazz);
    }

    public static Container fromGeneric(Type type) {
        if (!(type instanceof ParameterizedType)) {
            Class clazz = (Class<?>) type;
            return fromClass(clazz);
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class clazz = (Class) parameterizedType.getRawType();
        if (Collection.class.isAssignableFrom(clazz)) {
            CollectionContainer con = new CollectionContainer();
            con.contentType = fromGeneric(parameterizedType.getActualTypeArguments()[0]);
            con.collectionType = clazz;
            return con;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            MapContainer con = new MapContainer();
            con.keyType = fromGeneric(parameterizedType.getActualTypeArguments()[0]);
            con.valueType = fromGeneric(parameterizedType.getActualTypeArguments()[1]);
            con.mapType = clazz;
            return con;
        }
        return fromClass(clazz);
    }
}
