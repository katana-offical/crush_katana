package org.ck.cluster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task implements Serializable {
    public String taskId;

    // 12 mnemonic words
    private String[] mnemonic;

    private byte version;
}
