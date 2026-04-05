package com.livgo.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.livgo.po.FileDetail;
import com.livgo.utils.PathUtil;
import xyz.erupt.annotation.fun.OperationHandler;

import java.io.File;
import java.util.List;

public class FileDetailOriginPart implements OperationHandler<FileDetail, Void> {

    private static final String ORIGIN = "origin-part";

    @Override
    public String exec(List<FileDetail> data, Void unused, String[] param) {
        if (data.isEmpty()) {
            return "window.msg.warning('请选择需要转出的文件')";
        }
        Integer  count = 0;
        String dateStr = DateUtil.format(DateUtil.date(), "yyyy-MM-dd");
        for (FileDetail fd : data) {
            File oriFile = FileUtil.file(PathUtil.getJarPath(), fd.getFilePath());
            File target = FileUtil.file(PathUtil.getJarPath(),
                    ORIGIN,
                    dateStr,
                    fd.getOriginalFileName());
            if (target.exists() && target.isFile()) {
                if (DigestUtil.md5Hex(target).equals(fd.getFileMd5())) {
                    continue;
                }
                String nameWithOutExt = FileNameUtil.mainName(oriFile);
                File f = FileUtil.file(PathUtil.getJarPath(),
                        ORIGIN,
                        dateStr,
                        nameWithOutExt,
                        fd.getOriginalFileName());
                if (FileUtil.exist(f)) {
                    continue;
                }
                FileUtil.copy(oriFile, f, false);
                count++;
                continue;
            }
            FileUtil.copy(oriFile, target, false);
            count++;
        }
        String msg = "整合完成，共处理" + count + "个文件";
        return String.format("window.msg.info('%s')", msg);
    }
}
