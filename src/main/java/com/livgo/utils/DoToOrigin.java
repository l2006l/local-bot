package com.livgo.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.livgo.po.FileDetail;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class DoToOrigin {

    public static void doToOrigin(FileDetail fd, String dateStr, AtomicInteger count, String origin) {
        File oriFile = FileUtil.file(PathUtil.getJarPath(), fd.getFilePath());
        File target = FileUtil.file(PathUtil.getJarPath(),
                origin,
                dateStr,
                fd.getOriginalFileName());
        if (target.exists() && target.isFile()) {
            if (DigestUtil.md5Hex(target).equals(fd.getFileMd5())) {
                return;
            }
            String nameWithOutExt = FileNameUtil.mainName(oriFile);
            File f = FileUtil.file(PathUtil.getJarPath(),
                    origin,
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
        FileUtil.copy(oriFile, target, false);
        count.getAndIncrement();
    }

}
