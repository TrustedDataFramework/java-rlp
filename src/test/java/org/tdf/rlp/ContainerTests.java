package org.tdf.rlp;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class ContainerTests {
    private static interface TestMap1 extends Map<ByteArrayMap<Map<String, List<Integer>>>, Collection<Map<byte[], ByteArraySet>>>{}
    @Test
    @Ignore
    public void test0(){
        Container container = RLPUtils.fromClass(TestMap1.class);
        Container container2 = RLPUtils.fromType(TestMap1.class.getGenericInterfaces()[0]);
    }
}
