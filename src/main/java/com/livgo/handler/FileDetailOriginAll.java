package com.livgo.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.livgo.mapper.FileDetailMapper;
import com.livgo.po.FileDetail;
import com.livgo.utils.PathUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FileDetailOriginAll implements OperationHandler<FileDetail, Void> {

    @Resource
    private FileDetailMapper mapper;

    private static final String ORIGIN = "origin-all";

    @Override
    public String exec(List<FileDetail> data, Void unused, String[] param) {
        String dateStr = DateUtil.format(DateUtil.date(), "yyyy-MM-dd");
        AtomicInteger count = new AtomicInteger();
        mapper.selectList(new QueryWrapper<>(null), resultContext -> {
            FileDetail fd = resultContext.getResultObject();
            File oriFile = FileUtil.file(PathUtil.getJarPath(), fd.getFilePath());
            File file = FileUtil.file(PathUtil.getJarPath(),
                    ORIGIN,
                    dateStr,
                    fd.getOriginalFileName());
            if (FileUtil.exist(file) && FileUtil.isFile(file)) {
                if (DigestUtil.md5Hex(file).equals(fd.getFileMd5())) {
                    return;
                }
                String nameWithOutExt = FileNameUtil.mainName(oriFile);
                File f = FileUtil.file(PathUtil.getJarPath(),
                        ORIGIN,
                        dateStr,
                        nameWithOutExt,
                        fd.getOriginalFileName());
                if (FileUtil.exist(f)) {
                    return;
                }
                FileUtil.copy(oriFile, f, false);
                count.getAndIncrement();
                return;
            }
            FileUtil.copy(oriFile, file, false);
            count.getAndIncrement();
        });
        String msg = "整合完成，共处理" + count + "个文件";
        return String.format("window.msg.info('%s')", msg);
    }
}
