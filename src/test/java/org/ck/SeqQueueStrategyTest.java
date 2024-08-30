package org.ck;

import org.ck.cluster.SeqQueueStrategy;
import org.ck.cluster.Task;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Properties;

import static org.junit.Assert.*;

public class SeqQueueStrategyTest {

    private static final Logger log = LoggerFactory.getLogger(SeqQueueStrategyTest.class);

    private SeqQueueStrategy seqQueueStrategy;

    @Before
    public void setUp() {
        Properties config = new Properties();
        // 初始化策略
        seqQueueStrategy = new SeqQueueStrategy(config);
    }

    @Test
    public void testTaskAllocation() {
        Task task1 = seqQueueStrategy.acquireTask();
        assertNotNull("Task should not be null", task1);
        log.info("First Task: " + task1.getTaskId() + " Mnemonic: " + String.join(" ", task1.getMnemonic()));

        Task task2 = seqQueueStrategy.acquireTask();
        assertNotNull("Task should not be null", task2);
        assertNotEquals("Each task should have a unique ID", task1.getTaskId(), task2.getTaskId());
        log.info("Second Task: " + task2.getTaskId() + " Mnemonic: " + String.join(" ", task2.getMnemonic()));

        // 检查是否任务按顺序分配
        BigInteger id1 = new BigInteger(task1.getTaskId());
        BigInteger id2 = new BigInteger(task2.getTaskId());
        assertTrue("Second task ID should be greater than first task ID", id2.compareTo(id1) > 0);
    }


    @Test
    public void testTaskAllocationMultiTimes(){
        for(int i=0; i<100000; i++){
            testTaskAllocation();
        }
    }

    @Test
    public void testProgressPersistence() {
        Task task = seqQueueStrategy.acquireTask();
        assertNotNull("Task should not be null", task);

        // 获取最后处理的索引
        BigInteger lastIndex = seqQueueStrategy.getLastProcessedIndex();
        assertEquals("Last processed index should match the task ID", lastIndex, new BigInteger(task.getTaskId()));

        // 再获取一个任务并检查进度是否正确保存
        Task task2 = seqQueueStrategy.acquireTask();
        assertNotNull("Task should not be null", task2);
        BigInteger lastIndexAfterSecondTask = seqQueueStrategy.getLastProcessedIndex();
        assertEquals("Last processed index should match the second task ID", lastIndexAfterSecondTask, new BigInteger(task2.getTaskId()));
    }
}
