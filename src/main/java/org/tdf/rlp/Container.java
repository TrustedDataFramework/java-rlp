package org.tdf.rlp;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public interface Container<V> {
    static Container<?> fromField(Field field) {
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

    static Container fromClass(Class clazz){
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

    static Container fromGeneric(Type type) {
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

    ContainerType getType();

    Class<V> asRaw();

    CollectionContainer<? extends Collection<V>, V> asCollection();

    MapContainer<? extends Map<?, V>, ?, V> asMap();
}
