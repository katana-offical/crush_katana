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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class EthereumBalanceFetcher implements OfflineFetch {
    private static final Logger log = LoggerFactory.getLogger(EthereumBalanceFetcher.class);
    private static final String PROGRESS_TABLE = "eth_balances_progress";
    private static final String BALANCES_TABLE = "eth_balances";

    private final Web3j web3j;
    private final Properties config;
    private final Database database;

    public EthereumBalanceFetcher(Web3j web3j, Properties config) {
        this.web3j = web3j;
        this.config = config;
        this.database = PersistenceFactory.createDatabase("sqlite"); // 使用 SQLite 数据库
        initializeDatabase(); // 初始化数据库表
    }

    @Override
    public void fetch(Long fromBlockNo, Long toBlockNo, String outputPath) {
        validateBlockRange(fromBlockNo, toBlockNo);

        BigInteger lastProcessedBlock = getLastProcessedBlock(fromBlockNo);

        for (BigInteger blockNumber = lastProcessedBlock;
             blockNumber.compareTo(BigInteger.valueOf(toBlockNo)) <= 0;
             blockNumber = blockNumber.add(BigInteger.ONE)) {

            try {
                processBlock(blockNumber);
                saveProgress(blockNumber);
            } catch (Exception e) {
                log.error("Failed to process block " + blockNumber, e);
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
                        return web3j.ethBlockNumber().send().getBlockNumber();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, Integer.parseInt(config.getProperty("retry.max.attempts", "3")),
                Long.parseLong(config.getProperty("retry.sleep.time", "1000")));

        if (fromBlockNo < 1 || fromBlockNo > toBlockNo) {
            throw new InvalidParameterException("Invalid block range");
        }

        if (latestBlockNumber.compareTo(BigInteger.valueOf(toBlockNo)) < 0) {
            throw new InvalidParameterException("Latest block number is smaller than the requested to block");
        }
    }

    private void processBlock(BigInteger blockNumber) {
        EthBlock block = Retry.doRetry(() -> {
                    try {
                        return web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockNumber), true).send();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, Integer.parseInt(config.getProperty("retry.max.attempts", "3")),
                Long.parseLong(config.getProperty("retry.sleep.time", "1000")));

        String minerAddress = block.getBlock().getMiner();
        updateBalance(minerAddress);

        block.getBlock().getTransactions().forEach(transactionResult -> {
            EthBlock.TransactionObject transaction = (EthBlock.TransactionObject) transactionResult.get();

            String fromAddress = transaction.getFrom();
            String toAddress = transaction.getTo();

            if (fromAddress != null) {
                updateBalance(fromAddress);
            }

            if (toAddress != null) {
                updateBalance(toAddress);
            }
        });

        log.info(String.format("Processed block %d, transactions: %d", blockNumber, block.getBlock().getTransactions().size()));
    }

    private void updateBalance(String address) {
        if (address == null) return;

        try {
            EthGetBalance ethGetBalance = Retry.doRetry(() -> {
                        try {
                            return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, Integer.parseInt(config.getProperty("retry.max.attempts", "3")),
                    Long.parseLong(config.getProperty("retry.sleep.time", "1000")));

            BigInteger balanceInWei = ethGetBalance.getBalance();
            BigDecimal balanceInEth = Convert.fromWei(balanceInWei.toString(), Convert.Unit.ETHER);

            if (balanceInEth.compareTo(BigDecimal.ZERO) > 0) {
                saveOrUpdateBalance(address, balanceInEth);
            }
        } catch (Exception e) {
            log.error("Error checking balance for address " + address, e);
        }
    }

    private void saveOrUpdateBalance(String address, BigDecimal balance) {
        String selectSQL = "SELECT balance FROM " + BALANCES_TABLE + " WHERE address = ?;";
        String updateSQL = "UPDATE " + BALANCES_TABLE + " SET balance = ? WHERE address = ?;";
        String insertSQL = "INSERT INTO " + BALANCES_TABLE + " (address, balance) VALUES (?, ?);";

        try (Connection conn = database.getConnection();
             PreparedStatement selectPstmt = conn.prepareStatement(selectSQL)) {

            selectPstmt.setString(1, address);
            ResultSet rs = selectPstmt.executeQuery();

            if (rs.next()) {
                BigDecimal existingBalance = rs.getBigDecimal("balance");
                balance = balance.add(existingBalance);

                try (PreparedStatement updatePstmt = conn.prepareStatement(updateSQL)) {
                    updatePstmt.setBigDecimal(1, balance);
                    updatePstmt.setString(2, address);
                    updatePstmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertSQL)) {
                    insertPstmt.setString(1, address);
                    insertPstmt.setBigDecimal(2, balance);
                    insertPstmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            log.error("Failed to update balance for address: " + address, e);
        }
    }
}
