package com.fmsh.blockchain.biz.block;

/**
 * 区块body内一条指令
 * @author wuweifeng wrote on 2018/3/2.
 */
public class Instruction extends InstructionBase {
    /**
     * 新的内容
     */
    private String json;
    /**
     * 时间戳
     */
    private Long timestamp;
    /**
     * 操作人的公钥
     */
    private String publicKey;
    /**
     * 签名
     */
    private String sign;
    /**
     * 该操作的hash
     */
    private String hash;


    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "Instruction{" +
                "json='" + json + '\'' +
                ", timestamp=" + timestamp +
                ", publicKey='" + publicKey + '\'' +
                ", sign='" + sign + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}
