package com.livgo.dtproxy;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.livgo.mapper.FileDetailMapper;
import com.livgo.po.FileDetail;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.erupt.annotation.fun.DataProxy;
import xyz.erupt.core.exception.EruptApiErrorTip;
import xyz.erupt.core.view.EruptApiModel;

import java.io.File;
import java.time.LocalDateTime;

import static com.livgo.utils.PathUtil.getJarPath;

@Service
public class FileDetailProxy implements DataProxy<FileDetail> {

    @Resource
    private FileDetailMapper mapper;

    private static final String LEVELS = "levels";

    @Value("${run.upload-path}")
    private String UPLOAD;

    private static String rootPath = getJarPath();

    @Override
    public void beforeAdd(FileDetail fileDetail) {
        String tempPath = fileDetail.getTempPath();
        System.out.println(fileDetail);
        File file = FileUtil.file(UPLOAD, tempPath);

        String fmd5 = DigestUtil.md5Hex(file);
        if (mapper.exists(
                new LambdaQueryWrapper<>(FileDetail.class)
                        .eq(FileDetail::getFileMd5, fmd5)
        )) {
            FileUtil.del(file);
            throw new EruptApiErrorTip("文件已存在", EruptApiModel.PromptWay.MESSAGE);
        }

        fileDetail.setOriginalFileName(file.getName());
        fileDetail.setFileAliasName(
                fileDetail.getFileAliasName().isEmpty() ?
                        file.getName() :
                        fileDetail.getFileAliasName()
        );
        fileDetail.setFileSize(FileUtil.readableFileSize(file));
        fileDetail.setFileMd5(fmd5);
        File fd = FileUtil.rename(file, IdUtil.getSnowflakeNextIdStr(), true, false);
        File target = FileUtil.file(rootPath, LEVELS, fd.getName());
        FileUtil.move(fd, target, false);
        fileDetail.setFilePath(FileUtil.subPath(rootPath, target));
        fileDetail.setFileName(target.getName());
        fileDetail.setFileUploadTime(LocalDateTime.now());
    }

    @Override
    public void afterDelete(FileDetail fileDetail) {
        System.out.println(fileDetail.toString());
        String filePath = fileDetail.getFilePath();
        FileUtil.del(FileUtil.file(rootPath, filePath));
    }
}
