# java-rlp

Fast ethereum rlp decode & encode in java.

## Notes

- Supports RLP primitives of boolean, short, int, long, java.math.BigInteger and java.lang.String.
- Supports POJO with a no-arguments constructor and at least one @RLP annotated field.
- Supports container-like interfaces of java.util.Collection, java.util.List, java.util.Set, java.util.Queue, java.util.Deque, java.util.Map, java.util.ConcurrentMap and their implementations.
- Generic field could be nested to any deepth.
- The value in @RLP should be continously natural numbers.

- null String will be encoded as empty String ```""```
- ```true``` are encoded as ```1``` while ```false``` are encoded as ```0```
- null value of Boolean, Byte, Short, Integer, Long and BigInteger will be encoded as zero 
- null byte array ```byte[] bytes = null;``` will be encoded as empty byte array ```byte[] bytes = new byte[0];```

## Examples

- RLP encoding & decoding of your POJO.

```java
package org.tdf.rlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// RLP could encode & decode Tree-like object.
public class Node{
    // RLP annotation specify the order of field in encoded list
    @RLP(0)
    public String name;

    @RLP(1)
    public List<Node> children;

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

- Notes on Collection and Map encode & decode
    - Map are encoded as key-value pair RLPList [key1, value1, key2, value2, ...]
    - TreeMap is recommended implementation of Map since key-value pair in TreeMap is ordered.
    - TreeSet is recommended implementation of Set since key in TreeSet is ordered.
    - Instead of TreeMap, you can specify the ordering of key-value pair in encoded RLPList by @RLPEncoding.keyOrdering().
    - Instead of TreeSet, you can specify the ordering of element in encoded RLPList by @RLPEncoding.keyOrdering().
    - If the Map or Set is not ordered and no ordering specified by annotation, the RLPList encoded is not determined. This may break the idempotency of rlp encoding.

```java
public static class Tree{
    @RLP
    @RLPDecoding(as = ConcurrentHashMap.class) 
    /* The decoded type will be java.util.concurrent.ConcurrentHashMap 
    instead of java.util.HashMap which is the default implementation of java.util.Map. */ 
    public Map<ByteArrayMap<Set<String>>, byte[]> tree;
}
```    

```java
public class Main{
    public static void main(String[] args){
        Tree tree = new Tree();
        tree.tree = new HashMap<>();
        ByteArrayMap<Set<String>> map = new ByteArrayMap<>();
        map.put("1".getBytes(), new HashSet<>(Arrays.asList("1", "2", "3")));
        tree.tree.put(map, "1".getBytes());
        byte[] encoded = RLPCodec.encode(tree);
        RLPElement el = RLPElement.fromEncoded(encoded, false);
        Tree tree1 = RLPCodec.decode(encoded, Tree.class);
        assert tree1.tree instanceof ConcurrentHashMap;
        ByteArrayMap<Set<String>> tree2 = tree1.tree.keySet().stream()
                .findFirst().get();
        assert Arrays.equals(tree1.tree.get(tree2), "1".getBytes());
        assert tree2.get("1".getBytes()).containsAll(Arrays.asList("1", "2", "3"));
    }
}
```

- Tree-like encoding/decoding without wrapper class.

```java
public class Main{
    // store generic info in a dummy field
    private abstract class Dummy2 {
        private List<ByteArrayMap<String>> dummy;
    }

    public static void main(String[] args) throws Exception{
        List<ByteArrayMap<String>> list = new ArrayList<>();
        list.add(new ByteArrayMap<>());
        list.get(0).put("1".getBytes(), "1");

        List<Map<byte[], String>> decoded = (List<Map<byte[], String>>) RLPCodec.decodeContainer(
                RLPCodec.encode(list),
                Container.fromField(Dummy2.class.getDeclaredField("dummy"))
        );

        assert decoded.get(0).get("1".getBytes()).equals("1");
    }
}
```


- Custom encode & decode with annotation configuration.

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
    

Benchmark compare to EthereumJ:

decoding list 10000000 times: 

ethereumJ: 3698ms 
our: 2515ms

