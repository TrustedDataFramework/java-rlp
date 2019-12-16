package org.tdf.rlp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RLPDecoding(Node.Decoder.class)
public class Node {
    // RLP annotation specify the order of field in encoded list
    public String name;

    public List<Node> children;

    public Node() {
    }

    public Node(String name) {
        this.name = name;
    }

    public void addChildren(Collection<Node> nodes) {
        if (children == null) {
            children = new ArrayList<>();
        }
        if (!nodes.stream().allMatch(x -> x != this)) {
            throw new RuntimeException("tree like object cannot add self as children");
        }
        children.addAll(nodes);
    }

    private static class Nested {
        @RLP
        private List<List<List<String>>> nested;

        public Nested() {
        }
    }

    public static class Decoder implements RLPDecoder<Node> {
        @Override
        public Node decode(RLPElement element) {
            Node n = new Node();
            n.name = element.get(0).asString();
            if (element.get(1).isNull()) return n;
            n.children = new ArrayList<>(element.get(1).size());
            for (int i = 0; i < element.get(1).size(); i++) {
                n.children.add(decode(element.get(1).get(i)));
            }
            return n;
        }
    }

    public static void main(String[] args) throws Exception {
        boolean lazy = false;
        Node root = new Node("1");
        root.addChildren(Arrays.asList(new Node("2"), new Node("3")));
        Node node2 = root.children.get(0);
        node2.addChildren(Arrays.asList(new Node("4"), new Node("5")));
        root.children.get(1).addChildren(Arrays.asList(new Node("6"), new Node("7")));

        // encode to byte array
        byte[] encoded = RLPCodec.encode(root);
        RLPElement el = RLPElement.fromEncoded(encoded, lazy);
        ObjectMapper mapper = new ObjectMapper();
        byte[] json = mapper.writeValueAsBytes(root);
        System.out.println("rlp size " + encoded.length);
        System.out.println("json size " + json.length);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            // read as rlp tree
            RLPElement.fromEncoded(encoded, lazy).as(Node.class);
        }

        long end = System.currentTimeMillis();
        System.out.println(end - start + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            // read as rlp tree
            mapper.readValue(json, Node.class);
        }
        end = System.currentTimeMillis();
        System.out.println(end - start + " ms");
    }

    public static void assertTrue(boolean b) {
        if (!b) throw new RuntimeException("assertion failed");
    }
}