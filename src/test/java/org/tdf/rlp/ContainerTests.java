package org.tdf.rlp;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

@RunWith(JUnit4.class)
public class ContainerTests {
    private static interface TestMap1<A, B, C> extends Map<ByteArrayMap<A>, Collection<Map<C, Set<B>>>>{}

    private static interface TestMap2<D> extends TestMap1<Set<String>, ArrayList<byte[]>, D>{}

    private static interface TestMap3 extends TestMap2<ByteArraySet>{}

    @Test
    public void test0(){
        Container container = Container.fromNoGeneric(TestMap3.class);
    }
}
