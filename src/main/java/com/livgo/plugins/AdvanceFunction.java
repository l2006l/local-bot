package com.livgo.plugins;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.livgo.mapper.FileDetailMapper;
import com.livgo.po.FileDetail;
import com.livgo.utils.PermissionUtil;
import com.mikuac.shiro.annotation.GroupUploadNoticeHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GroupFilesResp;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import com.mikuac.shiro.dto.event.notice.GroupUploadNoticeEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static com.livgo.plugins.FilePluginInit.LEVELS;
import static com.livgo.plugins.FilePluginInit.TEMP_FILE;
import static com.livgo.utils.ExecutorUtil.getExecutor;
import static com.livgo.utils.PathUtil.getJarPath;
import static com.livgo.utils.ProgressUtil.getProgress;

/**
 * standard模式的同步部分
 *
 * @author livlong
 * @date 2026-03-18
 */
@Shiro
@Component
@Slf4j
public class AdvanceFunction {

    @Resource
    private FileDetailMapper fileDetailMapper;

    /**
     * 同步机器人所在群聊的全部文件（同步压缩包）
     * 如果需要同步其他文件请自行更改 if 判断条件
     *
     * @param bot   机器人
     * @param event 活动
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "全部同步")
    public void allSync(Bot bot, PrivateMessageEvent event) {

        if (!PermissionUtil.isAdmin(event.getUserId())) {
            return;
        }

        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }

        ThreadUtil.execute(() -> {

            AtomicInteger i = new AtomicInteger();

            bot.getGroupList().getData().forEach(g -> {
                List<GroupFilesResp.Files> files = Optional.ofNullable(bot.getGroupRootFiles(g.getGroupId()))
                        .map(ActionData::getData)
                        .map(GroupFilesResp::getFiles)
                        .orElse(new ArrayList<>());

                Optional.ofNullable(bot.getGroupRootFiles(g.getGroupId()))
                        .map(ActionData::getData)
                        .map(GroupFilesResp::getFolders)
                        .ifPresent(folders -> folders.forEach(f -> {
                            Optional.ofNullable(bot.getGroupFilesByFolder(g.getGroupId(), f.getFolderId()))
                                    .map(ActionData::getData)
                                    .map(GroupFilesResp::getFiles)
                                    .ifPresent(files::addAll);
                        }));
                doFiles(bot, files, i, g.getGroupId());
                bot.sendPrivateMsg(event.getUserId(), "群" + g.getGroupId() + "完成同步，共 " + i + " 个文件", false);
                i.set(0);
            });
            bot.sendPrivateMsg(event.getUserId(), "全部群聊完成同步", false);
        });
    }

    /**
     * 同步某个群聊的文件
     *
     * @param bot     机器人
     * @param event   活动
     * @param matcher 匹配器
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^同步群聊 (.\\d+)$")
    public void groupUpdate(Bot bot, PrivateMessageEvent event, Matcher matcher) {

        if (!PermissionUtil.isAdmin(event.getUserId())) {
            return;
        }

        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }

        Long groupId = Long.valueOf(matcher.group(1));

        String needDel = matcher.group(2).strip();

        AtomicInteger i = new AtomicInteger();

        List<GroupFilesResp.Files> files = Optional.ofNullable(bot.getGroupRootFiles(groupId))
                .map(ActionData::getData)
                .map(GroupFilesResp::getFiles)
                .orElse(new ArrayList<>());
        Optional.ofNullable(bot.getGroupRootFiles(groupId))
                .map(ActionData::getData)
                .map(GroupFilesResp::getFolders)
                .ifPresent(folders -> folders.forEach(f -> {
                    Optional.ofNullable(bot.getGroupFilesByFolder(groupId, f.getFolderId()))
                            .map(ActionData::getData)
                            .map(GroupFilesResp::getFiles)
                            .ifPresent(files::addAll);
                }));
        doFiles(bot, files, i, groupId);
        if (needDel.equals("1")) {
            files.forEach(f -> {
                bot.deleteGroupFile(groupId, f.getFileId(), f.getBusId());
            });
        }
        bot.sendPrivateMsg(event.getUserId(), "群" + groupId + "完成同步，共 " + i.get() + " 个文件", false);
        IdUtil.nanoId();
    }

    /**
     * 自动同步文件
     *
     * @param bot   机器人
     * @param event 活动
     */
    @GroupUploadNoticeHandler
    public void groupUpdate(Bot bot, GroupUploadNoticeEvent event) {

        if (!PermissionUtil.isAutoUpload(event.getGroupId())
                && !PermissionUtil.isAutoUpload(event.getUserId())) {
            return;
        }
        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }
        if (!event.getFile().getName().endsWith(".zip")
                && !event.getFile().getName().endsWith(".rar")
                && !event.getFile().getName().endsWith(".7z")) {
            log.info("文件格式错误");
            return;
        }
        FileDetail detail = new FileDetail();
        detail.setOriginalFileName(event.getFile().getName());
        detail.setFileAliasName(event.getFile().getName());
        detail.setOriginalGroupId(event.getGroupId());

        File tempFile = FileUtil.file(getJarPath(),
                TEMP_FILE,
                IdUtil.getSnowflakeNextIdStr(),
                event.getFile().getName());
        try {
            String url = bot.getGroupFileUrl(event.getGroupId(),
                            event.getFile().getId(),
                            event.getFile().getBusid().intValue())
                    .getData()
                    .getUrl();
            HttpRequest.get(url).setFollowRedirects(true)
                    .setReadTimeout(30000)
                    .setConnectionTimeout(30000)
                    .execute(true)
                    .writeBody(tempFile);

            String fmd5 = DigestUtil.md5Hex(tempFile);
            if (fileDetailMapper.exists(
                    new LambdaQueryWrapper<>(FileDetail.class).eq(
                            FileDetail::getFileMd5, fmd5
                    )
            )) {
                log.info("文件 {} 已存在", event.getFile().getName());
                bot.sendGroupMsg(event.getGroupId(), "文件 " + event.getFile().getName() + " 已存在", false);
                return;
            }
            File file = FileUtil.rename(tempFile, IdUtil.getSnowflakeNextIdStr(), true, false);
            File target = FileUtil.file(getJarPath(), LEVELS, file.getName());
            FileUtil.move(file, target, false);

            detail.setFileName(target.getName());
            detail.setFileMd5(fmd5);
            detail.setFileSize(FileUtil.readableFileSize(target));
            detail.setFilePath(FileUtil.subPath(getJarPath(), target));
            detail.setFileUploadTime(LocalDateTime.now());
            fileDetailMapper.insert(detail);
            String resMsg = MsgUtils.builder()
                    .text("文件 ")
                    .text(event.getFile().getName())
                    .text("同步成功 ")
                    .build();
            bot.sendGroupMsg(event.getGroupId(), resMsg, false);
        } catch (Exception e) {
            log.error("文件同步失败", e);
        } finally {
            FileUtil.del(tempFile.getParentFile());
        }

    }

    public void doFiles(Bot bot, List<GroupFilesResp.Files> files, AtomicInteger i, Long groupId) {
        StreamProgress progress = getProgress();
        ThreadPoolExecutor exec = getExecutor();
        files.forEach(f -> {
            if (!isZip(f)) {
                return;
            }
            exec.submit(() -> {
                FileDetail detail = new FileDetail();
                detail.setOriginalFileName(f.getFileName());
                detail.setFileAliasName(f.getFileName());
                detail.setOriginalGroupId(groupId);

                File tempFile = FileUtil.file(getJarPath(),
                        TEMP_FILE,
                        IdUtil.getSnowflakeNextIdStr(),
                        f.getFileName());
                try {
                    String url = bot.getGroupFileUrl(groupId, f.getFileId(), f.getBusId()).getData().getUrl();
                    HttpRequest.get(url).setFollowRedirects(true)
                            .setReadTimeout(30000)
                            .setConnectionTimeout(30000)
                            .execute(true)
                            .writeBody(tempFile, progress);

                    String fmd5 = DigestUtil.md5Hex(tempFile);
                    if (fileDetailMapper.exists(
                            new LambdaQueryWrapper<>(FileDetail.class).eq(
                                    FileDetail::getFileMd5, fmd5
                            )
                    )) {
                        log.info("文件 {} 已存在", f.getFileName());
                        return;
                    }
                    File file = FileUtil.rename(tempFile, IdUtil.getSnowflakeNextIdStr(), true, false);
                    File target = FileUtil.file(getJarPath(), LEVELS, file.getName());
                    FileUtil.move(file, target, false);

                    detail.setFileName(target.getName());
                    detail.setFileMd5(fmd5);
                    detail.setFileSize(FileUtil.readableFileSize(target));
                    detail.setFilePath(FileUtil.subPath(getJarPath(), target));
                    detail.setFileUploadTime(LocalDateTime.now());
                    fileDetailMapper.insert(detail);
                } catch (Exception e) {
                    log.error("同步文件失败", e);
                    FileUtil.del(tempFile);
                    return;
                } finally {
                    FileUtil.del(tempFile.getParentFile());
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
