package org.ck;

import com.google.common.collect.ImmutableList;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.ck.account.KeyPairUtils;
import org.ck.account.MnemonicUtils;
import org.ck.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.util.List;
import java.util.Objects;

/**
 * Unit test for simple App.
 */
public class MnemonicTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(MnemonicTest.class);
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MnemonicTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( MnemonicTest.class );
    }


    /**
     * Rigourous Test :-)
     */
    public void testMnemonicForEth()
    {
        // generate 12 word mnemonic words by default
        // angry lion together divide square oblige film together rebel possible bonus above
        String mnemonic = MnemonicUtils.generateMnemonic();
        log.info("generate 12 word mnemonic words = {}",mnemonic);

        // generate seed
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        log.info("generate seed = " + Numeric.toHexString(seed));

        // generate priKey
        byte[] ethPrivateKeyBytes = KeyPairUtils.generatePrivateKey(seed, KeyPairUtils.CoinTypes.ETH);
        log.info("generate eth privateKey = " + Numeric.toHexString(ethPrivateKeyBytes));

        // parse pubKey
        ECKeyPair ethKeyPair = ECKeyPair.create(ethPrivateKeyBytes);
        log.info("get eth publicKey = " + Numeric.toHexString(ethKeyPair.getPublicKey().toByteArray()));

        log.info("generate eth privateKey = " + Numeric.toHexString(ethKeyPair.getPrivateKey().toByteArray()));

        // parse address
        log.info("transfer to eth address = " + Keys.getAddress(ethKeyPair.getPublicKey()));
    }

    public void testMnemonicForBtc() throws UnreadableWalletException {
        // 使用一个12个单词的助记词
        String mnemonic = "angry lion together divide square oblige film together rebel possible bonus above";
        String passphrase = "";

        // 将助记词转换为种子
        DeterministicSeed seed = new DeterministicSeed(mnemonic, null, passphrase, 0L);

        // 定义比特币主网参数
        NetworkParameters params = MainNetParams.get();

        // 生成根私钥（Master Private Key）
        DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(Objects.requireNonNull(seed.getSeedBytes()));

        // 使用 BIP-44 的派生路径：m/44'/0'/0'/0/0
        List<ChildNumber> bip44Path = ImmutableList.of(
                new ChildNumber(44, true),
                new ChildNumber(0, true),
                new ChildNumber(0, true),
                ChildNumber.ZERO,
                ChildNumber.ZERO
        );

        // 通过派生路径生成子私钥
        DeterministicHierarchy hierarchy = new DeterministicHierarchy(masterKey);
        DeterministicKey childKey = hierarchy.get(bip44Path, true, true);

        // 生成私钥和公钥
        ECKey privateKey = ECKey.fromPrivate(childKey.getPrivKeyBytes());
        String privateKeyHex = privateKey.getPrivateKeyAsHex();
        String publicKeyHex = privateKey.getPublicKeyAsHex();

        log.info("Private Key: " + privateKeyHex);
        log.info("Public Key: " + publicKeyHex);

        // 生成 Legacy 地址（P2PKH）
        LegacyAddress legacyAddress = LegacyAddress.fromPubKeyHash(params, privateKey.getPubKeyHash());
        log.info("Legacy Address (P2PKH): " + legacyAddress);

        // 生成 Nested SegWit 地址（P2SH）
        Script p2wpkhScript = ScriptBuilder.createP2WPKHOutputScript(privateKey);
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(p2wpkhScript);
        LegacyAddress p2shAddress = LegacyAddress.fromScriptHash(params, Utils.sha256hash160(p2shScript.getProgram()));
        log.info("Nested SegWit Address (P2SH): " + p2shAddress);

        // 生成 Native SegWit 地址（Bech32）
        // todo:bad address? can not match address generate by unisat
        SegwitAddress segwitAddress = SegwitAddress.fromKey(params, privateKey);
        log.info("Native SegWit Address (Bech32): " + segwitAddress);
    }
}
