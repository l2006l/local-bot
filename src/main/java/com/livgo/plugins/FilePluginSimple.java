package com.livgo.plugins;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
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
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static com.livgo.plugins.FilePluginInit.LEVELS;
import static com.livgo.plugins.FilePluginInit.TEMP_FILE;
import static com.livgo.utils.ResultMsgUtil.RUN_LOCATION;
import static com.livgo.utils.doFilesUtil.doFiles;

/**
 * simple模式的同步功能
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
        havingValue = "simple",
        matchIfMissing = true
)
public class FilePluginSimple {

    /**
     * 配置文件中配置的密码
     */
    @Value("${run.pwd}")
    private String runPwd;


    /**
     *
     *
     * @param bot   机器人
     * @param event 活动
     */
    @GroupUploadNoticeHandler
    public void groupUpdate(Bot bot, GroupUploadNoticeEvent event) {
        log.info("监听到群文件上传");
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
            String fmd5 = DigestUtil.md5Hex(tempFile);
            File levelMd = FileUtil.file(RUN_LOCATION, LEVELS, fmd5);
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

        bot.sendPrivateMsg(event.getUserId(), "群" + groupId + "完成同步，共 " + i.get() + " 个文件", false);
    }


    /**
     * 将文件按照相同的格式从 tempFile 移动到 levels
     * 用于统一格式（含去重）
     *
     * @param bot   机器人
     * @param event 活动
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "离线同步")
    public void offlineSync(Bot bot, PrivateMessageEvent event) {

        if (PermissionUtil.isAdmin(event.getUserId())) {
            return;
        }

        AtomicInteger all = new AtomicInteger(0);
        FileUtil.walkFiles(FileUtil.file(RUN_LOCATION, TEMP_FILE), f -> {
            all.getAndIncrement();
        });
        AtomicInteger del = new AtomicInteger(0);
        AtomicInteger res = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);

        ProgressBar pb = new ProgressBarBuilder()
                .setTaskName("当前进度")
                .setInitialMax(all.get())
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .build()
                .maxHint(all.get());
        FileUtil.walkFiles(FileUtil.file(RUN_LOCATION, TEMP_FILE), f -> {
            if (f == null || !f.isFile()) {
                return;
            }
            pb.setExtraMessage("处理中……");
            String fmd5 = DigestUtil.md5Hex(f);
            if (FileUtil.file(RUN_LOCATION, LEVELS, fmd5).exists()
                    && FileUtil.file(RUN_LOCATION, LEVELS, fmd5).isDirectory()) {
                FileUtil.del(f);
                del.getAndIncrement();
                pb.step();
                return;
            }
            File dir = FileUtil.mkdir(FileUtil.file(RUN_LOCATION, LEVELS, fmd5));
            FileUtil.move(f, dir, true);
            success.getAndIncrement();
            pb.step();
        });

        synchronized (pb) {
            pb.stepTo(all.get());
            pb.setExtraMessage("处理完成");
            pb.refresh();
        }
        pb.close();

        FileUtil.walkFiles(FileUtil.file(RUN_LOCATION, LEVELS), f -> {
            res.getAndIncrement();
        });
        try {
            Files.walk(Path.of(RUN_LOCATION, TEMP_FILE))
                    .sorted(Comparator.reverseOrder())
                    .filter(Files::isDirectory)
                    .filter(FileUtil::isDirEmpty)
                    .forEach(FileUtil::del);
        } catch (Exception e) {
            log.error("删除空文件夹异常", e);
        }

        String msg = MsgUtils.builder()
                .text("离线同步完成\n")
                .text("共 " + all + " 个文件需要同步\n")
                .text("成功同步 " + success + " 个文件\n")
                .text("删除 " + del + " 个文件\n")
                .text("当前共计 " + res + " 个文件\n")
                .build();
        bot.sendPrivateMsg(event.getUserId(), msg, false);

    }

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
                log.info("开始同步群 {}", g.getGroupId());
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

}
