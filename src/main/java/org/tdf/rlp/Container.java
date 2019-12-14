package org.tdf.rlp;

import java.util.Collection;

public interface Container<V> {
    ContainerType getType();

    Class<V> asRaw();

    CollectionContainer<? extends Collection<V>, ?> asCollection();

    MapContainer<?, ?, V> asMap();

}
