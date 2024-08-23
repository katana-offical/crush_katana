package org.ck.cluster;

import org.ck.persistence.Database;
import org.ck.persistence.PersistenceFactory;
import org.ck.wordlist.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

public class SeqQueueStrategy implements AllocateStrategy {

    private static final Logger log = LoggerFactory.getLogger(SeqQueueStrategy.class);

    private String[] mnemonics;
    private BigInteger totalCombinations;
    private Database database;

    public SeqQueueStrategy(Properties config) {
        this.database = PersistenceFactory.createDatabase("sqlite");
        initializeDatabase();
        loadMnemonics();
        totalCombinations = calculateCombinations(mnemonics.length, 12);
    }

    @Override
    public void init() {
        // 可根据需要初始化额外的配置
    }

    @Override
    public Task acquireTask() {
        BigInteger nextIndex = getLastProcessedIndex().add(BigInteger.ONE);

        if (nextIndex.compareTo(totalCombinations) >= 0) {
            log.info("All tasks have been distributed.");
            return null; // No more tasks available
        }

        String[] taskMnemonic = getMnemonicCombination(nextIndex);
        Task task = new Task();
        task.setMnemonic(taskMnemonic);
        task.setTaskId(nextIndex.toString());
        saveTask(task);
        saveProgress(nextIndex);
        return task;
    }

    @Override
    public String getStrategyName() {
        return "SeqQueueStrategy";
    }

    private void loadMnemonics() {
        mnemonics = English.words;
    }

    private void initializeDatabase() {
        String createProgressTableSQL = "CREATE TABLE IF NOT EXISTS eth_allocate_progress (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "last_processed_index TEXT NOT NULL);";

        String createTasksTableSQL = "CREATE TABLE IF NOT EXISTS eth_allocate_tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id TEXT NOT NULL, " +
                "mnemonic TEXT NOT NULL, " +
                "status TEXT NOT NULL);";

        database.executeUpdate(createProgressTableSQL);
        database.executeUpdate(createTasksTableSQL);
    }

    public BigInteger getLastProcessedIndex() {
        String querySQL = "SELECT last_processed_index FROM eth_allocate_progress ORDER BY id DESC LIMIT 1;";
        final BigInteger[] lastIndex = {BigInteger.ZERO};

        database.executeQuery(querySQL, rs -> {
            try {
                if (rs.next()) {
                    lastIndex[0] = new BigInteger(rs.getString("last_processed_index"));
                }
            } catch (SQLException e) {
                log.error("Error retrieving last processed index", e);
            }
        });

        return lastIndex[0];
    }

    private void saveProgress(BigInteger index) {
        String insertSQL = "INSERT INTO eth_allocate_progress (last_processed_index) VALUES (?);";
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, index.toString());
            pstmt.executeUpdate();
            log.info("Progress saved at index: " + index);

        } catch (SQLException e) {
            log.error("Failed to save progress", e);
        }
    }

    private void saveTask(Task task) {
        String insertSQL = "INSERT INTO eth_allocate_tasks (task_id, mnemonic, status) VALUES (?, ?, ?);";
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, task.getTaskId());
            pstmt.setString(2, String.join(" ", task.getMnemonic()));
            pstmt.setString(3, "pending");
            pstmt.executeUpdate();
            log.info("Task saved with ID: " + task.getTaskId());

        } catch (SQLException e) {
            log.error("Failed to save task", e);
        }
    }

    private String[] getMnemonicCombination(BigInteger index) {
        int[] indices = getCombinationIndices(index.intValue(), mnemonics.length, 12);
        String[] combination = new String[12];

        for (int i = 0; i < 12; i++) {
            combination[i] = mnemonics[indices[i]];
        }

        return combination;
    }

    private BigInteger calculateCombinations(int n, int k) {
        return factorial(n).divide(factorial(k).multiply(factorial(n - k)));
    }

    private BigInteger factorial(int number) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= number; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    private int[] getCombinationIndices(int index, int n, int k) {
        int[] indices = new int[k];
        int a = n, b = k, x = index;

        for (int i = 0; i < k; i++) {
            indices[i] = x % a;
            x /= a;
            a--;
        }

        Arrays.sort(indices);
        return indices;
    }
}
