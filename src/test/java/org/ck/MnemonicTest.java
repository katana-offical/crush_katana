package org.ck;

import com.google.common.collect.ImmutableList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.crypto.*;
import org.bitcoinj.params.MainNetParams;
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
        String mnemonic = "angry lion together divide square oblige film together rebel possible bonus above";

        String passphrase = "";

        DeterministicSeed seed = new DeterministicSeed(mnemonic, null, passphrase, 0L);

        NetworkParameters params = MainNetParams.get();

        DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(Objects.requireNonNull(seed.getSeedBytes()));

        // use BIP-44 path：m/44'/0'/0'/0/0
        List<ChildNumber> bip44Path = ImmutableList.of(
                new ChildNumber(44, true),
                new ChildNumber(0, true),
                new ChildNumber(0, true),
                ChildNumber.ZERO,
                ChildNumber.ZERO
        );

        // generate private key and public key
        DeterministicHierarchy hierarchy = new DeterministicHierarchy(masterKey);
        DeterministicKey childKey = hierarchy.get(bip44Path, true, true);

        ECKey privateKey = ECKey.fromPrivate(childKey.getPrivKeyBytes());
        String privateKeyHex = privateKey.getPrivateKeyAsHex();
        String publicKeyHex = privateKey.getPublicKeyAsHex();

        log.info("Private Key: " + privateKeyHex);
        log.info("Public Key: " + publicKeyHex);

        // generate Legacy address
        LegacyAddress legacyAddress = LegacyAddress.fromPubKeyHash(params, privateKey.getPubKeyHash());
        log.info("Legacy Address: " + legacyAddress);

        // generate SegWit address
        SegwitAddress segwitAddress = SegwitAddress.fromKey(params, privateKey);
        log.info("SegWit Address: " + segwitAddress);
    }
}
