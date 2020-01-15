package org.tdf.rlp;

public class Transaction {
    public int version;

    public int type;

    public long nonce;

    public byte[] from;

    public long gasPrice;

    public long amount;

    public byte[] payload;

    public byte[] to;

    public byte[] signature;
}
