package org.tdf.rlp;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

public final class SpongyCastleProvider {

    private static class Holder {
        private static final Provider INSTANCE;
        static{
            Provider p = Security.getProvider("SC");

            INSTANCE = (p != null) ? p : new BouncyCastleProvider();

            INSTANCE.put("MessageDigest.ETH-KECCAK-256", "org.tdf.rlp.Keccak256");
            INSTANCE.put("MessageDigest.ETH-KECCAK-512", "org.tdf.rlp.Keccak256");
        }
    }

    public static Provider getInstance() {
        return Holder.INSTANCE;
    }
}
