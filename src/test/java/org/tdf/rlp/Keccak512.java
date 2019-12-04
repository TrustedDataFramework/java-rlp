package org.tdf.rlp;

public class Keccak512 extends KeccakCore {

    /**
     * Create the engine.
     */
    public Keccak512()
    {
        super("eth-keccak-512");
    }

    /** @see Digest */
    public Digest copy()
    {
        return copyState(new Keccak512());
    }

    /** @see Digest */
    public int engineGetDigestLength()
    {
        return 64;
    }

    @Override
    protected byte[] engineDigest() {
        return null;
    }

    @Override
    protected void engineUpdate(byte input) {
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
    }
}
