package org.tdf.rlp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class MapContainer<M extends Map<K, V>, K, V> implements Container<V> {
    Class mapType;

    public ContainerType getType() {
        return ContainerType.MAP;
    }

    Container keyType;
    Container valueType;

    @Override
    public Class<V> asRaw() {
        throw new RuntimeException("not a raw type");
    }

    @Override
    public CollectionContainer<? extends Collection<V>, V> asCollection() {
        throw new RuntimeException("not a collection container");
    }

    @Override
    public MapContainer<? extends Map<?, V>, ?, V> asMap() {
        return this;
    }

    static <M extends Map<K, V>, K, V> MapContainer<M, K, V> fromTypes(Class<M> mapTYpe, Class<K> keyType, Class<V> valueType) {
        return new MapContainer<>(mapTYpe, new Raw<>(keyType), new Raw<>(valueType));
    }
}
