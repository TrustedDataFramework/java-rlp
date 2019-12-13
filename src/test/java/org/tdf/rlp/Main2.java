package org.tdf.rlp;

public class Main2 {
    public static void main(String[] args) {
        int n = 1000000;
        RLPList list = RLPList.createEmpty(n);
        byte[] bytes = new byte[]{1};
        for (int i = 0; i < n; i++) {
            list.add(RLPItem.fromBytes(bytes));
        }
        long start = System.currentTimeMillis();
        byte[] encoded = list.getEncoded();
        long end = System.currentTimeMillis();
        System.out.println("encode " + (n * 32) + " bytes in " + (end - start) + " ms");

        start = System.currentTimeMillis();
        RLPElement decoded = RLPElement.fromEncoded(encoded);
        end = System.currentTimeMillis();
        System.out.println("decode " + (n * 32) + " bytes in " + (end - start) + " ms");
    }
}
