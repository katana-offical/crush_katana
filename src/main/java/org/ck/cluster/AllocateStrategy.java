package org.ck.cluster;

public interface AllocateStrategy {
    void init();
    Task acquireTask();
    String getStrategyName();
}
