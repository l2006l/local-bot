package com.livgo.utils;

import cn.hutool.core.thread.ExecutorBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工具类
 *
 * @author livlong
 * @date 2026-03-18
 */
public class ExecutorUtil {

    //创建线程池
    public static ThreadPoolExecutor getExecutor() {
        int core = Runtime.getRuntime().availableProcessors();
        return ExecutorBuilder.create()
                .setCorePoolSize(core * 2)
                .setMaxPoolSize(core * 4)
                .setWorkQueue(new LinkedBlockingQueue<>(200))
                .setKeepAliveTime(60L, TimeUnit.SECONDS)
                .setHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .build();
    }

}
