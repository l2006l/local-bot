package com.livgo.plugins;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.livgo.utils.PermissionUtil;
import com.livgo.utils.ResultMsgUtil;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

@Shiro
@Component
@Slf4j
public class BaseFunction {

    @Value("${run.location}")
    private String runLocation;

    @Value("${run.pageSize}")
    private Integer pageSize;

    private static final String LEVELS = "levels";

    private static final TimedCache<Long, List<File>> CACHE = new TimedCache<>(1000 * 60 * 10);

    private static final TimedCache<Long, Integer> PAGE_CACHE = new TimedCache<>(1000 * 60 * 10);

    /**
     * 搜索文件
     *
     * @param bot
     * @param event
     * @param matcher
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^搜索\s*(.*)?$")
    public void search(Bot bot, GroupMessageEvent event, Matcher matcher) {

        // 判断群是否在群组列表内
        if (PermissionUtil.inGroupList(event.getGroupId())) {
            return;
        }

        // 获取搜索词
        String keyword = matcher.group(1).strip();


        // 判断搜索词是否为空
        if (keyword.isEmpty()) {
            MsgUtils msg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("请输入搜索关键字\n")
                    .text("---------------\n");

            String errMsg = ResultMsgUtil.msgWithNotice(msg);

            bot.sendGroupMsg(event.getGroupId(), errMsg, false);

            return;
        }

        List<File> levels = new ArrayList<>();

        // 遍历本地目录搜索
        FileUtil.walkFiles(FileUtil.file(runLocation, LEVELS), f -> {
            if (f.getName().contains(keyword) || StrUtil.containsIgnoreCase(f.getName(), keyword)) {
                levels.add(f);
            }
        });


        // 判断搜索结果是否为空
        if (levels.isEmpty()) {
            MsgUtils msg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("没有搜到相关内容\n")
                    .text("---------------\n");

            String errMsg = ResultMsgUtil.msgWithNotice(msg);

            bot.sendGroupMsg(event.getGroupId(), errMsg, false);

            return;
        }

        // 统计目录下所有文件数
        AtomicInteger all = new AtomicInteger(0);
        FileUtil.walkFiles(FileUtil.file(runLocation, LEVELS), f -> {
            all.getAndIncrement();
        });

        // 计算总页数
        int totalPage = PageUtil.totalPage(levels.size(), pageSize);

        // 分页
        List<File> page = ListUtil.page(0, pageSize, levels);

        Integer pageNum = 0;

        String resMsg = ResultMsgUtil.fileListMsg(page);

        CACHE.put(event.getUserId(), levels);

        PAGE_CACHE.put(event.getUserId(), pageNum);

        // 构建消息
        String resultMsg = ResultMsgUtil.pageMsg(event.getMessageId(),
                resMsg,
                all.get(),
                levels.size(),
                totalPage,
                pageNum);

        bot.sendGroupMsg(event.getGroupId(), resultMsg, false);

    }

    /**
     * 上一页
     * @param bot
     * @param event
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "上一页")
    public void prevPage(Bot bot, GroupMessageEvent event) {

        if (PermissionUtil.inGroupList(event.getGroupId())) {
            return;
        }

        if (CACHE.get(event.getUserId()) == null) {
            return;
        }

        Integer pageNum = PAGE_CACHE.get(event.getUserId());

        if (pageNum - 1 < 0) {
            log.info("用户 {} 已是第一页", event.getUserId());
            MsgUtils msg = MsgUtils.builder()
                    .text("已经是第一页了\n")
                    .text("---------------\n");
            String resMsg = ResultMsgUtil.msgWithNotice(msg);
            bot.sendGroupMsg(event.getGroupId(), resMsg, false);
            return;
        }

        AtomicInteger all = new AtomicInteger(0);
        FileUtil.walkFiles(FileUtil.file(runLocation, LEVELS), f -> {
            all.getAndIncrement();
        });

        PAGE_CACHE.put(event.getUserId(), pageNum - 1);

        int totalPage = PageUtil.totalPage(CACHE.get(event.getUserId()).size(), pageSize);

        BuildMsgAndSend(bot, event, all, totalPage, pageNum);
    }

    /**
     * 下一页
     * @param bot
     * @param event
     */

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "下一页")
    public void nextPage(Bot bot, GroupMessageEvent event) {

        if (PermissionUtil.inGroupList(event.getGroupId())) {
            return;
        }

        if (CACHE.get(event.getUserId()) == null) {
            return;
        }

        AtomicInteger all = new AtomicInteger(0);
        FileUtil.walkFiles(FileUtil.file(runLocation, LEVELS), f -> {
            all.getAndIncrement();
        });

        int totalPage = PageUtil.totalPage(CACHE.get(event.getUserId()).size(), pageSize);

        Integer pageNum = PAGE_CACHE.get(event.getUserId());

        if (pageNum + 1 >= totalPage) {
            log.info("用户 {} 已是最后一页", event.getUserId());
            MsgUtils msg = MsgUtils.builder()
                    .text("已是最后一页了\n")
                    .text("---------------\n");
            String resMsg = ResultMsgUtil.msgWithNotice(msg);
            bot.sendGroupMsg(event.getGroupId(), resMsg, false);
        }

        PAGE_CACHE.put(event.getUserId(), pageNum + 1);

        BuildMsgAndSend(bot, event, all, totalPage, pageNum);

    }

    /**
     * 构建消息并发送
     * @param bot
     * @param event
     * @param all
     * @param totalPage
     * @param pageNum
     */

    private void BuildMsgAndSend(Bot bot,
                                 GroupMessageEvent event,
                                 AtomicInteger all,
                                 int totalPage,
                                 Integer pageNum) {
        List<File> levels = CACHE.get(event.getUserId());

        List<File> page = ListUtil.page(pageNum, pageSize, levels);

        String resMsg = ResultMsgUtil.fileListMsg(page);

        String resultMsg = ResultMsgUtil.pageMsg(event.getMessageId(),
                resMsg,
                all.get(),
                levels.size(),
                totalPage,
                pageNum);

        bot.sendGroupMsg(event.getGroupId(), resultMsg, false);
    }

    /**
     * 下载文件
     *
     * @param bot
     * @param event
     * @param matcher
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^下载\s*(\\d+)$")
    public void download(Bot bot, GroupMessageEvent event, Matcher matcher) {

        if (PermissionUtil.inGroupList(event.getGroupId())) {
            return;
        }

        int rowIndex = Integer.parseInt(matcher.group(1).strip());

        List<File> levels = CACHE.get(event.getUserId());

        if (levels == null) {
            log.info("用户 {} 未搜索文件", event.getUserId());
            return;
        }

        List<File> page = ListUtil.page(PAGE_CACHE.get(event.getUserId()), pageSize, levels);

        if (rowIndex < 0 || rowIndex > page.size()) {
            MsgUtils msg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("请输入正确的序号\n")
                    .text("---------------\n");

            String errMsg = ResultMsgUtil.msgWithNotice(msg);

            bot.sendGroupMsg(event.getGroupId(), errMsg, false);

            return;
        }

        File level = page.get(rowIndex - 1);
        CACHE.remove(event.getUserId());
        PAGE_CACHE.remove(event.getUserId());
        bot.uploadGroupFile(event.getGroupId(), level.getAbsolutePath(), level.getName());

    }

}
