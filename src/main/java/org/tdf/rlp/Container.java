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

        if (clazz == null || clazz == Void.class) return container;
        if (container.getType() == ContainerType.RAW)
            throw new RuntimeException("@RLPDecoding.as is used on collection or map type");
        if (!field.getType().isAssignableFrom(clazz))
            throw new RuntimeException("cannot assign " + clazz + " as " + field.getType());
        if (container.getType() == ContainerType.COLLECTION) {
            container.asCollection().collectionType = clazz;
        }
        if (container.getType() == ContainerType.MAP) {
            container.asMap().mapType = clazz;
        }
        return container;
    }

    static Container fromNoGeneric(Class clazz) {
        if (Collection.class.isAssignableFrom(clazz)) {
            CollectionContainer con = new CollectionContainer();
            Class contentType = RLPUtils.getGenericTypeParameterRecursively(clazz, 0);
            con.contentType = contentType == null ? null : new Raw(contentType);
            con.collectionType = clazz;
            return con;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            MapContainer con = new MapContainer();
            Class keyType = RLPUtils.getGenericTypeParameterRecursively(clazz, 0);
            con.keyType = keyType == null ? null : new Raw(keyType);
            Class valueType = RLPUtils.getGenericTypeParameterRecursively(clazz, 1);
            con.valueType = valueType == null ? null : new Raw(valueType);
            con.mapType = clazz;
            return con;
        }
        return new Raw(clazz);
    }

    static Container fromGeneric(Type type) {
        if (type instanceof Class) {
            return fromNoGeneric((Class) type);
        }
        if (!(type instanceof ParameterizedType)) throw new RuntimeException(type + " is not allowed in rlp decoding");
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] types = parameterizedType.getActualTypeArguments();
        Class clazz = (Class) parameterizedType.getRawType();
        Container container = fromNoGeneric(clazz);
        switch (container.getType()) {
            case RAW:
                return container;
            case MAP: {
                MapContainer con = container.asMap();
                int i = 0;
                if (con.keyType == null) {
                    con.keyType = i < types.length ? fromGeneric(types[i++]) : null;
                }
                if(con.valueType == null){
                    con.valueType = i < types.length ? fromGeneric(types[i++]) : null;
                }
                return con;
            }
            case COLLECTION: {
                CollectionContainer con = container.asCollection();
                if (con.contentType == null) {
                    con.contentType = 0 < types.length ? fromGeneric(types[0]) : null;
                }
                return con;
            }
            default:
                throw new RuntimeException("this is unreachable");
        }
    }

    ContainerType getType();

    Class<V> asRaw();

    CollectionContainer<? extends Collection<V>, V> asCollection();

    MapContainer<? extends Map<?, V>, ?, V> asMap();
}
