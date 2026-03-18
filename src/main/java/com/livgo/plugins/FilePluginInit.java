package com.livgo.plugins;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 文件插件 init
 *
 * @author livlong
 * @date 2026-03-18
 */
@Component
public class FilePluginInit {

    /**
     * 运行位置
     */
    @Value("${run.location}")
    public String runLocation;

    /**
     * 临时文件目录
     */
    public static final String TEMP_FILE = "tempFile";

    /**
     * 最终文件目录
     */
    public static final String LEVELS = "levels";

    /**
     * 初始化方法
     *
     */
    @PostConstruct
    public void init() {

        File tempFile = FileUtil.file(runLocation, TEMP_FILE);

        File levels = FileUtil.file(runLocation, LEVELS);

        File message = FileUtil.file(runLocation, "message.txt");

        FileUtil.touch(message);

        FileUtil.mkdir(tempFile);

        FileUtil.mkdir(levels);
    }

}
