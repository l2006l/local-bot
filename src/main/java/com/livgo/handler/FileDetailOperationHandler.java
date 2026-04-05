package com.livgo.handler;

import com.livgo.mapper.FileDetailMapper;
import com.livgo.po.FileDetail;
import com.livgo.utils.SyncFileUtil;
import jakarta.annotation.Resource;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

import static com.livgo.utils.PathUtil.getJarPath;

public class FileDetailOperationHandler implements OperationHandler<FileDetail, Void> {

    private final String rootPath = getJarPath();

    private final String TEMP_PATH = "tempFile";

    private final String LEVELS = "levels";

    @Resource
    private FileDetailMapper mapper;

    @Override
    public String exec(List<FileDetail> data, Void unused, String[] param) {
        int count = SyncFileUtil.syncFiles();
        String msg = "整合完成，共处理" + count + "个文件";
        return String.format("window.msg.info('%s')", msg);
    }
}
