package org.tdf.rlp;

import java.util.Collection;
import java.util.Map;

public interface Container<V> {
    ContainerType getType();

    Class<V> asRaw();

    CollectionContainer<? extends Collection<V>, V> asCollection();

    MapContainer<? extends Map<?, V>, ?, V> asMap();
}
