package org.tdf.rlp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class MapContainer<M, K, V> implements Container<V> {
    Class<M> mapType;

    public ContainerType getType() {
        return ContainerType.MAP;
    }

    Container<K> keyType;
    Container<V> valueType;

    @Override
    public Class<V> asRaw() {
        throw new RuntimeException("not a raw type");
    }

    @Override
    public CollectionContainer<?, V> asCollection() {
        throw new RuntimeException("not a collection container");
    }

    @Override
    public MapContainer<M, K, V> asMap() {
        return this;
    }

    static <M extends Map<K, V>, K, V> MapContainer<M, K, V> fromTypes(Class<M> mapTYpe, Class<K> keyType, Class<V> valueType) {
        return new MapContainer<>(mapTYpe, new Raw<>(keyType), new Raw<>(valueType));
    }
}
