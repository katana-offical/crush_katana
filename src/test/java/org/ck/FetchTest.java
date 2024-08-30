package org.ck;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.ck.data.fetch.offline.EthereumBalanceFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class FetchTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(FetchTest.class);


    /**
     * 该测试方法也许会消耗基础服务的使用次数，请谨慎使用，后续用自己搭建的geth
     */
    public void testFetchEthereumBalance() {
        // https://mainnet.infura.io/v3/87dd52c6af094eb2a518f5a1349aba03
        //https://eth.llamarpc.com/
        //https://eth-mainnet.public.blastapi.io
        //https://rpc.flashbots.net/
        //https://cloudflare-eth.com/
        //https://ethereum.publicnode.com
        //https://nodes.mewapi.io/rpc/eth
        //https://eth-mainnet.nodereal.io/v1/1659dfb40aa24bbb8153a677b98064d7
        //Web3j web3j = Web3j.build(new HttpService("https://eth-mainnet.nodereal.io/v1/1659dfb40aa24bbb8153a677b98064d7"));
        List<Web3j> web3js = acquireWeb3Nodes();
        Properties properties = new Properties();
        EthereumBalanceFetcher fetcher = new EthereumBalanceFetcher(web3js, properties);
        fetcher.fetch(1L, 20569025L);
        log.info("fetch done");
    }

    private List<Web3j> acquireWeb3Nodes(){
        List<Web3j> web3jArr = Lists.newArrayList();
        final List<String> nodes = List.of(
                "https://mainnet.infura.io/v3/87dd52c6af094eb2a518f5a1349aba03",
                "https://eth.llamarpc.com/",
                "https://eth-mainnet.public.blastapi.io",
                "https://rpc.flashbots.net/",
               // "https://cloudflare-eth.com/",
                "https://ethereum.publicnode.com",
                "https://nodes.mewapi.io/rpc/eth",
                "https://eth-mainnet.nodereal.io/v1/1659dfb40aa24bbb8153a677b98064d7"
        );
        return nodes.stream().map(url -> Web3j.build(new HttpService(url))).collect(Collectors.toList());
    }
}
