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
        byte[] encoded = RLPElement.encode(root);
        // encode to rlp element
        RLPElement el = RLPElement.readRLPTree(root);
        // decode from byte array
        Node root2 = RLPDeserializer.deserialize(encoded, Node.class);
        assertTrue(root2.children.get(0).children.get(0).name.equals("4"));
        assertTrue(root2.children.get(0).children.get(1).name.equals("5"));
        assertTrue(root2.children.get(1).children.get(0).name.equals("6"));
        assertTrue(root2.children.get(1).children.get(1).name.equals("7"));

        Nested nested = new Nested();
        nested.nested = new ArrayList<>();
        nested.nested.add(new ArrayList<>());
        nested.nested.get(0).add(new ArrayList<>());
        nested.nested.get(0).get(0).addAll(Arrays.asList("aaa", "bbb"));
        encoded = RLPElement.encode(nested);
        nested = RLPDeserializer.deserialize(encoded, Nested.class);
        assertTrue(nested.nested.get(0).get(0).get(0).equals("aaa"));
        assertTrue(nested.nested.get(0).get(0).get(1).equals("bbb"));
    }

    public static void assertTrue(boolean b){
        if(!b) throw new RuntimeException("assertion failed");
    }
}