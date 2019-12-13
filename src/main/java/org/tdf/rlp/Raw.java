package org.tdf.rlp;

class Raw implements Container {
    Class<?> rawType;

    public ContainerType getContainerType() {
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
        throw new RuntimeException("not a map container");
    }

    public Raw() {
    }

    Raw(Class<?> rawType) {
        this.rawType = rawType;
    }
}
