package org.tdf.rlp;

import java.util.Map;

public class MapContainer implements Container {
    Class<? extends Map> mapType;

    public ContainerType getContainerType() {
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
