package org.ck.data.fetch.offline;

import com.google.common.collect.ImmutableList;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.DeterministicSeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class BitcoinBalanceFetcher implements OfflineFetch {

    private static final Logger log = LoggerFactory.getLogger(BitcoinBalanceFetcher.class);

    @Override
    public void fetch(Long fromBlockNo, Long toBlockNo) throws Exception {

    }
}
