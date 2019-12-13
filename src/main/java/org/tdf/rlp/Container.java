package org.tdf.rlp;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

interface Container {
    ContainerType getContainerType();

    Class<?> asRawType();

    CollectionContainer asCollectionContainer();

    MapContainer asMapContainer();

    static Container resolveField(Field field) {
        Container container = resolveContainerofGeneric(field.getGenericType());
        Class clazz = null;
        if (field.isAnnotationPresent(RLPDecoding.class)) {
            clazz = field.getAnnotation(RLPDecoding.class).as();
        }

        if(clazz == null || clazz == Void.class) return container;
        if(container.getContainerType() == ContainerType.RAW)
            throw new RuntimeException("@RLPDecoding.as is used on collection or map type");
        if(!field.getType().isAssignableFrom(clazz))
            throw new RuntimeException("cannot assign " + clazz + " as " + field.getType());
        if(container.getContainerType() == ContainerType.COLLECTION){
            container.asCollectionContainer().collectionType = clazz;
        }
        if(container.getContainerType() == ContainerType.MAP){
            container.asMapContainer().mapType = clazz;
        }
        return container;
    }

    static Container resolveContainerOfNoGeneric(Class clazz){
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

    static Container resolveContainerofGeneric(Type type) {
        if (!(type instanceof ParameterizedType)) {
            Class clazz = (Class<?>) type;
            return resolveContainerOfNoGeneric(clazz);
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class clazz = (Class) parameterizedType.getRawType();
        if (Collection.class.isAssignableFrom(clazz)) {
            CollectionContainer con = new CollectionContainer();
            con.contentType = resolveContainerofGeneric(parameterizedType.getActualTypeArguments()[0]);
            con.collectionType = clazz;
            return con;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            MapContainer con = new MapContainer();
            con.keyType = resolveContainerofGeneric(parameterizedType.getActualTypeArguments()[0]);
            con.valueType = resolveContainerofGeneric(parameterizedType.getActualTypeArguments()[1]);
            con.mapType = clazz;
            return con;
        }
        return resolveContainerOfNoGeneric(clazz);
    }
}
