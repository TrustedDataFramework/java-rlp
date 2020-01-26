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
    Container keyType;
    Container valueType;
    MapContainer(Class mapType) {
        this.mapType = mapType;
    }

    public ContainerType getType() {
        return ContainerType.MAP;
    }

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
}
