package org.ck;

import junit.framework.TestCase;
import org.ck.data.fetch.offline.EthereumBalanceFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.Properties;

public class FetchTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(FetchTest.class);


    /**
     * 该测试方法也许会消耗基础服务的使用次数，请谨慎使用，后续用自己搭建的geth
     */
    public void testFetchEthereumBalance() {
        // https://mainnet.infura.io/v3/87dd52c6af094eb2a518f5a1349aba03
        Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/87dd52c6af094eb2a518f5a1349aba03"));
        Properties properties = new Properties();
        EthereumBalanceFetcher fetcher = new EthereumBalanceFetcher(web3j, properties);
        fetcher.fetch(1L, 20569025L, "positive_balances.csv");
        log.info("fetch done");
    }
}
