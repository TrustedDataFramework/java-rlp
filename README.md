# java-rlp

fast ethereum rlp decode/encode in java

RLP Encoding/Decoding

- declaring your pojo

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

    private static class Nested{
        @RLP
        private List<List<List<String>>> nested;

        public Nested() {
        }
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

- supports List and POJO only for rlp is ordered while set is no ordered, generic list could be nested to any deepth

```java
public class Nested{
    @RLP
    private List<List<List<String>>> nested;

    public Nested() {
    }
}
```    

```java
public class Main{
    public static void main(String[] args){
        Nested nested = new Nested();
        nested.nested = new ArrayList<>();
        nested.nested.add(new ArrayList<>());
        nested.nested.get(0).add(new ArrayList<>());
        nested.nested.get(0).get(0).addAll(Arrays.asList("aaa", "bbb"));
        byte[] encoded = RLPCodec.encode(nested);
        nested = RLPCodec.decode(encoded, Nested.class);
        assert nested.nested.get(0).get(0).get(0).equals("aaa");
        assert nested.nested.get(0).get(0).get(1).equals("bbb");
    }
}
```

- encoding/decoding of Set/Map

```java
public class Main{

    private static class ByteArraySetWrapper {
        @RLP
        @RLPDecoding(as = TreeSet.class)
        @RLPEncoding(contentOrdering = BytesComparator.class)
        private Set<byte[]> bytesSet;
    }

    private static class BytesComparator implements Comparator<byte[]> {
        @Override
        public int compare(byte[] o1, byte[] o2) {
            return new BigInteger(1, o1).compareTo(new BigInteger(1, o2));
        }
    }

    public static class MapWrapper2 {
        @RLP
        @RLPDecoding(as = TreeMap.class)
        @RLPEncoding(keyOrdering = StringComparator.class)
        public Map<String, Map<String, String>> map = new HashMap<>();
    }    

    private static class StringComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o1.length() - o2.length();
        }
    }    

    public static void main(String[] args){
        ByteArraySetWrapper wrapper =
                RLPList.of(RLPList.of(RLPItem.fromLong(1), RLPItem.fromLong(2))).as(ByteArraySetWrapper.class);
        assert wrapper.bytesSet instanceof TreeSet;

        wrapper = new ByteArraySetWrapper();
        wrapper.bytesSet = new HashSet<>();
        wrapper.bytesSet.add(new byte[]{1});
        wrapper.bytesSet.add(new byte[]{2});
        wrapper.bytesSet.add(new byte[]{3});
        wrapper.bytesSet.add(new byte[]{4});
        wrapper.bytesSet.add(new byte[]{5});
        boolean sorted = true;
        int i = 0;
        for (byte[] b : wrapper.bytesSet) {
            if (new BigInteger(1, b).compareTo(BigInteger.valueOf(i + 1)) != 0) {
                sorted = false;
                break;
            }
            i++;
        }
        assert !sorted;
        RLPElement el = RLPElement.readRLPTree(wrapper).get(0);
        for (int j = 0; j < el.size(); j++) {
            assert new BigInteger(1, el.get(j).asBytes()).compareTo(BigInteger.valueOf(j + 1)) == 0;
        }

        MapWrapper2 wrapper2 = new MapWrapper2();
        wrapper2.map.put("1", new HashMap<>());
        wrapper2.map.put("22", new HashMap<>());
        wrapper2.map.put("sss", new HashMap<>());
        wrapper2.map.get("sss").put("aaa", "bbb");
        hasSorted = true;
        i = 1;
        for (String k : wrapper2.map.keySet()) {
            if (k.length() != i) {
                hasSorted = false;
                break;
            }
            i++;
        }
        assert !hasSorted;
        byte[] encoded = RLPCodec.encode(wrapper2);
        el = RLPElement.readRLPTree(wrapper2);
        for (int j = 0; j < 3; j++) {
            assert el.get(0).get(j * 2).asString().length() == j + 1;
        }
        MapWrapper2 decoded = RLPCodec.decode(encoded, MapWrapper2.class);
        assert decoded.map instanceof TreeMap;
        assert decoded.map.get("sss").get("aaa").equals("bbb");        
    }
}
```

Benchmark compare to EthereumJ:

decoding list 10000000 times: 

ethereumJ: 3698ms 
our: 2515ms

