package com.livgo.plugins;

import cn.hutool.core.io.FileUtil;
import com.livgo.utils.PathUtil;
import jakarta.annotation.PostConstruct;
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

        String rootPath = PathUtil.getJarPath();

        File tempFile = FileUtil.file(rootPath, TEMP_FILE);

        File levels = FileUtil.file(rootPath, LEVELS);

        FileUtil.mkdir(tempFile);

        FileUtil.mkdir(levels);
    }

}
