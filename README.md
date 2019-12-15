# java-rlp

fast ethereum rlp decode/encode in java

## Notes

- supports RLP primitives of boolean, short, int, long, BigInteger, String
- supports container-like interfaces of Collection, List, Set, Queue, Deque, Map, ConcurrentMap.
- supports POJO with a no-arguments constructor and at least one @RLP annotated field.
- Generic field could be nested to any deepth.
- the value in @RLP, should be ordered strictly, e.g. 

```java
public class POJO{
    @RLP(0)
    public int field1;
    @RLP(2)
    public int field2;
    @RLP(3)
    public int field3;
}
``` 

is not allowed, since ```@RLP(1)``` is missing

## Examples

- RLP encoding/decoding of your pojo

```java
package org.tdf.rlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Node{
    // RLP annotation specify the order of field in encoded list
    @RLP(0)
    public String name;

    @RLP(1)
    public List<Node> children;

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

- RLP encoding/decoding of pojo with tree-like structure

```java
public static class Tree{
    @RLP
    @RLPDecoding(as = ConcurrentHashMap.class 
        /* the type of deserialized tree will be ConcurrentHashMap instead of defualt implementation HashMap*/ 
    ) 
    // map are serialized as RLPList [key, value, key, value, ...] 
    // use @RLPEncoding.keyOrdering() to specify the ordering of key-value pair
    // use @RLPEncoding.contentOrdering() to speficy the ordering of set or anther collection type
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

- tree-like encoding/decoding without wrapper

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


- custom encoding/decoding

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

