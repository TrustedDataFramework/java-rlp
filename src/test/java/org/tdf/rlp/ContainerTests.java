package org.tdf.rlp;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(JUnit4.class)
public class ContainerTests {
    private static interface TestMap1<A, B, C> extends Map<ByteArrayMap<A>, Collection<Map<C, Set<B>>>> {
    }

    private static interface TestMap2<D> extends TestMap1<Set<String>, ArrayList<byte[]>, D> {
    }

    private static interface TestMap3 extends TestMap2<ByteArraySet> {
    }

    @Test
    public void test0() {
        List<Class> classes = getHierarchyChain(TestMap3.class, Map.class)
                .collect(Collectors.toList());
        Collections.reverse(classes);
        for (Class z : classes) {
            
        }
    }

    static Stream<Class> getHierarchyChain(Class clazz, Class interfaceClass) {
        if (clazz == interfaceClass) return Stream.of(clazz);
        Stream.Builder<Class> builder = Stream.builder();
        while (clazz != interfaceClass) {
            builder.accept(clazz);
            if (clazz.getSuperclass() != null && interfaceClass.isAssignableFrom(clazz.getSuperclass())) {
                clazz = clazz.getSuperclass();
                continue;
            }
            Optional<Class> superInterface = Arrays.stream(clazz.getInterfaces()).filter(x -> interfaceClass.isAssignableFrom(x))
                    .findAny();
            if (superInterface.isPresent()) {
                clazz = superInterface.get();
                continue;
            }
            break;
        }
        builder.accept(interfaceClass);
        return builder.build();
    }

    static Type getTypeParameterOf(Class clazz, Class interfaceClass, final int index) {
        Optional<ParameterizedType> o = Arrays.stream(clazz.getGenericInterfaces())
                .filter(x -> interfaceClass == getRawTypeOf(x))
                .findAny()
                .map(x -> {
                    if (x instanceof ParameterizedType) return (ParameterizedType) x;
                    return null;
                });
        if (o.isPresent()) {
            Type[] types = o.get().getActualTypeArguments();
            if (index < types.length) return types[index];
        }
        Optional<Class> superClass = Arrays.stream(clazz.getInterfaces())
                .filter(x -> interfaceClass.isAssignableFrom(getRawTypeOf(x)))
                .findAny();
        if (superClass.isPresent()) return getTypeParameterOf(superClass.get(), interfaceClass, index);
        if (clazz.getSuperclass() != null) return getTypeParameterOf(clazz.getSuperclass(), interfaceClass, index);
        return null;
    }

    static Class getRawTypeOf(Type type) {
        if (type instanceof Class) return (Class) type;
        if (type instanceof ParameterizedType) return (Class) ((ParameterizedType) type).getRawType();
        throw new RuntimeException("unreachable");
    }
}
