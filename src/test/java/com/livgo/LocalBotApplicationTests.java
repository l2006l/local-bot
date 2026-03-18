package com.livgo;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

//@SpringBootTest
class LocalBotApplicationTests {

    @Test
    void contextLoads() {

        AtomicInteger a = new AtomicInteger(0);
        add(a);
        System.out.println(a.get());
    }

    void add(AtomicInteger a) {
        for (int i = 0; i < 100000; i++) {
            a.getAndIncrement();
        }
    }

}
