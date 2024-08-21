package org.ck.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class Retry {
    private static final Logger log = LoggerFactory.getLogger(Retry.class);

    // 重试机制的实现
    public static <T> T doRetry(Supplier<T> supplier, int maxRetryTime, long failSleepTimeMills) {
        int attempts = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (Exception e) {
                attempts++;
                if (attempts >= maxRetryTime) {
                    log.error("Operation failed after {} attempts: {}", attempts, e.getMessage());
                    throw e;
                }
                log.warn("Operation failed, retrying... (attempt {} of {})", attempts, maxRetryTime);
                try {
                    Thread.sleep(failSleepTimeMills);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread was interrupted during sleep", ex);
                }
            }
        }
    }
}
