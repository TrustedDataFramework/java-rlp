package org.tdf.rlp;

import java.util.Collection;

class CollectionContainer implements Container {
    Class<? extends Collection> collectionType;

    public ContainerType getContainerType() {
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