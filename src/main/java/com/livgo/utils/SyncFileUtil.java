package com.livgo.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.livgo.mapper.FileDetailMapper;
import com.livgo.po.FileDetail;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.livgo.utils.PathUtil.getJarPath;

@Component
@Slf4j
public class SyncFileUtil {

    private final FileDetailMapper fileDetailMapper;

    private static FileDetailMapper mapper;

    private static final String rootPath = getJarPath();

    private static final String TEMP_PATH = "tempFile";

    private static final String LEVELS = "levels";

    public SyncFileUtil(FileDetailMapper fileDetailMapper) {
        this.fileDetailMapper = fileDetailMapper;
    }

    @PostConstruct
    public void init() {
        mapper = fileDetailMapper;
    }

    public static int syncFiles() {
        log.info("开始整合文件");
        AtomicInteger count = new AtomicInteger(0);
        List<FileDetail> fdl = new ArrayList<>();
        FileUtil.walkFiles(FileUtil.file(rootPath, TEMP_PATH), file -> {
            count.getAndIncrement();
            String fname = file.getName().toLowerCase();
            if (!fname.endsWith(".zip")
                    && !fname.endsWith(".rar")
                    && !fname.endsWith(".7z")) {
                FileUtil.del(file);
                return;
            }
            String fmd5 = DigestUtil.md5Hex(file);
            if (mapper.exists(new LambdaQueryWrapper<>(FileDetail.class)
                    .eq(FileDetail::getFileMd5, fmd5))) {
                FileUtil.del(file);
                return;
            }
            FileDetail fd = new FileDetail();
            fd.setOriginalFileName(file.getName());
            fd.setFileAliasName(file.getName());
            fd.setFileMd5(fmd5);
            File frm = FileUtil.rename(file, IdUtil.getSnowflakeNextIdStr(), true, false);
            File target = FileUtil.file(rootPath, LEVELS, frm.getName());
            FileUtil.move(frm, target, false);
            fd.setFilePath(FileUtil.subPath(rootPath, target));
            fd.setFileName(target.getName());
            fd.setFileSize(FileUtil.readableFileSize(target));
            fd.setFileUploadTime(LocalDateTime.now());
            fdl.add(fd);
            if (fdl.size() >= 100) {
                mapper.insert(fdl);
                fdl.clear();
            }
        });
        if (!fdl.isEmpty()) {
            mapper.insert(fdl);
            fdl.clear();
        }
        FileUtil.cleanEmpty(FileUtil.file(rootPath, TEMP_PATH));
        log.info("整合完成");
        FileUtil.mkdir(FileUtil.file(rootPath, LEVELS));
        return count.get();
    }

}
