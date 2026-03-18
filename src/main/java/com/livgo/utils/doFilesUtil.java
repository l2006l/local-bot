package com.livgo.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.response.GroupFilesResp;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.livgo.plugins.FilePluginInit.LEVELS;
import static com.livgo.plugins.FilePluginInit.TEMP_FILE;
import static com.livgo.utils.ExecutorUtil.getExecutor;
import static com.livgo.utils.ProgressUtil.getProgress;
import static com.livgo.utils.ResultMsgUtil.RUN_LOCATION;

/**
 * 执行获取到qq文件列表后的操作
 *
 * @author livlong
 * @date 2026-03-18
 */
@Slf4j
public class doFilesUtil {

    public static void doFiles(Bot bot, List<GroupFilesResp.Files> files, AtomicInteger i, Long groupId) {
        StreamProgress progress = getProgress();
        ThreadPoolExecutor exec = getExecutor();
        files.forEach(f -> {
            if (!isZip(f)) {
                return;
            }
            exec.submit(() -> {
                File tmp = FileUtil.file(RUN_LOCATION, TEMP_FILE);
                File tempFile = FileUtil.file(tmp, f.getFileName());
                try {
                    String url = bot.getGroupFileUrl(groupId, f.getFileId(), f.getBusId()).getData().getUrl();
                    HttpRequest.get(url).setFollowRedirects(true)
                            .setReadTimeout(30000)
                            .setConnectionTimeout(30000)
                            .execute(true)
                            .writeBody(tempFile, progress);

                    String fmd5 = DigestUtil.md5Hex(tempFile);
                    File levelMd = FileUtil.file(RUN_LOCATION, LEVELS, fmd5);
                    if (levelMd.exists() && levelMd.isDirectory()) {
                        FileUtil.del(levelMd);
                        log.info("文件 {} 已存在", f.getFileName());
                    }
                    FileUtil.mkdir(levelMd);
                    FileUtil.move(tempFile, levelMd, true);
                } catch (Exception e) {
                    log.error("同步文件失败", e);
                    FileUtil.del(tempFile);
                    return;
                } finally {
                    FileUtil.del(tempFile);
                }
                i.getAndIncrement();
            });
        });

        exec.shutdown();
        try {
            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean isZip(GroupFilesResp.Files f) {
        return f.getFileName().endsWith(".zip")
                || f.getFileName().endsWith(".rar")
                || f.getFileName().endsWith(".7z");
    }

}
