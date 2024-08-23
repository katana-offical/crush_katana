package org.ck.cluster;

import java.io.Serializable;

public class TaskResult implements Serializable {

    private String taskId;
    private Boolean mined;
    private String[] luckyAddress;

    public TaskResult(String taskId, Boolean mined, String[] luckyAddress) {
        this.taskId = taskId;
        this.mined = mined;
        this.luckyAddress = luckyAddress;
    }

    public static TaskResult build(String taskId, Boolean mined, String[] luckyAddress) {
        return new TaskResult(taskId, mined, luckyAddress);
    }
}
