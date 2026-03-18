package com.livgo.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;

/**
 * 进度条工具类
 *
 * @author livlong
 * @date 2026-03-18
 */
public class ProgressUtil {

    public static StreamProgress getProgress() {
        return new StreamProgress() {
            private long startTime;

            @Override
            public void start() {
                System.out.println("开始下载...");
                startTime = System.currentTimeMillis();
            }

            @Override
            public void progress(long total, long progressSize) {
                // total可能为-1（服务器未返回大小时）
                if (total > 0) {
                    double percent = (progressSize * 100.0) / total;
                    // 使用 \r 实现行内刷新，显示更友好的进度
                    System.out.printf("\r下载进度: %6.2f%% (%s/%s)",
                            percent,
                            FileUtil.readableFileSize(progressSize),
                            FileUtil.readableFileSize(total));
                } else {
                    System.out.printf("\r已下载: %s ", FileUtil.readableFileSize(progressSize));
                }
            }

            @Override
            public void finish() {
                long costTime = System.currentTimeMillis() - startTime;
                System.out.println("耗时: " + costTime / 1000.0 + " 秒");
            }
        };
    }

}
