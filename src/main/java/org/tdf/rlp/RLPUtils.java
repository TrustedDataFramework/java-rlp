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

    static Comparator getContentOrdering(AnnotatedElement element){
        if (!element.isAnnotationPresent(RLPEncoding.class)) {
            return null;
        }
        Class<? extends Comparator> clazz = element.getAnnotation(RLPEncoding.class).contentOrdering();
        if(clazz == RLPEncoding.None.class) return null;
        try{
            Constructor<? extends Comparator> con = clazz.getDeclaredConstructor();
            con.setAccessible(true);
            return con.newInstance();
        }catch (Exception e){
            throw new RuntimeException("new instance of " + clazz + " failed " + e.getMessage());
        }
    }

    static Comparator getKeyOrdering(AnnotatedElement element){
        if (!element.isAnnotationPresent(RLPEncoding.class)) {
            return null;
        }
        Class<? extends Comparator> clazz = element.getAnnotation(RLPEncoding.class).keyOrdering();
        if(clazz == RLPEncoding.None.class) return null;
        try{
            return clazz.newInstance();
        }catch (Exception e){
            throw new RuntimeException("new instance of " + clazz + " failed " + e.getCause());
        }
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

    enum ContainerType{
        RAW,
        COLLECTION,
        MAP
    }

    interface Container{
        ContainerType getContainerType();
        Class<?> asRawType();
        CollectionContainer asCollectionContainer();
        MapContainer asMapContainer();
    }

    static class Raw implements Container{
        Class<?> rawType;
        public ContainerType getContainerType(){
            return ContainerType.RAW;
        }

        @Override
        public Class<?> asRawType() {
            return rawType;
        }

        @Override
        public CollectionContainer asCollectionContainer() {
            throw new RuntimeException("not a collection container");
        }

        @Override
        public MapContainer asMapContainer() {
            throw new RuntimeException("not a map container");        }
    }

    static class CollectionContainer implements Container{
        Class<? extends Collection> collectionType;

        public ContainerType getContainerType(){
            return ContainerType.COLLECTION;
        }

        Container contentType;

        @Override
        public Class<?> asRawType() {
            throw new RuntimeException("not a raw type");
        }

        @Override
        public CollectionContainer asCollectionContainer() {
            return this;
        }

        @Override
        public MapContainer asMapContainer() {
            throw new RuntimeException("not a map container");
        }
    }

    static class MapContainer implements Container{
        Class<? extends Map> mapType;

        public ContainerType getContainerType(){
            return ContainerType.MAP;
        }

        Container keyType;
        Container valueType;

        @Override
        public Class<?> asRawType() {
            throw new RuntimeException("not a raw type");
        }

        @Override
        public CollectionContainer asCollectionContainer() {
            throw new RuntimeException("not a collection container");
        }

        @Override
        public MapContainer asMapContainer() {
            return this;
        }
    }

    static Container resolveContainer(Type type){
        if(!(type instanceof ParameterizedType)){
            Raw raw = new Raw();
            raw.rawType = (Class<?>) type;
            return raw;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class clazz = (Class) parameterizedType.getRawType();
        if(Collection.class.isAssignableFrom(clazz)){
            CollectionContainer con = new CollectionContainer();
            con.contentType = resolveContainer(parameterizedType.getActualTypeArguments()[0]);
            con.collectionType = clazz;
            return con;
        }
        if(Map.class.isAssignableFrom(clazz)){
            MapContainer con = new MapContainer();
            con.keyType = resolveContainer(parameterizedType.getActualTypeArguments()[0]);
            con.valueType = resolveContainer(parameterizedType.getActualTypeArguments()[1]);
            con.mapType = clazz;
            return con;
        }
        Raw raw = new Raw();
        raw.rawType = clazz;
        return raw;
    }
}
