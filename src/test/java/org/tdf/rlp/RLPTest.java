package org.tdf.rlp;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.tdf.rlp.Container.fromField;
import static org.tdf.rlp.RLPCodec.*;
import static org.tdf.rlp.RLPItem.NULL;
import static org.tdf.rlp.RLPItem.ONE;

@RunWith(JUnit4.class)
public class RLPTest {
    public static class TestSerializer {
        @RLP(0)
        public List<String> strings;

        public TestSerializer() {
        }

        public TestSerializer(List<String> strings) {
            this.strings = strings;
        }
    }

    public static class Node {
        @RLP(0)
        public String name;
        @RLP(1)
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
    }

    public static class RLPSerializer {
        public static RLPSerializer SERIALIZER = new RLPSerializer();

        public byte[] serialize(Object o) {
            return RLPElement.readRLPTree(o).getEncoded();
        }
    }

    public static class HexBytes {
        public static String encode(byte[] data) {
            return Hex.encodeHexString(data);
        }

        public static byte[] parse(String s) throws Exception {
            return Hex.decodeHex(s);
        }

        public static byte[] decode(String s) throws Exception {
            return parse(s);
        }
    }

    @Test
    public void test0() {
        byte[] data = RLPSerializer.SERIALIZER.serialize(-1L);
        assert RLPCodec.decode(data, Long.class) == -1L;
        data = RLPSerializer.SERIALIZER.serialize(0L);
        assert RLPCodec.decode(data, Long.class) == 0;
        data = RLPSerializer.SERIALIZER.serialize(Long.MAX_VALUE);
        assert RLPCodec.decode(data, Long.class) == Long.MAX_VALUE;
        data = RLPSerializer.SERIALIZER.serialize(Long.MIN_VALUE);
        assert RLPCodec.decode(data, Long.class) == Long.MIN_VALUE;

        data = RLPSerializer.SERIALIZER.serialize(Integer.valueOf(0));
        assert RLPCodec.decode(data, Integer.class) == 0;
        data = RLPSerializer.SERIALIZER.serialize(Integer.MIN_VALUE);
        assert RLPCodec.decode(data, Integer.class) == Integer.MIN_VALUE;
        data = RLPSerializer.SERIALIZER.serialize(Integer.MAX_VALUE);
        assert RLPCodec.decode(data, Integer.class) == Integer.MAX_VALUE;
        data = RLPSerializer.SERIALIZER.serialize(Integer.valueOf(-1));
        assert RLPCodec.decode(data, Integer.class) == -1;

        data = RLPSerializer.SERIALIZER.serialize(Short.valueOf((short) 0));
        assert RLPCodec.decode(data, Short.class) == 0;
        data = RLPSerializer.SERIALIZER.serialize(Short.MIN_VALUE);
        assert RLPCodec.decode(data, Short.class) == Short.MIN_VALUE;
        data = RLPSerializer.SERIALIZER.serialize(Short.MAX_VALUE);
        assert RLPCodec.decode(data, Short.class) == Short.MAX_VALUE;
    }

    @Test
    public void test1() {
        byte[] data = RLPSerializer.SERIALIZER.serialize(new TestSerializer(Arrays.asList("1", "2", "3")));
        TestSerializer serializer = RLPCodec.decode(data, TestSerializer.class);
        assert serializer.strings.get(0).equals("1");
        assert serializer.strings.get(1).equals("2");
        assert serializer.strings.get(2).equals("3");
        RLPList list = RLPElement.fromEncoded(data).asRLPList();
        list.setEncoded(null);
        assertArrayEquals(data, list.getEncoded());
    }


    @Test
    public void testEncodeList() {
        String[] test = new String[]{"cat", "dog"};
        String expected = "c88363617483646f67";
        byte[] encoderesult = RLPSerializer.SERIALIZER.serialize(test);
        assertEquals(expected, HexBytes.encode(encoderesult));

        String[] decodedTest = RLPCodec.decode(encoderesult, String[].class);
        assertArrayEquals(decodedTest, test);

        test = new String[]{"dog", "god", "cat"};
        expected = "cc83646f6783676f6483636174";
        encoderesult = RLPSerializer.SERIALIZER.serialize(test);
        assertEquals(expected, HexBytes.encode(encoderesult));
        assertArrayEquals(RLPElement.fromEncoded(encoderesult).asRLPList().stream().map(x -> x.asRLPItem().asString()).toArray(), test);
    }

    @Test
    public void test333() {
        assert RLPList.createEmpty() instanceof RLPElement;
    }

    @Test
    /** encode byte */
    public void test4() {

        byte[] expected = {(byte) 0x80};
        byte[] data = encodeByte((byte) 0);
        assertArrayEquals(expected, data);

        byte[] expected2 = {(byte) 0x78};
        data = encodeByte((byte) 120);
        assertArrayEquals(expected2, data);

        byte[] expected3 = {(byte) 0x7F};
        data = encodeByte((byte) 127);
        assertArrayEquals(expected3, data);
    }

    @Test
    /** encode short */
    public void test5() {

        byte[] expected = {(byte) 0x80};
        byte[] data = encodeShort((byte) 0);
        assertArrayEquals(expected, data);

        byte[] expected2 = {(byte) 0x78};
        data = encodeShort((byte) 120);
        assertArrayEquals(expected2, data);

        byte[] expected3 = {(byte) 0x7F};
        data = encodeShort((byte) 127);
        assertArrayEquals(expected3, data);

        byte[] expected4 = {(byte) 0x82, (byte) 0x76, (byte) 0x5F};
        data = encodeShort((short) 30303);
        assertArrayEquals(expected4, data);

        byte[] expected5 = {(byte) 0x82, (byte) 0x4E, (byte) 0xEA};
        data = encodeShort((short) 20202);
        assertArrayEquals(expected5, data);
    }

    @Test
    /** encode int */
    public void testEncodeInt() {

        byte[] expected = {(byte) 0x80};
        byte[] data = encodeInt(0);
        assertArrayEquals(expected, data);
        assertEquals(0, decodeInt(data));

        byte[] expected2 = {(byte) 0x78};
        data = encodeInt(120);
        assertArrayEquals(expected2, data);
        assertEquals(120, decodeInt(data));

        byte[] expected3 = {(byte) 0x7F};
        data = encodeInt(127);
        assertArrayEquals(expected3, data);
        assertEquals(127, decodeInt(data));

        assertEquals(256, decodeInt(encodeInt(256)));
        assertEquals(255, decodeInt(encodeInt(255)));
        assertEquals(127, decodeInt(encodeInt(127)));
        assertEquals(128, decodeInt(encodeInt(128)));

        data = encodeInt(1024);
        assertEquals(1024, decodeInt(data));

        byte[] expected4 = {(byte) 0x82, (byte) 0x76, (byte) 0x5F};
        data = encodeInt(30303);
        assertArrayEquals(expected4, data);
        assertEquals(30303, decodeInt(data));

        byte[] expected5 = {(byte) 0x82, (byte) 0x4E, (byte) 0xEA};
        data = encodeInt(20202);
        assertArrayEquals(expected5, data);
        assertEquals(20202, decodeInt(data));

        byte[] expected6 = {(byte) 0x83, 1, 0, 0};
        data = encodeInt(65536);
        assertArrayEquals(expected6, data);
        assertEquals(65536, decodeInt(data));

        byte[] expected8 = {(byte) 0x84, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        data = encodeInt(Integer.MAX_VALUE);
        assertArrayEquals(expected8, data);
        assertEquals(Integer.MAX_VALUE, decodeInt(data));
    }

    @Test(expected = RuntimeException.class)
    public void incorrectZero() {
        decodeInt(new byte[]{0x00});
    }

    @Test
    public void testMaxNumerics() {
        int expected1 = Integer.MAX_VALUE;
        assertEquals(expected1, decodeInt(encodeInt(expected1)));
        short expected2 = Short.MAX_VALUE;
        assertEquals(expected2, decodeShort(encodeShort(expected2)));
        long expected3 = Long.MAX_VALUE;
        assertEquals(expected3, decodeLong(encodeBigInteger(BigInteger.valueOf(expected3))));
    }

    @Test
    /** encode BigInteger */
    public void test6() {

        byte[] expected = new byte[]{(byte) 0x80};
        byte[] data = encodeBigInteger(BigInteger.ZERO);
        assertArrayEquals(expected, data);
    }


    @Test
    /** encode string */
    public void test7() {

        byte[] data = encodeString("");
        assertArrayEquals(new byte[]{(byte) 0x80}, data);

        byte[] expected = {(byte) 0x90, (byte) 0x45, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x65,
                (byte) 0x75, (byte) 0x6D, (byte) 0x4A, (byte) 0x20, (byte) 0x43, (byte) 0x6C,
                (byte) 0x69, (byte) 0x65, (byte) 0x6E, (byte) 0x74};

        String test = "EthereumJ Client";
        data = encodeString(test);

        assertArrayEquals(expected, data);

        String test2 = "Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++";

        byte[] expected2 = {(byte) 0xAD, (byte) 0x45, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x65,
                (byte) 0x75, (byte) 0x6D, (byte) 0x28, (byte) 0x2B, (byte) 0x2B, (byte) 0x29, (byte) 0x2F,
                (byte) 0x5A, (byte) 0x65, (byte) 0x72, (byte) 0x6F, (byte) 0x47, (byte) 0x6F, (byte) 0x78,
                (byte) 0x2F, (byte) 0x76, (byte) 0x30, (byte) 0x2E, (byte) 0x35, (byte) 0x2E, (byte) 0x30,
                (byte) 0x2F, (byte) 0x6E, (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x73, (byte) 0x65,
                (byte) 0x73, (byte) 0x2F, (byte) 0x4C, (byte) 0x69, (byte) 0x6E, (byte) 0x75, (byte) 0x78,
                (byte) 0x2F, (byte) 0x67, (byte) 0x2B, (byte) 0x2B};

        data = encodeString(test2);
        assertArrayEquals(expected2, data);

        String test3 = "Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++";

        byte[] expected3 = {(byte) 0xB8, (byte) 0x5A,
                (byte) 0x45, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x65,
                (byte) 0x75, (byte) 0x6D, (byte) 0x28, (byte) 0x2B, (byte) 0x2B, (byte) 0x29, (byte) 0x2F,
                (byte) 0x5A, (byte) 0x65, (byte) 0x72, (byte) 0x6F, (byte) 0x47, (byte) 0x6F, (byte) 0x78,
                (byte) 0x2F, (byte) 0x76, (byte) 0x30, (byte) 0x2E, (byte) 0x35, (byte) 0x2E, (byte) 0x30,
                (byte) 0x2F, (byte) 0x6E, (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x73, (byte) 0x65,
                (byte) 0x73, (byte) 0x2F, (byte) 0x4C, (byte) 0x69, (byte) 0x6E, (byte) 0x75, (byte) 0x78,
                (byte) 0x2F, (byte) 0x67, (byte) 0x2B, (byte) 0x2B,

                (byte) 0x45, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x65,
                (byte) 0x75, (byte) 0x6D, (byte) 0x28, (byte) 0x2B, (byte) 0x2B, (byte) 0x29, (byte) 0x2F,
                (byte) 0x5A, (byte) 0x65, (byte) 0x72, (byte) 0x6F, (byte) 0x47, (byte) 0x6F, (byte) 0x78,
                (byte) 0x2F, (byte) 0x76, (byte) 0x30, (byte) 0x2E, (byte) 0x35, (byte) 0x2E, (byte) 0x30,
                (byte) 0x2F, (byte) 0x6E, (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x73, (byte) 0x65,
                (byte) 0x73, (byte) 0x2F, (byte) 0x4C, (byte) 0x69, (byte) 0x6E, (byte) 0x75, (byte) 0x78,
                (byte) 0x2F, (byte) 0x67, (byte) 0x2B, (byte) 0x2B};

        data = encodeString(test3);
        assertArrayEquals(expected3, data);
    }

    @Test
    /** encode byte array */
    public void test8() throws Exception {

        String byteArr = "ce73660a06626c1b3fda7b18ef7ba3ce17b6bf604f9541d3c6c654b7ae88b239"
                + "407f659c78f419025d785727ed017b6add21952d7e12007373e321dbc31824ba";

        byte[] byteArray = HexBytes.parse(byteArr);

        String expected = "b840" + byteArr;

        assertEquals(expected, HexBytes.encode(encodeBytes(byteArray)));
        assertEquals(expected, HexBytes.encode(RLPItem.fromBytes(byteArray).getEncoded()));
        assertEquals(expected, HexBytes.encode(
                RLPElement.fromEncoded(HexBytes.decode(expected)).getEncoded()
        ));
    }

    @Test
    /** encode list */
    public void test9() {

        byte[] actuals = RLPList.createEmpty().getEncoded();
        assertArrayEquals(new byte[]{(byte) 0xc0}, actuals);
    }

    @Test
    /** encode null value */
    public void testencodeBytesNull() {

        byte[] actuals = encodeBytes(null);
        assertArrayEquals(new byte[]{(byte) 0x80}, actuals);
    }


    @Test
    /** encode single byte 0x00 */
    public void testencodeBytesZero() {

        byte[] actuals = encodeBytes(new byte[]{0x00});
        assertArrayEquals(new byte[]{0x00}, actuals);
    }

    @Test
    /** encode single byte 0x01 */
    public void testencodeBytesOne() {

        byte[] actuals = encodeBytes(new byte[]{0x01});
        assertArrayEquals(new byte[]{(byte) 0x01}, actuals);
    }

    @Test
    /** found bug encode list affects element value,
     hhh... not really at  the end but keep the test */
    public void test10() {

        /* 2 */
        byte[] prevHash =
                {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        prevHash = encodeBytes(prevHash);

        /* 2 */
        byte[] uncleList = HashUtil.sha3(RLPList.createEmpty().getEncoded());

        /* 3 */
        byte[] coinbase =
                {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00};
        coinbase = encodeBytes(coinbase);

        byte[] header = encodeElements(
                Arrays.asList(prevHash, uncleList, coinbase));

        assertEquals("f856a000000000000000000000000000000000000000000000000000000000000000001dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000",
                HexBytes.encode(header));
    }

    @Test
    public void testEncodeEmptyString() {
        String test = "";
        String expected = "80";
        byte[] encoderesult = encodeString(test);
        assertEquals(expected, HexBytes.encode(encoderesult));

        String decodeResult = decodeString(encoderesult);
        assertEquals(test, decodeResult);
    }

    @Test
    public void testEncodeShortString() throws Exception {
        String test = "dog";
        String expected = "83646f67";
        byte[] encoderesult = encodeString(test);
        assertEquals(expected, HexBytes.encode(encoderesult));
        assert RLPElement.fromEncoded(HexBytes.decode(expected)).asRLPItem().asString().equals(test);

        byte[] decodeResult = RLPElement.fromEncoded(encoderesult).asRLPItem().asBytes();
        assertEquals(test, new String(decodeResult, StandardCharsets.US_ASCII));
    }

    @Test
    /**
     * ethereumJ: 3770ms
     * our: 2465ms
     */
    public void performanceDecode() throws Exception {
        boolean performanceEnabled = false;

        if (performanceEnabled) {
            String blockRaw = "f8cbf8c7a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a02f4399b08efe68945c1cf90ffe85bbe3ce978959da753f9e649f034015b8817da00000000000000000000000000000000000000000000000000000000000000000834000008080830f4240808080a004994f67dc55b09e814ab7ffc8df3686b4afb2bb53e60eae97ef043fe03fb829c0c0";
            byte[] payload = HexBytes.decode(blockRaw);

            final int ITERATIONS = 10000000;
            RLPList list = null;
            System.out.println("Starting " + ITERATIONS + " decoding iterations...");

            long start1 = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                list = RLPElement.fromEncoded(payload, false).asRLPList();
            }
            long end1 = System.currentTimeMillis();

            long start2 = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                list = RLPElement.fromEncoded(payload, true).asRLPList();
            }
            long end2 = System.currentTimeMillis();

            System.out.println("Result RLPElement.fromEncoded\t: " + (end1 - start1) + "ms and\t " + (payload.length * ITERATIONS) + " bytes for each resulting object list");
            System.out.println("Result RLPElement.fromEncoded lazy\t: " + (end2 - start2) + "ms and\t " + (payload.length * ITERATIONS) + " bytes for each resulting object list");
        } else {
            System.out.println("Performance test for RLP.decode() disabled");
        }
    }

    @Test // found this with a bug - nice to keep
    public void encodeEdgeShortList() throws Exception {

        String expectedOutput = "f7c0c0b4600160003556601359506301000000600035040f6018590060005660805460016080530160005760003560805760203560003557";

        byte[] rlpKeysList = HexBytes.decode("c0");
        byte[] rlpValuesList = HexBytes.decode("c0");
        byte[] rlpCode = HexBytes.decode("b4600160003556601359506301000000600035040f6018590060005660805460016080530160005760003560805760203560003557");
        byte[] output = encodeElements(Arrays.asList(rlpKeysList, rlpValuesList, rlpCode));

        assertEquals(expectedOutput, HexBytes.encode(output));
        assertArrayEquals(RLPElement.fromEncoded(HexBytes.decode(expectedOutput)).getEncoded(), HexBytes.decode(expectedOutput));
        assertArrayEquals(
                RLPElement.fromEncoded(HexBytes.decode(expectedOutput))
                        .asRLPList().stream().map(x -> x.getEncoded()).toArray(),
                new byte[][]{rlpKeysList, rlpValuesList, rlpCode}
        );
    }

    @Test
    public void encodeBigIntegerEdge_1() {

        BigInteger integer = new BigInteger("80", 10);
        byte[] encodedData = encodeBigInteger(integer);
        assert RLPElement.fromEncoded(encodedData).asRLPItem().asInt() == 80;
    }

    @Test
    public void testEncodeInt_7f() throws Exception {
        String result = HexBytes.encode(encodeInt(0x7f));
        String expected = "7f";
        assertEquals(expected, result);
        assert RLPElement.fromEncoded(HexBytes.decode(expected)).asRLPItem().asInt() == 0x7f;
    }

    @Test
    public void testEncodeInt_80() throws Exception {
        String result = HexBytes.encode(encodeInt(0x80));
        String expected = "8180";
        assertEquals(expected, result);
        assert RLPElement.fromEncoded(HexBytes.decode(expected)).asRLPItem().asInt() == 0x80;
    }


    @Test
    public void testEncode_ED() throws Exception {
        String result = HexBytes.encode(encodeInt(0xED));
        String expected = "81ed";
        assertEquals(expected, result);
        assert RLPElement.fromEncoded(HexBytes.decode(result)).asRLPItem().asInt() == 0xED;
    }

    // encode a binary tree
    @Test
    public void testTreeLike() {
        Node root = new Node("1");
        root.addChildren(Arrays.asList(new Node("2"), new Node("3")));
        Node node2 = root.children.get(0);
        node2.addChildren(Arrays.asList(new Node("4"), new Node("5")));
        root.children.get(1).addChildren(Arrays.asList(new Node("6"), new Node("7")));

        byte[] encoded = RLPSerializer.SERIALIZER.serialize(root);
        RLPElement el = RLPElement.readRLPTree(root);
        Node root2 = RLPCodec.decode(encoded, Node.class);
        assert root2.children.get(0).children.get(0).name.equals("4");
        assert root2.children.get(0).children.get(1).name.equals("5");
        assert root2.children.get(1).children.get(0).name.equals("6");
        assert root2.children.get(1).children.get(1).name.equals("7");
        assertArrayEquals(RLPElement.fromEncoded(encoded).getEncoded(), encoded);
    }

    public static class MapEncoderDecoder implements RLPEncoder<Map<String, String>>, RLPDecoder<Map<String, String>> {
        public static MapEncoderDecoder CODEC = new MapEncoderDecoder();

        @Override
        public Map<String, String> decode(RLPElement element) {
            RLPList list = element.asRLPList();
            Map<String, String> map = new HashMap<>(list.size() / 2);
            for (int i = 0; i < list.size(); i += 2) {
                map.put(list.get(i).asRLPItem().asString(), list.get(i + 1).asRLPItem().asString());
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

    public static class MapWrapper {
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

    @Test
    public void testMap() {
        Map<String, String> m = new HashMap<>();
        m.put("a", "1");
        m.put("b", "2");
        byte[] encoded = RLPSerializer.SERIALIZER.serialize(new MapWrapper(m));
        MapWrapper decoded = RLPCodec.decode(encoded, MapWrapper.class);
        assert decoded.map.get("a").equals("1");
        byte[] encoded2 = MapEncoderDecoder.CODEC.encode(m).getEncoded();
        Map<String, String> m2 = MapEncoderDecoder.CODEC.decode(RLPElement.fromEncoded(encoded2));
        assert m2.get("a").equals("1");
    }

    @Test
    public void rlpEncodedLength() throws Exception {
        // Sorry for my length: real world data, and it hurts!
        String rlpBombStr = "f906c83d29b5263f75d4059883dfaee37593f076fe42c254e0a9f247f62eafa00773636a33d5aa4bea5d520361874a8d34091c456a5fdf14fefd536eee06eb68bddba42ae34b81863a1d8a0fdb1167f6e104080e10ded0cd4f0b9f34179ab734135b58671a4a69dd649b10984dc6ad2ce1caebfc116526fe0903396ab89898a56e8384d4aa5061c77281a5d50640a4b6dd6de6b7f7574d0cfef68b67d3b66c0349bc5ea18cccd3904eb8425258e282ddf8b13bde46c99d219dc145056232510665b95760a735aa4166f58e8deefa02bc5923f420940b3b781cd3a8c06653a5f692de0f0080aa8810b44de96b0237f25162a31b96fb3cb0f1e2976ff3c2d69c565b39befb917560aa5d43f3a857ffe4cae451a3a175ccb92f25c9a89788ee6cf9275fd99eaa6fc29e9216ed5b931409e9cd1e8deb0423be72ce447ab8aa4ff39caee8f3a0cfecb62da5423954f2fa6fd14074c1e9dbce3b77ceaf96a0b31ae6c87d40523550158464fcaab748681f775b2ab1e09fff6db8739d4721824654ae3989ae7c35eb1a42021c43c77a040dfef6d91954f5b005c84cd60d7ab535d3f06c10c86bf6390111839a5ffc985a38941d840db64806d4181d42c572b54d90ea7b2fbad0c86550f48cca0d9a3aacfd436e1663836615c0a8e6a5ed1be1f75c7f98b43fd154fbffd9b2316d51691523eeb3fa52746499deddf65a8ccc7d0313370651004e719071947dab6b64a99ccebeabf66418b6265ff5d7ed7ecf6fac620ede69e48946b26205c7add1117952ea8199d4e42d592ed112c1341f4c1d9aeee0a03ac655ad02148cab6613c11c255c99c9eb1ed8609397f4e93a523a31d545055c4fe85baccf2a2903d6b64d2e4b6fb5cbc9029e9eee4a33e3ece51e42602c64a66ee1c6ce9d47911daf7b57c9272b6f96029c797081c1ca3f59f67ed1a0a4a612f86222ab2da812fd09cdcebe3fa2f33c07b9fb8435937091370ecdb028dd9446275eb063ea74149da4b72149187c786ee6839946f85e3d3d41786c2f5aec041f77e278320dc4e0f0618fa0c5e7352dd7a598581531e804e62b3be475d8a7e2cf3a5b05be36f33a9beef75a18a5d11d615e04b595ca649fadd30b8d52392bac5cdce0f2a524576ac155ffa05ba73d255a066d8feff1d105f29e0925" +
                "3d6b2f3fc02fcb4d0c604b8dadb2ee95d36b77320071fe2d6105c9bc7a940dcfc2b5e8571a8678f8e81c473698411ae217051d5dce243a10bd1494539f2bdbb4898a5b8655f9ae9a99b622cec42bdf87db58673461524c27754503ace9d0a90684ec165da55247cab70af1dc928cb134f2d4d1a876a7cf318c77feb14148419526ca63c152cd4e43f9f7138d6fe44c3c104c15e6d904abe262cea244e59bdde57bdbb2b04fc2baa65b01a3af31e0e8e65b9991a43faadcee218412c834dd2415de3cb4fbf0e549a97d0172a595b367e80a1dae0a233780788784f214d2fb15d79f71ce464fb5e4ad664114802069e6fe26e8f1440e30815dbf5f4b20676b95ec5bb23df3074ffa4bc53f375a4622acb4756c102cba940bcdba53cd944aa8dc18a12237ac2c8cbae62a6f7fb7cec617b7f65280ff9ef26c9e146ed2b7d22f28883e02a05378cc031f722185e423fc66ad150771fb29a1867a978190aa6bb556e6c15009b644a06622e6606a0cf00e9a2fe25a83f99e802eca63e05aa46a5c7f090d63a25b6344ec70c798e142573bfa56980eba687bd52423e2c04ed4a9a5b6b7419c5abcc178348059ec6ba9bb47a22285f67115193c3c98288517e4ce30e32d971c390169294f3bc22f9d5b534022875c77620b0fdd85fd65dead59537ebfe5b09d4442c45f75d9188f24eef00af41faa7072a984f988d47af751d2c933aea51855572cfdf197eac950759cc36d9be3d390c357d778d770f03801153442d80a98f8ed151c6f4de26f746b714ce829b02aab07fbeae1890f91a1d7e79ea253027ad443b5f0272782ffccfabda5e09c9ee4d238dd00553ebda0151af0811c009e97f1a2aef3fce30bfb66f8c8996cf10eedcf7735bb7686149a5459b07086db3e950828a55b84e371cb9e9bd2b17111a6fcc9612b77859c52f630983c483822832375c3a82aa918c7347a443a86fb2b27dd6daae0d1c35e759670f8108225187d5376633881179b0860d8b08de2019db85cdd39420748fc31695dd66f90e1c6d3b176b5f8b6ea35900e7d56ebf6485c241f2b7ebe4a0f253556d647f1a4e3a8f4e6aa21131253821df415694467d8ac6d20282fe93be23c6177e68d3f7860a75a238f741d5d589635190e12a4821eb8b4ca87e3e04ec33e760abaa5" +
                "ece16bded3f35b62d5a2675be82f7c4d9d5950a64ba5f74f71b150c71902fc6306244c55aa1482940e34dae3715ed655ab0eb1d806a15c39becfba6ad3e815ea9af9ecb8f88088cc31613ecb473844d8d249bacbfdfd30d20b4fe2f17ae9cbe54deba7c90cb6fe8cc3cfe0b1af675ec863d531d526773b8b3c830604d9469b03b9471e96ea20e2c07e747707e13719c1bf470c52470b946ea2aab55d69759c07adab0aebcecf49e1aee6524b9e89d56cf7ce3f49309ed8cfe3357e4da045fefeb6c38855c04d7f32f732177e3079430cdb19841692cbdd1ee639de071b44fe7332bf2257484410c3f2cb0a0d7e9ef63c9b1d7168ecd7675ea6f6744dd2f53e954f0e252903ad8038fdd488421d72748c804b2cb38b3d3b81a4ad883ec967b56115750aa079f0d17b7a8e584b4b39071ef0a5cfe74508793d7b5442e85b1a54aa5aaa290c8cd0c016049f56cf86b6e306a92c74cbf2f1f199e85ffaefadda74c1fb8ad1113451db02809eb8eb5e55eafe8cafaaa6af8f2a1504908648ab7df5dbeaac321c94829b80170ee823895c025ef0640b20df185d2be7c9cc62e9ee1f1ebeb39e1018238c97aa77522748492cca4e1a1ba9f25419a9d22b2ff88d3a57c0388446c35e43977553957630295b8879a09e9d4173040a2f9f8cbd747f54f84d23a50e50c820988719a4513f72f0f55a0facba02a6598d77843b988214409fa9bccf62b04ad5258723ab44147ddd9b9313aece089441e74bc16b9386059a8d25cf669bce89dd6cfaf656cc57f391e13d840471aede7b9a25edc4c457dcb7b9258d48ec34ac66c7d6a34c9a35504a072c93377b6059c95b523596c64fbdd89d1a54a56325557b6da120032d901e5b3a77b7839d48d8ae27eaac510ed593f36930d123d60ef0449ce9d2948006a6c10a86a2d1e4afce6b8a8150579cf45551eb40084bfa770f1f7d64963330d00d919f9412579a23258be909e40cd2046f7016d9d5469fd725cc85c08653295a3e92033ecd5e779d02779f3bd2dc17996ccde59c2e3b5cf90816ec25b8dc9ef265b193e37263a76286ff8f5e80f786d6e28093eb148d1dea41eb0b4efa587d24464d12e3364c93099b67edda466c7f1ed7f14dc2fc6dd25be918935fba1b5f342a27b825cbc7432c1a28f87dd3e4929" +
                "09c4de72b3fb415144fc866324e1986212c19952ee25f3cfa7508c1a3573b8a574fcb32d3d9039559e58402f4a031bc1c90149e7bb9f44a12f9d197957bdd485cf36d8c1fefeb89d07c0539d294cb972c8b064f6f9945542818f893cfe49c4702a3ac974a5fcca3052926530f6969b1964db848ffeda89697e3a94b75bd67a9854caf829fd89a27ff111f961f00adb3057bfd62b557befd52fd73e5b38f8989d1459232370f492faf800ef32fa33ce85af30189f09e99678cb64036c77ef38d07129892d31f618bc6d730de263e9fe830a206b45c302e18f196c18bc3813b2d02ddcab1e59264f8032efbb0e4e6668bdce2dbbe950edc8fc1939bb087497d73ff86f08a010649f443dc29ae61c5d30a957b5b756dcad08a155644c9154db0c9ccfa8b049fc824e85d8e048dca99d8830a0de961e7284f1326d4ae489621158c05fc684652c1bb01eec2f45185dfaa070370988f23d0d56b6b57205cc7aeb9d9f0ae639ca477d98f5abed88b7d7bb2d5c2d87c364436f3b41f63678189bf7e6e8569e39c8e5153330ea61720fd455f81f73a13f5063243fd499aee902dcc7daae3c62180fdde0ed06e8f050efd98fdf139931aac0a41098394122a7e55ff2a4adc9012a0e608c1964b752b8271b36c2a0b536c3088c4bc335987eb4b0aa5b785f7164f2e149db84abda8c2a86dd1dd8ebfbc998554fb11c3812c6853f050133bdde0bbb0053f5184ed7f6f4fd8fb53090b9d7a1366f37e6c64c13a8436b9a49e05952b5d7ed4d5f2197a683e77620087822bda700d823ee5fcc50a0886f056e3e14a9387b450b3b036ff559c7e00592f20a4998818cc64874100292580dad40f6f2b4274683f5f97dd145d86ea41c83a66b156337b0913c791e3380d11bb72b99f2b5668c4c21f97cdd80a890e4f94452c89e3b90de7e340646c984a80858b8d67c7adbf52a8dbb8441318acc878972d7499d9876678ca92d9689fba358aee8adeb9f3a9c007ec8022a0877419b1a7bca032a4e613e02e3f2db3ed5ceb32e7ec221decbc069802d8605ed6ee15974e7027053a4270fbc8bc27218b74b3c748a57c66083d5f86c11c5299d1bc4e361de4a84427c8bc7e0edb77acc843465738bffd9fa498ce4b1700ad463baefe0794f46494ccbea58e6b110ee996d4" +
                "e8d8d31700a51c075c4e5fbe316d897a9b05d3d7ac2827c38c65b5719f47e7b00ff29693abc71dcb06135c66693695cfda3a93b3be0b4022218c4c258ae3c5ca9a0664812dda00b283b7d4dbfd8595bc5e7ea43b3b5166be838b5fb2c25456d96a597bc3fcbf17fc144cf8ead03300b5c17746e968ce28a3a51d8e30585d79f944ace9b5fcf8d63a40aff7930d706fb00148b643fc2100b6df658402cb86b1974a87d783d3c1ae6fc1f826a93721bd4d97afb884872a52886d010c285e8b0aa8b03a4356e10adb6c6e7da5b6c6b9990a9a25a5ab66cf025b57f7802f9dbd017dea3ebc9211bd31aaadcb3799c7eaccebbc563a4fdf3a52d8267f79aac839511091a96658ad5407cbccfdbeede199a5aa5436f107fd353535565f83f01d99791a818d4fadf7043e7cead86013c32ecec835fe01d2684620ad1552d457905ec23ab33afe1db4f5c7d1286b1fad134c15581b601ebefe0153941d357e4f908c19acebea15d987b926317a36f430f0b7e3b0916945ac5cde2d82832b637eea29882e2335a197a86d41ac5e04268153cce0274a277c4d2583ed8fd363f0af7a47b00c1cca73c5d22993f94292783965cd45aeb9f5bf8326c5b72c240cddd25ca6fd23ab890fa184c9cfdf91f53c572492840f0875807c342f97a19bc37dc6135b5bfc94a6cdc4f470f44997a017a08a8651a10219ce1b38ec84faa7120a5bd062d1a09f4e4c1ac272787c13913c301d965c0ff785d5a3d76a36f340aecfe0a8c4fedc5a7a8e8b7521baa3f44a6e8f039badc4fc5e4cb8089a1b9f0f869ba28e8ab06d1c380f6dde280e003419c16130cb92a2874bf656568bd5d33006c0ea7d83ef02bc1289a2dc0aaed3eadc62203ad78b4441fd1dc3f8e9c5a2f5158d57585e922aa1974bb31ecb463febbe16a599b85ba58b3bb655a90076bfed63b7a830d94da5b0d4eccf43f9a780edbe8cd0b29dbb5fb664d1a4dca0711be820263e77eac37816a008538c53afa4da2550706b7d209ccad725638c1f0506b09d6fa4fa6933eb05753daeda1784619e8d6eb14f40e9a78e5e39344c6848e5f89972b5fed1a8adf6da9ea79865d05697e3c5549ec22f8232a8be10db284fa7255bf9162e6f558e1185cb1c49315dfcba2e91a64b66f0a535848719f9a52e7ce90194a" +
                "f802116d986d1cd6ad1cfccacf306a7aeb938bb557912fad996fdef76093f2be3bc866d9f7eeb9b73ef1f74d87de604b5660bbd2cfa5639e60efef2bff66ff5d8767849f9b4fd4eadd033d79fc15280640c77e2db6842d4d72a1e1fb6b4967fef5af8ada22f76f9daaf229996e53346917f24d91a3bc6e74fb2b984dac851d0bd3f91e8aa5bc5591c2dd37169675419da1f52fb03cc15b865a7a665ea69d67a07ff45cdda28fc6163284956e24e7ca343203d34a0657c5fda3c1abb4edd133e62380cf2f0604956502994eadc9ff5757712ecce133e79dca29623713c860befdc951df3d773dadd9c29eee4cc8c846b6315af9adcb29180e895dce80ace196c5a9f2bc8bdac1d89dffbdf1112a9d4227a61d9e56f7bf9840ed60eabdec8bcf4289b4a45b723a1aa3d8e0b9cccec4e24ded63ecd47ba4f54bb0eca4ed3a39a0e2e0a34edde4ac0fdebaf3f7539214e1cafbd5ef05f78ffadde765e339c716493f9c54368e23644dd55a48f78d1485f89e148c7ddeba4da9f6051919c17a1fd23d5eb17cd01083c3b01f2c54baaa577545a2707a1d5054ffe8c5f99b129a6cbe6c2d2f03d899d18ced4652360c55de7dddd2c0bb5b7dfbbc76cb6388cd797dde1a358859173e03ab1813d036689000236d7fb283378690502327e0bee1bc42154c4defda8d720d41240d2f544675a95b1e97d73a33bc4b77fd77411b4e29fdc1db1386ed23cff560be246a650d67b18186cff41840add42180ae5839f3611f2d3c9495436f89a284f9e919587d2e683d4293026a7c8a61463f8636ba8e8df590f5e3124fc676f62d7c82efda7289c07a9ed2d01215faa2c7aa946c536c83c270af271390bef9aa817d5ff6e8957b5439ccb6c4358d1ede77e313eb1a09422f7e08397c110ca736d03071765ab34acec0d6934c2a9c5646f7fcdd26071533b56ac40400ebd779a35cdbd9a040acc9255f2b51a0eb451f3ef48d3244c6b7308f085ccca25036cb9ab4286927289b7e889f3a6fd2676cfd7db57a4efddbe27d2306ff5475caaf5428662cff339d10710f9222547a102c7292fd884956389cf3f7deaf58f4b0708d94582716ffff919a8c4fcceaad206644a9a5205aab17c96791c9babcfde23c1c585692b77082cb64c43465655777f381507b8c001f4d3b" +
                "58cb054034bf168b7b339394fed8480c07548793d554b2e0ad8eea9a7ed9aab44099037c38f9d6cff2c0c193812528549712141390a5b76c3d685c3915b0283b437bdcfe2cc1d8a54671096c3746a63d88c4f91bbc908d9f53d308217b215a3c2067518d6b41b26d7f266efeecd3f5ad2a29ba0c2449176c4d7977d34ac6ab116c92b12664c7796c276929a247ab74f0c483e569340c1a039480f60c5be3834c8561876baeed5c0014a98b3be7e8e88c78c871c4dff690547a11951f48ea4a5cc71e587c7d390ce056b603d81c7c9ae77086b6a7752eb4bc71cb8f69fd2a99bc88204a0dded3f27cf66ef20f9b8ec5e7c95e8aef35a45d65ff87bc0e1b00c13f9d59ecdb784be53ead535d9439660d69a3a04b9f86193bff4f8c4fb6445e3e9b53f6705a8f0b452fb282e391e5c9699e27058de811b740055cdd9edfa95027e2793845ce8770256d52faf9ec66daffafa4b20c9ba99d4553b6dd9747002d319fdce446dfd8f74318bbc13140c13e19969c3581345df20eefd13beff252e52f506060c7d7df041d766d25e62fcf955555e1824519981cfdae25b32d8d7982a5cfc06e934a28eafa36d2484e878200c208c996adafc7b0ac2b729e483cbbf1414d2b944f7ae5c7205ff1091ef1761bf3686bfd82ce793bb49f7caa720ff48574a65e44b06370131546a2a31c3037655e9fe82312507655da910d1b300f3bdfb21eb7e941dfea0632db82541cecc20e6051db1ac67f874397afece22019ea7d0f9a5ee531524a28c9d58d8c3dbfef69959653c18d67e5ba6c96a197a88bfc10a3bfe27ed52181a885f1542e5928c7a860a6b3ef7e9fe3442588f3b5c3162fd1db84f03014f01d97adbfe32e4cf321bde42e9fc5c4ae3ad0547199e489dfe6fd1210b9c2aeb5cff545e3e4df835873af24df162e59fade2852c7093d53069bffefccc5a4193f0d8c75c6d2251e0cbf5beb30101a3e1928e4f8bf071774ca596241101521eb0099efb24ffaaf2f4f3c770022733d02a0ac2cf0a388052be1de2feb7f34e2ae50b7adeaa13de36ed49291e5e58275062f37d63aadaa9dcb1f2bc4a8885b503937385c6d6f2ba12548aba8187599543ebaf03d80620539d01f246d3aa7dd0ee09c7991818e4cbb852d0ee38faf5de5c4eb93208da8683167a" +
                "70db8d75da696009f512cc73c045ec8c83f52c30d43f70d6ab67bb0df52b9740713cc86400178f2676fa13b5e98a1434f4059efa8b40934407dca1f0c8e5b4917f9bdd3ef43d9ef5fea0064def162aecf5487a6a30700b7bf7463d30b889c79ff0b61fc8d4b3e83bb796fa26f68702dcf3b2fc9e68af55aeab57122bfa5120bff6da079ae588887825d03218eb61fee4bc341ddaf625f38df0d40b9484b3b57358c4f22a420f495df665049f452f834c14ad955d01d1275182ce9b00e8d6e2609d53a69b24afdfefe744d740f4c421feebd27ffe2d623e3b0fe773416080b8eb9e3f0555eaa233784e65ae1bfd6be5131e70e69d74b82ac7edb24883f7ecf61e202f8481003adff3ceef8f51ed588abf3a933d3e8c00965be4a838a16a7af178ed52db1d8b0ef672e41b7fe5314a840f6e6c959ced6076896ee108b129c6ec335fca5a6735b00bf3ecd7e075b0fbad86b03df6b36d5814ffcd0fc036ef51e541e5e24003d235454c04e36bce4efd3bc3e1e61ebb64ae968e30235c25a203647d7787afbfe08e02e07c807c8dc5a3ef4a762d4bafc5fe160de0773a3f36b79f3e770c8d5b49629e54e04d70dce38e7085376c8eeec5ec1f4ce17b8f6e1641c2a8e3ad3a41b870bd2cd6cc3d31955b5ef11826a87de34c6ff46f197c959f76872a0d0caefc01d05674e25660d2ebb0ab6002dd723c3c7e942550202d5ee70362ecd6ca3c6deb2687afdac4fc00ec86006be7263bde79e0bbcaefbb7258cbc48929e6f03292e730b666eb330173e66a659956a96393c2227ca5e77f53dcb752c768d1cac1f999a95e179c380fdfe6bfd78d7eeb19180472eb1bcbd020f88389842919ef7e8fdf881f92c3414f38d6399b112f8dbdd419756ba25d46f9e4fb155fce2fa5499cb5ff8b9e18615a4e3ee69eab740fde21413c3900a147d93f1b1a4fc182fb367a6e1c3d76257f49240f2ca7161a7d8b199682a292473455c8ef0deedf2dbfbc0d1d8ac26e5dc70cda86ea5d32efeec39b4de771c51c9bc8071c5f4afc5b4da02f423288aab4d7ed241f7105515a40072da7269a185b23cd2b45fe94561ea9f79422f789d55ba502aff3c8347a8ebebbfa1e36a855b483983d0c9ecb1ec56a80fbff8071d210b8154e7762958900e6210e9c03f5485f8acd4" +
                "0104ea7058ec2b9fc9a04d7312aa1be1763490fcc13b4ad5fe0986e5b197bb13b19f23afd13337f6a131b3c721c3a7931192a6d63fcf405eb91cc8da0132e570224d3e3961da91ca9f9ead72123d31f4612878c1904bdde965189cbd29f259acc29c8dbe21249bd3975be4160bfea9892b9d8488aa468be1f14a387e8813fdb31e95e37304c26b7b34d9487dd176d8f39d0256dff8f4f9ccf4781e1b0258a23cc177db203b936ff8ef6195ffe7a415d75dad20de2168e3a23ac154aabf2563ceaaba66ec094fd3a6e164fab210b0a9516a8bbb529b616d33e053429b91665b5f8572c13bfb268818564f7d65a0054d93a0936ede77e44adb6b6c5648c9f1283c1bf0b4f03083468866dc93241587c2f2bc5dc85023eb203d90a31008b314d46ce66cfaf08f862581807962d8e0dad923552c912d8970bef5d0d7c2ec22cf0fe560ce0e4d051fe5d49c5a840e87dbfb0bfcd1c4c4b0c462a6ba5d484387e8152e56eee062509f1a7f10faaf22b071b6bca71dc1b0b68a4c6db7197012f0fb4635e3388b0c70a4817fa02099c33f9b61ea30f31920bd6eea600c6f134c36da37c31818715fb0f799ece7ebe3fe224c6fd60b69c39eebd7c52f10ec597530e326ed10929a09afb2f181b47349f5a0efe72934134be837a6b88a8076478dd6fb09f3100c681e030782edc037738e70352bd2fecafc1c903ebe6db946b3ee557439e5bfe93714756594e1b03f483f1c1f45ee28c0b6c14a6c1ed4559528e7f033c0ce3c85b687492c9cc27ed9d89eb8bef94ad05cbc248051ddd80b82dc0a7d1bf9f31bb57963605f54bb0b7bb387267b896c32b63da9830e52b40055d2b8e30bad892542d0cdaee632264c7fceb6405618900deb789ebabf34488708a2f051a1244f7048be752ade19487252f37995850e08cd70e70f70481a6497494780aac0429728648618dd8f469760499fec7dbec04029852cc20a44843d42999b1f1078645227f5601425cb26ea627a842f036b9340a07bc4167275df84871f0a81783f362d4f588242807cabb8d1764fccf10c1652c9d650606a08e58c607d6cdc2790439413f5e6a3214c9394b62aae2e707e93230ff0837887c74165216454fccd225597f07e02e02914241ce625b6a316d85ce12ae08297939bb4cd544dc8c" +
                "3898aeecaf5aec056e26e8d78339c3ae9849cd16f76221d10efcce44031ac7bf9b394cbcd5e82ed20eb95f69d3d29753eadde982d9388522195329dac553b265a480167de3199fb7f738b05630beae3940d47505d2959d837eb77fee2638f8c1a877dcfb9e8bda427eccd2570a32b56d503b367f28181ad4efe96fa4bdbeec520a781894cd3adf0a112693095ddeaa28c7d13e9204c8ae1a5d3a082abe9718b6328972f0f2b2777fbe102398be3a373a99049b9ceb610856a70c8c3ce52e0be5453a4a5b27cdd971985735e97be51824efa45193e83d0746206c72579c1e49069a6647f27d8824e3a93d4a1d36e7adb85c3dde394abcf8dfcdfd3e9a713d7925f599f60ca92a60efd5c6748f7cec39f2093cbdf4df3b7a7ed7c9b536c1b4c6967f0da287393faf77f95f6130d89adadf92313d5888600798993e97acef5ea9cfd1bb2c516a13be76fcab70b7d23123abef3219fb1708e889bf128a4e3bf35e1ce4313bb81390cf0ec651a21c26141f53391497a373309247594c659ed2a5a577be5eed674c55f82d9630b5071a24b12dc271439ef441de5cbb7131c92e30c2865609ba8e439bb7ffb1854ebccf4cceb0893395da1a9ed7345d1c34bc4c26913a075ba782e9328367367a1fa950bdba7c77f92f12b4418b484732ae3dc2ba23835c1eccca73f14ec90bb60f273232ff1d11a1b976a9e9abbe0b44728b97bea3243e359efc148de860c1b71158815f2d5211304857e935d9886edf84df1125773b3706b095cf691a62fa35640955a88ac0d85b2825916fc1d513053e02ef34f567671cf3b2aebe360acefbc5439d948b1afd37b384cbc5a470616adc90f38ec6435e5c8539743666c3f51881784f4136cdadd7346ec51c844bc5299ee7a784685567f15be47eec7c927eef34903028013d4f39952cf358cf2f5cf74e56663bdc9ba42b665dbce45238ac6bee123b3e4a3fa5df678ec62adc11d456ed6705b186cb8ab0b0f5adca44ce4f2af6115b4163c6e15d5869ed3096ff29477e48282dfd8629a1abf1a14e9b58663e2788b44b72c4602cd3f31b6ef4579e254134781826eded5b486fb12f18283c5aefc5597b926bc37aa68bcf837022c32f537080b4fa863589fb699dc3af46118cb383f82161ec44cd739294db53e2555c6325" +
                "4da527c06020860b89638b7629398329e4a807c12e6f8fd8b820d084f76a429f925c668ca11658a0bbd02cd3bf206bc8fd1d9d65fbb6dde960ba47e29f9846cd0c238e301b97180af62bf92a3d2e9228fb0e454c4f228e5855041aca44f67c220077dcf9c76185d92f227e2a5556729b5a789f7509a07926661af8b27e256f2530ee8ba666ba62091f8e62ad90ec1d0c1c243915934a258997452fa685e0936104f6a436262446b1f01782e829fe0233208be23e2a3cbac7d9271ebc25af406051f423fd0638f387c0a7ef177226d31e5ce2303c49c07739614e1c3186808f491c265e193f2ffb80be13d68b6880ab00775a79209f265549bc298c47b0d7a18a1e991c6d46c901f285592521a18f8b71ced15c7d9cd1429ce7cc76a542588fd5f2982aeb31623ef952c7d25dfcca1a6dfa589d6682d34cb909b84d1e416b6488ea90e9e2ba58424ce0563244303b3724d6fcc55185512e96865b72d3aa30555c55f461ae6872f04e0f9154e3135ae2c360db83d54defecd603d45bf66ddfbb75745c7d71c15e86d10da758fe4816fb2340b58f41635f8ea4434a20d97c12477794553dd898607521b85fb51cf92b4916d7881348510f3fc790ac6387355f790beaabc031d97aa541f5bf74ba76e4e16e7f5e90f4683b96d7926810c02eff1ac90331a43d015ebbc2e739a400fbce42c2a40404b82d0fddaf63519d4ee3fd5054bbf8c77e877bc0078c6c63480b519e94a23ad5faa0462d9c698dc4ed589126868d519559b3e78c8b27eb31054c6a62ac368acd440951195ef38b4815eed6a27192aea4dbae3edc027350b093b80c971b74afd7883a64052473a68566f553b5d1c8c54151b2c7ae79db207dc2ec5645520153c28b65d3018c0fe582ad37dd1fbd8b821d4fc05300baac9cdf78da34b8081b5a1739c416ae5cd65210404daee2b78cc4e4bd5c0274c19690ba74e955692a4bbb4c3bfa1bf787dc6799e53eaed6dbbe55790f148e5b32124db6e74c50ae010e766280bb5484197c83249a6d42ccaeb5bedbffab8e1c89bc5763cc530c10fd092ed9391ffc853120c251602756c69f474062932dfe7904da46ebde76dc246c6149c4177d839f16d0522fa2e7a10398a333bbc709495782083af00633b53139557b0d1fcded46fd7c497721" +
                "f105790deb20c0bf0e4933556ed8cfbd5048994030810319b4f441e0fdc05a7d81f424170448cfc798a49fdaf5ca7e612080006962c7e526e048ff4ffd5ccc62ce25e2c6628ac2d9054144c8084b82674a4c662ccfdd2c5b0633f936c8b97b3e764b016a07c9f12d1e31bfd6d454921cdee91fca774b48c23948aa10cbd4e1c96a8025ca68e0811d064b628f5a0f7b080707f7753bae8dd0db46e3bafa4abe96165db416c7eae1cdd873a7852344bb6c914a0f2d0564577840d61c4cae084626d147f764594a237ae92d48ae551bbf11b0d2744ca4c5660b954a71ae6aa4d445e52e9b174115cc192d86d4caba4dcc966f26610a148cc971c8fe827056690fc61e4d409d4624f7b56d48cc02d7a902b30b773089194c254c30922f996d935204e582e6335d52ccf9df4c464c062d6087da48e92d9816c3304fc14de9b84f9a4abef183639369d7c2c873c66a1d0dcb9e6e4fb90fd0f7d22d29bbeedd9a3e76f0a1bf69503a4f09238f99d47301436bf12004921fdea2b8193c4e80413b48af266f9757e202291663286531d077418baf0bb7b69247bc032605dec2c9228bd61aa94181c52ffae1a42cb2feaf2c4474f05d07b4041043a1bebdd97a0dc42e644fb0f1ab2bb3850254154336fcf68c50661f3f9fa2a42038d51ae52fc898435513ed994654704ab8e925f867f32a40a351d642cbe40844b1b51e99d3858f8bfe387bdaba611f45a880926b77bcea7f2478c28fbb7a718fae9f72455347536b294b292a029a1eea4005517a8ec20e457f88f1bc7aa03c1bcffbbd95e69dd76077ad2c448a78b0207cfa8ef236a34626d4826af6d42d0a442246e51d8f34953e871fd04266218c3ce3140e444f8abd2922bd27e614a6f10609f5bfdab37e4c446159d41b8fe6732f13894123ee6d7ac3cb9e6d46ab0e0a377d932d1843018bd10112ed8f3a676046a23b0f2d44414fead4ac564118fbbebba4147a464d533eee347aee7e5e48998d51e242b362f73e465c10c4b38135ad531971db15ee5abc4c503c7a7d87edbb9f438acebcb228ac32d66b89c63d029b7dd46ac3d40d6fcc32304c90fae285c8d03256867d569877271b52a00395b42eb295a137326481917cc395e97303af4a85e2650c97f34a6a8569fc327cef2070d9869ac513827c" +
                "8683586aa2aa1122ea7ce939abb9f2f45869311dd8346864e81d1f15137c1b97dc8c137c40b5d428cb9567ed10cf0a9a5c7c65854524dba6a3623d3e26fe6ca69cdb5a20c07fb2e68228963d4d21970423b9fc43c0b9ccaa4e7e96efbf0bc5c31d0b504374f9645594eda6421f7ecc08880909f985ada2f524f6b9f9454ae4476d7edbfda7fcc993ef1edd55d65a90f02edfb8e0fe712cb06926a1e30894573b1009220292a2f312227149b322e0d4efd7cd9e21831cae7b334c6d75ca7175453ffb7e31c52e495f82dcce71f5e08026c470252073c84d266394c8eb7dcffd9493e4d3532b70697724ec5072768c78f7adbd58c9c6b266f3ab73bd30133d8fa9a6fb7eef9e64f631fad9d38aeb737b5042da8363bca07caf67fd2a63a79c8c328e725b788702c5a3908f4fc393f0f7ec5b5f3faafe";

        byte[] rlpBomb = HexBytes.decode(rlpBombStr);
        boolean isProtected = false;
        try {
            RLPList list = RLPElement.fromEncoded(rlpBomb).asRLPList();
        } catch (Throwable cause) {
            // decode2 is protected!
            while (cause != null) {
                if (cause.getMessage().contains("encoding") && cause.getMessage().contains("invalid"))
                    isProtected = true;
                cause = cause.getCause(); // To the next level
            }
        }
        assert isProtected;

        isProtected = false;
        try {
            RLPList list = RLPElement.fromEncoded(rlpBomb).asRLPList();
        } catch (Throwable cause) {
            // decode is protected now too!
            // decode2 is protected!
            while (cause != null) {
                if (cause.getMessage().contains("encoding") && cause.getMessage().contains("invalid"))
                    isProtected = true;
                cause = cause.getCause(); // To the next level
            }
        }
        assert isProtected;
    }

    @Test
    public void testNestedListsBomb() {
        byte[] data = {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xc4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xd1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xc4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x0a, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xc4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0x0a, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xc4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x0a, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x21,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xc4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x0a, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xc4, (byte) 0xd4, (byte) 0xd4, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xc4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xd1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1,
                (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xc1, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x0a, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed,
                (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0xed, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xc4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x0a, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4,
                (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0xd4, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0xd4,
                (byte) 0xd4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };

        boolean tooBigNestedFired = false;

        try {
            RLPElement el = RLPElement.fromEncoded(data);
            assert el != null;
        } catch (RuntimeException ex) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause.getMessage().contains("invalid") && cause.getMessage().contains("encoding"))
                    tooBigNestedFired = true;
                cause = cause.getCause(); // To the next level
            }
        }

        assert tooBigNestedFired;
    }

    @Test(expected = Exception.class)
    public void testEncodeFail() {
        RLPElement.readRLPTree(new Foo());
    }

    static class Foo {
        public String a;
        public long b;
    }

    @Test
    public void testListCache() {
        byte[] encoded = RLPList.of(RLPItem.fromInt(1), RLPItem.fromInt(2)).getEncoded();
        RLPList list = RLPElement.fromEncoded(encoded).asRLPList();
        byte[] encoded2 = list.getEncoded();
        list.setEncoded(null);
        byte[] encoded3 = list.getEncoded();
        assert Arrays.equals(encoded, encoded2);
        assert Arrays.equals(encoded2, encoded3);
    }

    @Test
    public void testItemCache() {
        byte[] encoded = RLPItem.fromString("hello world").getEncoded();
        RLPItem item = RLPElement.fromEncoded(encoded).asRLPItem();
        byte[] encoded2 = item.getEncoded();
        item.setEncoded(null);
        byte[] encoded3 = item.getEncoded();
        assertArrayEquals(encoded, encoded2);
        assertArrayEquals(encoded2, encoded3);
    }

    @Test
//    ethereumJ: encode 320000 bytes in 127 ms
//    ethereumJ: decode 320000 bytes in 159 ms
//    our: encode 320000 bytes in 109 ms
//    our: decode 320000 bytes in 4 ms
    public void testBomb() {
        int n = 1000000;
        RLPList list = RLPList.createEmpty(n);
        byte[] bytes = new byte[]{1};
        for (int i = 0; i < n; i++) {
            list.add(RLPItem.fromBytes(bytes));
        }
        long start = System.currentTimeMillis();
        byte[] encoded = list.getEncoded();
        long end = System.currentTimeMillis();
        System.out.println("encode " + (n) + " bytes in " + (end - start) + " ms");

        start = System.currentTimeMillis();
        RLPElement decoded = RLPElement.fromEncoded(encoded, false);
        end = System.currentTimeMillis();
        System.out.println("decode " + (n) + " bytes in " + (end - start) + " ms");
    }

    private static class Nested {
        @RLP
        private List<List<List<String>>> nested;

        private String hello;

        public Nested() {
        }
    }

    private static class NoNested {
        @RLP
        private List<String> nested;

        public NoNested() {
        }
    }

    @Test
    public void testDecode2() {
        NoNested nested = new NoNested();
        nested.nested = new ArrayList<>();

        nested.nested.addAll(Arrays.asList("aaa", "bbb"));
        byte[] encoded = RLPSerializer.SERIALIZER.serialize(nested);
        NoNested noNested = RLPCodec.decode(encoded, NoNested.class);
        assert noNested.nested.get(0).equals("aaa");
        assert noNested.nested.get(1).equals("bbb");
    }

    @Test
    public void testDecode3() {
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

    @Test
    public void testNestedString() {
        RLPList li1 = RLPList.of(RLPItem.fromString("aa"), RLPItem.fromString("bbb"));
        RLPList li2 = RLPList.of(RLPItem.fromString("aa"), RLPItem.fromString("bbb"));
        byte[] encoded = RLPList.of(li1, li2).getEncoded();
        String[][] strs = RLPCodec.decode(encoded, String[][].class);
        assert strs[0][0].equals("aa");
        assert strs[0][1].equals("bbb");
        assert strs[1][0].equals("aa");
        assert strs[1][1].equals("bbb");
    }

    @Test
    public void testBoolean() {
        assert !RLPCodec.decode(NULL, Boolean.class);
        assert RLPCodec.decode(RLPItem.fromBoolean(true), Boolean.class);
        List<RLPItem> elements = Stream.of(1, 1, 1).map(RLPItem::fromInt).collect(Collectors.toList());
        RLPList list = RLPList.fromElements(elements);
        for (boolean b : RLPCodec.decode(
                list, boolean[].class
        ))
            assert b;
    }

    @Test(expected = RuntimeException.class)
    public void testBooleanFailed() {
        RLPCodec.decode(RLPItem.fromInt(2), Boolean.class);
    }

    @Test(expected = RuntimeException.class)
    public void testBooleanFailed2() {
        List<RLPItem> elements = Stream.of(1, 2, 3).map(RLPItem::fromInt).collect(Collectors.toList());
        RLPList list = RLPList.fromElements(elements);
        RLPCodec.decode(
                list, boolean[].class
        );
    }

    @Test
    public void test() {
        assert !NULL.as(boolean.class);
        assert ONE.as(boolean.class);
        assert NULL.asBigInteger().compareTo(BigInteger.ZERO) == 0;
        assert RLPItem.fromBoolean(true) == ONE;
        assert RLPItem.fromLong(1) == ONE;
        assert RLPItem.fromInt(0) == NULL;
        assert !NULL.isRLPList();
        assert NULL.isRLPItem();
        assert (NULL.getEncoded()[0] & 0xff) == RLPConstants.OFFSET_SHORT_ITEM;
        assert RLPItem.fromInt(2).asBigInteger().compareTo(BigInteger.valueOf(2)) == 0;
    }

    @Test(expected = RuntimeException.class)
    public void test2() {
        NULL.asRLPList();
    }

    @Test
    public void testLazyParse() throws Exception {
        String expected = "c88363617483646f67";

        RLPElement el = RLPElement.fromEncoded(Hex.decodeHex(expected)).asRLPList();
        assert el.asRLPList().stream().allMatch(x -> x instanceof LazyElement);
        el.get(0).asString();
    }

    @Test(expected = RuntimeException.class)
    public void testByteOverFlow() {
        RLPItem.fromLong(0xffL + 1).asByte();
    }

    @Test(expected = RuntimeException.class)
    public void testShortOverFlow() {
        RLPItem.fromLong(0xffffL + 1).asByte();
    }

    @Test(expected = RuntimeException.class)
    public void testIntOverFlow() {
        RLPItem.fromLong(0xffffffffL + 1).asByte();
    }

    @Test(expected = RuntimeException.class)
    public void testItemAsList1() {
        NULL.get(0);
    }

    @Test(expected = RuntimeException.class)
    public void testItemAsList2() {
        NULL.add(NULL);
    }

    @Test(expected = RuntimeException.class)
    public void testItemAsList3() {
        NULL.set(0, NULL);
    }

    @Test(expected = RuntimeException.class)
    public void testItemAsList4() {
        NULL.size();
    }

    @Test
    public void testAsByteSuccess() {
        assert RLPItem.fromLong(0xffL).asByte() == (byte) 0xff;
    }

    @Test
    public void testInstanceOf() {
        ArrayList<Object> li = new ArrayList<>();
        assert li instanceof Collection;
    }

    private static class SetWrapper0 {
        @RLP
        private Set<String> set = new HashSet<>();
    }

    private static class StringComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o1.length() - o2.length();
        }
    }

    private static class SetWrapper1 {
        @RLP
        @RLPEncoding(keyOrdering = StringComparator.class)
        Set<String> set = new HashSet<>();
    }


    @Test
    public void testEncodeSetSuccess() {
        SetWrapper1 w1 = new SetWrapper1();
        List<String> strings = Arrays.asList("1", "22", "333", "4444", "55555");
        w1.set.addAll(strings);
        int i = 0;
        boolean hasSorted = true;
        for (String s : w1.set) {
            if (!s.equals(strings.get(i))) {
                hasSorted = false;
                break;
            }
            i++;
        }
        assert !hasSorted;
        RLPElement el = RLPElement.readRLPTree(w1);
        for (int j = 0; j < strings.size(); j++) {
            assert el.get(0).get(j).asString().equals(strings.get(j));
        }
    }

    public static class Con {
        public List<Map<String, Set<HashMap<String, String>>>> sss;
        public Optional<String> ccc;
        public String vvv;
        public List li;
    }

    @Test
    public void testContainer() throws Exception {
        Container con = fromField(Con.class.getField("sss"));
        Container con2 = fromField(Con.class.getField("ccc"));
        Container con3 = fromField(Con.class.getField("vvv"));
        Container con4 = fromField(Con.class.getField("li"));
    }

    public static class MapWrapper2 {
        @RLP
        @RLPDecoding(as = TreeMap.class)
        @RLPEncoding(keyOrdering = StringComparator.class)
        public Map<String, Map<String, String>> map = new HashMap<>();
    }

    @Test
    public void testMapWrapper2() {
        MapWrapper2 wrapper2 = new MapWrapper2();
        wrapper2.map.put("1", new HashMap<>());
        wrapper2.map.put("22", new HashMap<>());
        wrapper2.map.put("sss", new HashMap<>());
        wrapper2.map.get("sss").put("aaa", "bbb");
        boolean hasSorted = true;
        int i = 1;
        for (String k : wrapper2.map.keySet()) {
            if (k.length() != i) {
                hasSorted = false;
                break;
            }
            i++;
        }
        assert !hasSorted;
        byte[] encoded = RLPCodec.encode(wrapper2);
        RLPElement el = RLPElement.readRLPTree(wrapper2);
        for (int j = 0; j < 3; j++) {
            assert el.get(0).get(j * 2).asString().length() == j + 1;
        }
        MapWrapper2 decoded = RLPCodec.decode(encoded, MapWrapper2.class);
        assert decoded.map instanceof TreeMap;
        assert decoded.map.get("sss").get("aaa").equals("bbb");
    }

    private static class ByteArraySetWrapper {
        @RLP
        @RLPDecoding(as = ByteArraySet.class)
        @RLPEncoding(keyOrdering = BytesComparator.class)
        private Set<byte[]> bytesSet;
    }

    private static class BytesComparator implements Comparator<byte[]> {
        @Override
        public int compare(byte[] o1, byte[] o2) {
            return new BigInteger(1, o1).compareTo(new BigInteger(1, o2));
        }
    }

    @Test
    public void testByteArraySet() {
        ByteArraySetWrapper wrapper =
                RLPList.of(RLPList.of(RLPItem.fromLong(1), RLPItem.fromLong(2))).as(ByteArraySetWrapper.class);
        assert wrapper.bytesSet instanceof ByteArraySet;

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
    }

    private static class ErrorCase {
        @RLP
        @RLPEncoding(keyOrdering = StringComparator.class)
        public List<String> some;
    }

    private static class ErrorCase1 {
        @RLP
        @RLPDecoding(as = JUnit4.class)
        public long some;
    }

    private static class ErrorCase2 {
        @RLP
        @RLPDecoding(as = Map.class)
        public long some;
    }

    private static class ErrorCase3 {
        @RLP
        @RLPDecoding(as = HashMap.class)
        public Set<String> some;
    }

    private static class ErrorCase4 {
        @RLP
        public ByteArrayMap<String> some;
    }

    private static class ErrorCase5 {
        @RLP
        public ByteArraySet some;
    }

    private static class ErrorCase6<V> {
        @RLP
        public Set<V> some;
    }

    private static class ErrorCase7 {
        @RLP
        @RLPDecoding(as = AbstractMap.class)
        public Map<String, String> some;
    }

    @Test(expected = RuntimeException.class)
    public void testCompareListFailed() {
        RLPCodec.encode(new ErrorCase());
    }

    @Test(expected = RuntimeException.class)
    public void testFail() {
        RLPCodec.decode(RLPCodec.encode(new ErrorCase1()), ErrorCase1.class);
    }

    @Test(expected = RuntimeException.class)
    public void testFail2() {
        RLPCodec.decode(RLPCodec.encode(new ErrorCase2()), ErrorCase2.class);
    }

    @Test(expected = RuntimeException.class)
    public void testFail3() {
        RLPCodec.decode(RLPCodec.encode(new ErrorCase3()), ErrorCase3.class);
    }

    @Test(expected = RuntimeException.class)
    public void testFail4() {
        RLPCodec.decode(RLPCodec.encode(new ErrorCase4()), ErrorCase4.class);
    }

    @Test(expected = RuntimeException.class)
    public void testFail5() {
        RLPCodec.decode(RLPCodec.encode(new ErrorCase5()), ErrorCase5.class);
    }

    @Test(expected = RuntimeException.class)
    public void testFail6() {
        RLPCodec.decode(RLPCodec.encode(new ErrorCase6<String>()), ErrorCase6.class);
    }


    @Test
    public void test000() throws Exception {
        System.out.println(Charset.defaultCharset());
        for (int i = 0; i < 1000; i++) {
            SecureRandom sr = new SecureRandom();
            byte[] randBytes = new byte[32];
            sr.nextBytes(randBytes);
            byte[] randBytes2 = new String(randBytes).getBytes();
            if (!Arrays.equals(randBytes, randBytes2)) {
                printHex(randBytes);
                printHex(randBytes2);
            }
        }
    }

    @Test
    public void test0001() throws Exception {
        for(int i = 0; i < 256; i++){
            byte[] data = new byte[]{(byte) i};
            System.out.println(Hex.encodeHexString(data) + " -> " + Hex.encodeHexString(new String(data).getBytes()));
        }
    }

    private void printHex(byte[] bytes) {
        int[] res = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            res[i] = bytes[i] & 0xff;
        }
        String s = Arrays.stream(res).mapToObj(x -> Hex.encodeHexString(new byte[]{(byte) x}))
                .reduce("", (x, y) -> x + "-" + y);
        System.out.println(s);
    }
}
