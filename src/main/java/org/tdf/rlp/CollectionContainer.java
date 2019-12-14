package org.tdf.rlp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class CollectionContainer<C extends Collection<T>, T> implements Container<T> {
    Class<C> collectionType;

    public ContainerType getType() {
        return ContainerType.COLLECTION;
    }

    Container<T> contentType;

    @Override
    public Class<T> asRaw() {
        throw new RuntimeException("not a raw type");
    }

    @Override
    public CollectionContainer<C, T> asCollection() {
        return this;
    }

    @Override
    public MapContainer<? extends Map<?, T>, ?, T> asMap() {
        throw new RuntimeException("not a map container");
    }

    static <C extends Collection<V>, V> CollectionContainer<C, V> fromTypes(Class<C> collectionType, Class<V> elementType) {
        return new CollectionContainer<>(collectionType, new Raw<>(elementType));
    }
}