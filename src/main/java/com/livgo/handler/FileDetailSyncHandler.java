package com.livgo.handler;

import com.livgo.po.FileDetail;
import com.livgo.utils.SyncFileUtil;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

public class FileDetailSyncHandler implements OperationHandler<FileDetail, Void> {

    @Override
    public String exec(List<FileDetail> data, Void unused, String[] param) {
        int count = SyncFileUtil.syncFiles();
        String msg = "整合完成，共处理" + count + "个文件";
        return String.format("window.msg.info('%s')", msg);
    }
}
