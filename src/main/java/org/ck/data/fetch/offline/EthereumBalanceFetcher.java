package org.ck.data.fetch.offline;

import org.ck.persistence.Database;
import org.ck.persistence.PersistenceFactory;
import org.ck.utils.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class EthereumBalanceFetcher implements OfflineFetch {
    private static final Logger log = LoggerFactory.getLogger(EthereumBalanceFetcher.class);
    private static final String PROGRESS_TABLE = "eth_balances_progress";
    private static final String BALANCES_TABLE = "eth_balances";

    private final List<Web3j> web3jArr;
    private final Properties config;
    private final Database database;
    private final ForkJoinPool customThreadPool;
    private final int maxBlocksPerBatch;  // 每批次最大区块数量

    private final Random random = new Random();


    public EthereumBalanceFetcher(List<Web3j> web3jArr, Properties config) {
        this.web3jArr = web3jArr;
        this.config = config;
        this.database = PersistenceFactory.createDatabase("sqlite"); // 使用 SQLite 数据库
        this.customThreadPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors()); // 根据CPU核心数调整并行度
        this.maxBlocksPerBatch = Integer.parseInt(config.getProperty("max.blocks.per.batch", "8"));  // 可配置的最大区块数量
        initializeDatabase(); // 初始化数据库表
    }

    private Web3j getRandomWeb3j() {
        return web3jArr.get(random.nextInt(web3jArr.size()));
    }

    @Override
    public void fetch(Long fromBlockNo, Long toBlockNo) {
        validateBlockRange(fromBlockNo, toBlockNo);

        BigInteger lastProcessedBlock = getLastProcessedBlock(fromBlockNo);

        for (BigInteger blockNumber = lastProcessedBlock;
             blockNumber.compareTo(BigInteger.valueOf(toBlockNo)) <= 0;
             blockNumber = blockNumber.add(BigInteger.valueOf(maxBlocksPerBatch))) {

            BigInteger batchEndBlock = blockNumber.add(BigInteger.valueOf(maxBlocksPerBatch - 1))
                    .min(BigInteger.valueOf(toBlockNo));

            try {
                processBlocksInRange(blockNumber, batchEndBlock);
                saveProgress(batchEndBlock);
            } catch (Exception e) {
                log.error("Failed to process blocks from " + blockNumber + " to " + batchEndBlock, e);
                break;  // 终止处理并报告错误
            }
        }

        log.info("Balance fetching completed. Data saved in SQLite database.");
    }

    private void initializeDatabase() {
        String createProgressTableSQL = "CREATE TABLE IF NOT EXISTS " + PROGRESS_TABLE + " (" +
                "id INTEGER PRIMARY KEY, " +
                "block_number BIGINT NOT NULL);";

        String createBalancesTableSQL = "CREATE TABLE IF NOT EXISTS " + BALANCES_TABLE + " (" +
                "address TEXT PRIMARY KEY, " +
                "balance DECIMAL(20, 10) NOT NULL);";

        database.executeUpdate(createProgressTableSQL);
        database.executeUpdate(createBalancesTableSQL);

        // 提高SQLite的写入性能
        database.executeQuery("PRAGMA journal_mode=WAL;", rs -> {});

        database.executeUpdate("PRAGMA synchronous=NORMAL;");
    }

    private BigInteger getLastProcessedBlock(Long fromBlockNo) {
        String querySQL = "SELECT block_number FROM " + PROGRESS_TABLE + " ORDER BY block_number DESC LIMIT 1;";
        final BigInteger[] lastBlock = {BigInteger.valueOf(fromBlockNo)};

        database.executeQuery(querySQL, rs -> {
            try {
                if (rs.next()) {
                    lastBlock[0] = new BigInteger(rs.getString("block_number")).add(BigInteger.ONE);
                }
            } catch (SQLException e) {
                log.error("Error retrieving last processed block", e);
            }
        });

        return lastBlock[0];
    }

    private void saveProgress(BigInteger blockNumber) {
        String insertSQL = "INSERT INTO " + PROGRESS_TABLE + " (block_number) VALUES (?);";
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setLong(1, blockNumber.longValue());
            pstmt.executeUpdate();
            log.info("Progress saved at block number: " + blockNumber);

        } catch (SQLException e) {
            log.error("Failed to save progress", e);
        }
    }

    private void validateBlockRange(Long fromBlockNo, Long toBlockNo) {
        BigInteger latestBlockNumber = Retry.doRetry(() -> {
                    try {
                        return getRandomWeb3j().ethBlockNumber().send().getBlockNumber();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, Integer.parseInt(config.getProperty("retry.max.attempts", "10")),
                Long.parseLong(config.getProperty("retry.sleep.time", "1000")));

        if (fromBlockNo < 1 || fromBlockNo > toBlockNo) {
            throw new InvalidParameterException("Invalid block range");
        }

        if (latestBlockNumber.compareTo(BigInteger.valueOf(toBlockNo)) < 0) {
            throw new InvalidParameterException("Latest block number is smaller than the requested to block");
        }
    }

    private void processBlocksInRange(BigInteger startBlock, BigInteger endBlock) {
        List<EthBlock> blocks = getBlocksInRange(startBlock, endBlock);

        blocks.forEach(block -> {
            String minerAddress = block.getBlock().getMiner();
            List<String> addressesToUpdate = new ArrayList<>();
            addressesToUpdate.add(minerAddress);

            customThreadPool.submit(() ->
                    block.getBlock().getTransactions().parallelStream().forEach(transactionResult -> {
                        EthBlock.TransactionObject transaction = (EthBlock.TransactionObject) transactionResult.get();
                        String fromAddress = transaction.getFrom();
                        String toAddress = transaction.getTo();

                        if (fromAddress != null) {
                            addressesToUpdate.add(fromAddress);
                        }
                        if (toAddress != null) {
                            addressesToUpdate.add(toAddress);
                        }
                    })
            ).join();

            batchUpdateBalances(addressesToUpdate);

            log.info(String.format("Processed block %d, transactions: %d", block.getBlock().getNumber(), block.getBlock().getTransactions().size()));
        });
    }

    private List<EthBlock> getBlocksInRange(BigInteger startBlock, BigInteger endBlock) {
        List<CompletableFuture<EthBlock>> futures = new ArrayList<>();

        for (BigInteger blockNumber = startBlock; blockNumber.compareTo(endBlock) <= 0; blockNumber = blockNumber.add(BigInteger.ONE)) {
            final BigInteger currentBlockNumber = blockNumber;  // 使用局部 final 变量
            CompletableFuture<EthBlock> future = CompletableFuture.supplyAsync(() -> {
                return Retry.doRetry(() -> {
                            try {
                                return getRandomWeb3j().ethGetBlockByNumber(new DefaultBlockParameterNumber(currentBlockNumber), true).send();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, Integer.parseInt(config.getProperty("retry.max.attempts", "10")),
                        Long.parseLong(config.getProperty("retry.sleep.time", "1000")));
            }, customThreadPool);
            futures.add(future);
        }

        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    private void batchUpdateBalances(List<String> addresses) {
        List<CompletableFuture<Void>> futures = addresses.stream()
                .map(address -> CompletableFuture.runAsync(() -> {
                    try {
                        EthGetBalance ethGetBalance = Retry.doRetry(() -> {
                                    try {
                                        return getRandomWeb3j().ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }, Integer.parseInt(config.getProperty("retry.max.attempts", "10")),
                                Long.parseLong(config.getProperty("retry.sleep.time", "1000")));

                        BigInteger balanceInWei = ethGetBalance.getBalance();
                        BigDecimal balanceInEth = Convert.fromWei(balanceInWei.toString(), Convert.Unit.ETHER);

                        if (balanceInEth.compareTo(BigDecimal.ZERO) > 0) {
                            updateBalanceInDatabase(address, balanceInEth);
                        }
                    } catch (Exception e) {
                        log.error("Error checking balance for address " + address, e);
                    }
                }, customThreadPool))
                .collect(Collectors.toList());

        futures.forEach(CompletableFuture::join);
    }

    private void updateBalanceInDatabase(String address, BigDecimal balance) {
        String updateSQL = "UPDATE " + BALANCES_TABLE + " SET balance = ? WHERE address = ?";
        String insertSQL = "INSERT INTO " + BALANCES_TABLE + " (address, balance) VALUES (?, ?)";

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSQL);
                 PreparedStatement insertPstmt = conn.prepareStatement(insertSQL)) {

                // 先尝试更新
                updatePstmt.setBigDecimal(1, balance);
                updatePstmt.setString(2, address);
                int rowsAffected = updatePstmt.executeUpdate();

                // 如果没有更新到，插入新记录
                if (rowsAffected == 0) {
                    insertPstmt.setString(1, address);
                    insertPstmt.setBigDecimal(2, balance);
                    insertPstmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                log.error("Failed to update/insert balance for address " + address, e);
            }
        } catch (SQLException e) {
            log.error("Failed to manage database connection", e);
        }
    }
}
