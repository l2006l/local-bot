package com.livgo.plugins;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static com.livgo.plugins.FilePluginInit.LEVELS;
import static com.livgo.plugins.FilePluginInit.TEMP_FILE;
import static com.livgo.utils.ProgressUtil.getProgress;
import static com.livgo.utils.ResultMsgUtil.RUN_LOCATION;
import static com.livgo.utils.doFilesUtil.doFiles;

/**
 * standard模式的同步部分
 *
 * @author livlong
 * @date 2026-03-18
 */
@Shiro
@Component
@Slf4j
@ConditionalOnProperty(
        prefix = "run",
        name = "mode",
        havingValue = "standard",
        matchIfMissing = true
)
public class FilePluginStandard {

    /**
     *
     */
    @Value("${run.pwd}")
    private String runPwd;

    /**
     * 同步机器人所在群聊的全部文件（同步压缩包）
     * 如果需要同步其他文件请自行更改 if 判断条件
     *
     * @param bot     机器人
     * @param event   活动
     * @param matcher 匹配器
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^全部同步 (.*?)$")
    public void allSync(Bot bot, PrivateMessageEvent event, Matcher matcher) {

        String pwd = matcher.group(1).strip();

        if (!pwd.equals(runPwd)) {
            String errMsg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("密码错误\n")
                    .build();
            bot.sendPrivateMsg(event.getUserId(), errMsg, false);
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

        if (PermissionUtil.isAdmin(event.getUserId())) {
            return;
        }

        Long groupId = Long.valueOf(matcher.group(1));

        String needDel = matcher.group(2).strip();

        AtomicInteger i = new AtomicInteger();

        StreamProgress progress = getProgress();

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
    }

    /**
     * 对levels中的文件进行去重
     *
     * @param bot   机器人
     * @param event 活动
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "去重")
    public void offlineSync(Bot bot, PrivateMessageEvent event) {

        if (PermissionUtil.inGroupList(event.getUserId())) {
            return;
        }

        AtomicInteger all = new AtomicInteger();
        AtomicInteger res = new AtomicInteger();
        int del = 0;

        // 通过大小筛选
        Map<Long, List<File>> sizeMap = new HashMap<>();
        FileUtil.walkFiles(FileUtil.file(RUN_LOCATION, LEVELS), f -> {
            if (f.isFile()) {
                sizeMap.computeIfAbsent(f.length(), k -> new ArrayList<>()).add(f);
                all.getAndIncrement();
            }
        });


        Map<String, List<File>> md5Map = new HashMap<>();
        byte[] buffer = new byte[64 * 1024];

        // 通过前64k的md5筛选
        for (Map.Entry<Long, List<File>> entry : sizeMap.entrySet()) {
            List<File> sameFile = entry.getValue();
            if (sameFile.size() < 2) {
                continue;
            }
            for (File f : sameFile) {
                String md5;
                try (FileInputStream fis = new FileInputStream(f)) {
                    int read = fis.read(buffer);
                    if (read > 0) {
                        md5 = DigestUtil.md5Hex(Arrays.copyOf(buffer, read));
                    } else {
                        md5 = "d41d8cd98f00b204e9800998ecf8427e";
                    }
                } catch (Exception e) {
                    System.err.println("读取采样失败，跳过文件: " + f + "，原因: " + e.getMessage());
                    continue;
                }
                String key = entry.getKey() + ":" + md5;
                md5Map.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
            }
        }


        for (List<File> entry : md5Map.values()) {
            if (entry.size() < 2) continue;

            Map<String, List<File>> resMap = new HashMap<>();
            for (File f : entry) {
                String md5 = DigestUtil.md5Hex(f);
                resMap.computeIfAbsent(md5, k -> new ArrayList<>()).add(f);
            }
            for (List<File> files : resMap.values()) {
                if (files.size() < 2) continue;
                for (int i = 1; i < files.size(); i++) {
                    FileUtil.del(files.get(i));
                    del++;
                }
            }
        }

        FileUtil.walkFiles(FileUtil.file(RUN_LOCATION, LEVELS), f -> {
            if (f.isFile()) {
                res.getAndIncrement();
            }
        });

        String msg = MsgUtils.builder()
                .text("文件去重完成\n")
                .text("共 " + all + " 个文件需要去重\n")
                .text("删除 " + del + " 个文件\n")
                .text("当前共计 " + res + " 个文件\n")
                .build();
        bot.sendPrivateMsg(event.getUserId(), msg, false);

    }

    /**
     * 自动同步文件
     *
     * @param bot   机器人
     * @param event 活动
     */
    @GroupUploadNoticeHandler
    public void groupUpdate(Bot bot, GroupUploadNoticeEvent event) {
        if (PermissionUtil.isAutoUpload(event.getGroupId())) {
            return;
        }
        if (!event.getFile().getName().endsWith(".zip")
                && !event.getFile().getName().endsWith(".rar")
                && !event.getFile().getName().endsWith(".7z")) {
            log.info("文件格式错误");
            return;
        }
        File tmp = FileUtil.file(RUN_LOCATION, TEMP_FILE);
        File tempFile = FileUtil.file(tmp, event.getFile().getName());
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

            File levelMd = FileUtil.file(RUN_LOCATION, LEVELS, event.getGroupId().toString());
            FileUtil.mkdir(levelMd);
            File dest = FileUtil.file(levelMd, tempFile.getName());
            int count = 1;
            while (FileUtil.exist(dest)) {
                String mainName = FileNameUtil.mainName(tempFile);
                String extName = FileNameUtil.extName(tempFile);

                String newName = StrUtil.format("{}({}).{}", mainName, count, extName);
                dest = FileUtil.file(levelMd, newName);
                count++;
            }
            FileUtil.move(tempFile, dest, false);
            if (levelMd.exists() && levelMd.isDirectory()) {
                log.info("文件已存在");
                FileUtil.del(tempFile);
                String resMsg = MsgUtils.builder()
                        .text("文件 ")
                        .text(event.getFile().getName())
                        .text(" 已存在")
                        .build();
                bot.sendGroupMsg(event.getGroupId(), resMsg, false);
                return;
            }
            FileUtil.mkdir(levelMd);
            FileUtil.move(tempFile, levelMd, true);
            String resMsg = MsgUtils.builder()
                    .text("文件 ")
                    .text(event.getFile().getName())
                    .text("同步成功 ")
                    .build();
            bot.sendGroupMsg(event.getGroupId(), resMsg, false);
        } catch (Exception e) {
            log.error("文件同步失败", e);
        } finally {
            FileUtil.del(tempFile);
        }
    }

}
