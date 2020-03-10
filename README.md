# java-rlp

Fast ethereum rlp encode, decode and object mapping in java.

## Notes

- Supports RLP primitives of ```boolean```, ```short```, ```int```, ```long```, ```java.math.BigInteger``` and ```String```.
- Supports POJO(Plain Ordinary Java Object) with at least one ```@RLP``` annotated field, a no-arguments constructor is required.
- Supports container-like interfaces of ```java.util.Collection```, ```java.util.List```, ```java.util.Set```, ```java.util.Queue```, ```java.util.Deque```, ```java.util.Map```, ```java.util.ConcurrentMap``` and their no-abstract implementations.
- Generic info of fields in POJO class could be nested to arbitrary deepth.
- Every value in ```@RLP``` of a POJO class should be unique and continous.

- ```String s = null``` will be encoded as empty string ```String s = ""```.
- For boolean, ```true``` will be encoded as ```1``` while ```false``` will be encoded as ```0```
- ```null``` values of Boolean, Byte, Short, Integer, Long and BigInteger will be encoded as ```0```
- ```null``` byte array ```byte[] bytes = null``` will be encoded as empty byte array ```byte[] bytes = new byte[0]```
- transient field will be ignored by default

## Notes on java.util.Collection and java.util.Map

- Maps will be encoded as key-value pairs RLPList [key1, value1, key2, value2, ...].
- ```java.util.TreeMap``` is recommended implementation of ```java.util.Map``` since key-value pairs in TreeMap are ordered.
- ```java.util.TreeSet``` is recommended implementation of ```java.util.Set``` since keys in TreeSet are ordered.
- Besides ```java.util.TreeMap```, the ordering of key-value pairs could be specified by ```@RLPEncoding.keyOrdering()``` when encoding.
- Besides ```java.util.TreeSet```, the ordering of element of ```java.util.Set``` could be specified by ```@RLPEncoding.keyOrdering()``` when encoding.
- If the ordering of key-value pairs is absent, the encoding of the ```java.util.Map``` may not be predictable, encoding of ```java.util.Set``` is similar.

## Examples

- RLP object mapping

```java
package org.tdf.rlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// RLP could encode & decode Tree-like object.
public class Node{
    // declared fields will be encoded with ordering of handwriting
    public String name;

    public List<Node> children;

    // field with @RLPIgnored will be ignored when encoding & decoding
    @RLPIgnored
    public String ignored;

    // if some fields are annotated with @RLP, 
    // fields without @RLP annotation will be ignored
    // the fields above is analogy to
    /**
     * @RLP(0)
     * public String name; 
     * @RLP(1)
     * public List<Node> children;
     * 
     * public String ignored;
    **/
     
    // a no-argument constructor
    public Node() {
    }

    public Node(String name) {
        this.name = name;
    }

    public void addChildren(Collection<Node> nodes){
        if(children == null){
            children = new ArrayList<>();
        }
        if (!nodes.stream().allMatch(x -> x != this)){
            throw new RuntimeException("tree like object cannot add self as children");
        }
        children.addAll(nodes);
    }

    public static void main(String[] args){
        Node root = new Node("1");
        root.addChildren(Arrays.asList(new Node("2"), new Node("3")));
        Node node2 = root.children.get(0);
        node2.addChildren(Arrays.asList(new Node("4"), new Node("5")));
        root.children.get(1).addChildren(Arrays.asList(new Node("6"), new Node("7")));

        // encode to byte array
        byte[] encoded = RLPCodec.encode(root);
        // read as rlp tree
        RLPElement el = RLPElement.readRLPTree(root);
        // decode from byte array
        Node root2 = RLPCodec.decode(encoded, Node.class);
        el = RLPElement.fromEncoded(encoded);
        // decode from rlp element
        root2 = el.as(Node.class);
        root2 = RLPCodec.decode(el, Node.class);
    }

    public static void assertTrue(boolean b){
        if(!b) throw new RuntimeException("assertion failed");
    }
}
```

- RLP encode & decode of POJO with tree-like field.

```java
public static class Tree{
    @RLPDecoding(as = ConcurrentHashMap.class) 
    /* The decoded type will be java.util.concurrent.ConcurrentHashMap 
    instead of java.util.HashMap which is the default implementation of java.util.Map. */ 
    public Map<Map<String, Set<String>>, byte[]> tree;

    // although ByteArrayMap in ethereumJ is not supported by default, you can enable it by annotation
    @RLPDecoding(as = ByteArrayMap.class)
    public Map<byte[], String> stringMap;
}
```    

```java
public class Main{
    public static void main(String[] args){
        Tree tree = new Tree();
        tree.tree = new HashMap<>();
        Map<String, Set<String>> map = new HashMap<>();
        map.put("1", new HashSet<>(Arrays.asList("1", "2", "3")));
        tree.tree.put(map, "1".getBytes());
        byte[] encoded = RLPCodec.encode(tree);
        RLPElement el = RLPElement.fromEncoded(encoded, false);
        Tree tree1 = RLPCodec.decode(encoded, Tree.class);
        assert tree1.tree instanceof ConcurrentHashMap;
        Map<String, Set<String>> tree2 = tree1.tree.keySet().stream()
                .findFirst().get();
        assert Arrays.equals(tree1.tree.get(tree2), "1".getBytes());
        assert tree2.get("1").containsAll(Arrays.asList("1", "2", "3"));
    }
}
```

- Tree-like type encode & decode without wrapper class.

```java
public class Main{
    // store generic info in a dummy field
    private abstract class Dummy2 {
        private List<Map<String, String>> dummy;
    }

    public static void main(String[] args) throws Exception{
        List<Map<String, String>> list = new ArrayList<>();
        list.add(new HashMap<>());
        list.get(0).put("1", "1");

        List<Map<String, String>> decoded = (List<Map<String, String>>) RLPCodec.decodeContainer(
                RLPCodec.encode(list),
                Container.fromField(Dummy2.class.getDeclaredField("dummy"))
        );

        assert decoded.get(0).get("1").equals("1");
    }
}
```


- Custom encode & decode configured by ```@RLPEncoding``` and ```@RLPDecoding```.

```java
package org.tdf.rlp;

import java.util.HashMap;
import java.util.Map;

public class Main{
    public static class MapEncoderDecoder implements RLPEncoder<Map<String, String>>, RLPDecoder<Map<String, String>> {
        @Override
        public Map<String, String> decode(RLPElement list) {
            Map<String, String> map = new HashMap<>(list.size() / 2);
            for (int i = 0; i < list.size(); i += 2) {
                map.put(list.get(i).asString(), list.get(i+1).asString());
            }
            return map;
        }

        @Override
        public RLPElement encode(Map<String, String> o) {
            RLPList list = RLPList.createEmpty(o.size() * 2);
            o.keySet().stream().sorted(String::compareTo).forEach(x -> {
                list.add(RLPItem.fromString(x));
                list.add(RLPItem.fromString(o.get(x)));
            });
            return list;
        }
    }

    public static class MapWrapper{
        @RLP
        @RLPEncoding(MapEncoderDecoder.class)
        @RLPDecoding(MapEncoderDecoder.class)
        public Map<String, String> map;

        public MapWrapper(Map<String, String> map) {
            this.map = map;
        }

        public MapWrapper() {
        }
    }

    public static void main(String[] args){
        Map<String, String> m = new HashMap<>();
        m.put("a", "1");
        m.put("b", "2");
        byte[] encoded = RLPCodec.encode(new MapWrapper(m));
        MapWrapper decoded = RLPCodec.decode(encoded, MapWrapper.class);
        assertTrue(decoded.map.get("a").equals("1"));
    }

    public static void assertTrue(boolean b){
        if(!b) throw new RuntimeException("assertion failed");
    }
}

```

- Add global context to avoid duplicated @RLPEncoding and @RLPDecoding

```java
public class Main{
    public static class LocalDateDecoder implements RLPDecoder<LocalDate> {
        @Override
        public LocalDate decode(RLPElement rlpElement) {
            if(rlpElement.isNull()) return null;
            int[] data = rlpElement.as(int[].class);
            return LocalDate.of(data[0], data[1], data[2]);
        }
    }

    public class LocalDateEncoder implements RLPEncoder<LocalDate> {
        @Override
        public RLPElement encode(LocalDate localDate) {
            return RLPElement.readRLPTree(
                    new int[]{localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth()}
            );
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User{
        private LocalDate birthDay;
    }

    public static void main(String[] args){
        RLPContext context = RLPContext
                .newInstance()
                .withDecoder(LocalDate.class, new LocalDateDecoder())
                .withEncoder(LocalDate.class, new LocalDateEncoder());
        RLPMapper mapper = new RLPMapper().withContext(context);
        User u = new User();
        element = mapper.readRLPTree(u);
        User decoded = mapper.decode(element, User.class);
        assert decoded.birthDay == null;

        u.birthDay = LocalDate.now();
        byte[] encoded = mapper.encode(u);
        decoded = mapper.decode(encoded, User.class);
        System.out.println(decoded.birthDay.format(DateTimeFormatter.ISO_DATE));    
    }

}
```

## Benchmark 

- see RLPTest.performanceDecode for benchmark

Benchmark compare to EthereumJ:

Platform: 

- Motherborad: B450M
- CPU: Ryzen 3700x, 8 core, 16 threads
- Memory: (16GB x 2) DDR4 3200hz

decoding list 10000000 times: 

ethereumJ: 3698ms 
our: 2515ms

