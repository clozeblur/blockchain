package com.fmsh.blockchain.biz.wallet;

import java.util.HashMap;
import java.util.Map;

public class PairKeyPersist {

    private PairKeyPersist() {}

    private static Map<String, String> walletMap = new HashMap<>();

    public static Map<String, String> getWalletMap() {
        return walletMap;
    }

    public static void setWalletMap(Map<String, String> map) {
        walletMap = map;
    }
}
