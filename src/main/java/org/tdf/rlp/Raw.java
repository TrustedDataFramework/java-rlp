package org.tdf.rlp;

class Raw<V> implements Container<V> {
    Class<V> rawType;

    public ContainerType getType() {
        return ContainerType.RAW;
    }

    @Override
    public Class<V> asRaw() {
        return rawType;
    }

    @Override
    public CollectionContainer<?, V> asCollection() {
        throw new RuntimeException("not a collection container");
    }

    @Override
    public MapContainer<?, ?, V> asMap() {
        throw new RuntimeException("not a map container");
    }

    Raw() {
    }

    Raw(Class<V> rawType) {
        this.rawType = rawType;
    }
}
