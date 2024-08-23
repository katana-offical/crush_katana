package org.ck.data.fetch.offline;


import java.io.IOException;

public interface OfflineFetch {

    /**
     * 离线获取数据
     * @param fromBlockNo
     * @param toBlockNo
     */
    void fetch(Long fromBlockNo, Long toBlockNo) throws Exception;


}
