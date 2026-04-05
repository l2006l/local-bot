package com.livgo.plugins;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.livgo.mapper.FileDetailMapper;
import com.livgo.po.FileDetail;
import com.livgo.utils.PermissionUtil;
import com.livgo.utils.ResultMsgUtil;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.regex.Matcher;

import static com.livgo.utils.PathUtil.getJarPath;

@Shiro
@Component
@Slf4j
public class UserFunction {

    private static String rootPath = getJarPath();

    @Value("${run.pageSize:10}")
    private Integer pageSize;

    private static final String LEVELS = "levels";

    private static final TimedCache<Long, Page<FileDetail>> CACHE = new TimedCache<>(1000 * 60 * 2);

    private static final TimedCache<Long, String> KEY_CACHE = new TimedCache<>(1000 * 60 * 2);

    @Resource
    private FileDetailMapper fileDetailMapper;

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
        if (!PermissionUtil.inGroupList(event.getGroupId())) {
            return;
        }

        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }

        // 获取搜索词
        String keyword = matcher.group(1).strip();

        Page<FileDetail> pg = new Page<>(1, pageSize);
        LambdaQueryWrapper<FileDetail> query = new LambdaQueryWrapper<>();
        query.apply("LOWER(file_alias_name) LIKE LOWER(CONCAT('%',{0},'%'))", keyword);
        Page<FileDetail> fpl = fileDetailMapper.selectPage(pg, query);

        // 判断搜索结果是否为空
        if (fpl.getTotal() == 0) {
            MsgUtils msg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("没有搜到相关内容\n")
                    .text("---------------\n");

            String errMsg = ResultMsgUtil.msgWithNotice(msg);

            bot.sendGroupMsg(event.getGroupId(), errMsg, false);

            return;
        }

        // 统计目录下所有文件数
        Long all = fileDetailMapper.selectCount(null);

        String resMsg = ResultMsgUtil.fileListMsg(fpl);

        // 构建消息
        String resultMsg = ResultMsgUtil.pageMsg(event.getMessageId(),
                resMsg,
                all,
                fpl.getTotal(),
                fpl.getPages(),
                fpl.getCurrent());

        CACHE.put(event.getUserId(), fpl);
        KEY_CACHE.put(event.getUserId(), keyword);

        bot.sendGroupMsg(event.getGroupId(), resultMsg, false);

    }

    /**
     * 上一页
     *
     * @param bot
     * @param event
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "上一页")
    public void prevPage(Bot bot, GroupMessageEvent event) {

        if (!PermissionUtil.inGroupList(event.getGroupId())) {
            return;
        }

        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }

        if (CACHE.get(event.getUserId()) == null) {
            return;
        }

        Page<FileDetail> files = CACHE.get(event.getUserId());
        String keyword = KEY_CACHE.get(event.getUserId());

        if (!files.hasPrevious()) {
            log.info("用户 {} 已是第一页", event.getUserId());
            MsgUtils msg = MsgUtils.builder()
                    .text("已经是第一页了\n")
                    .text("---------------\n");
            String resMsg = ResultMsgUtil.msgWithNotice(msg);
            bot.sendGroupMsg(event.getGroupId(), resMsg, false);
            return;
        }

        Long all = fileDetailMapper.selectCount(null);

        Page<FileDetail> pg = new Page<>(files.getCurrent() - 1, pageSize);
        pageQuery(bot, event, keyword, all, pg);

    }

    public void pageQuery(Bot bot, GroupMessageEvent event, String keyword, Long all, Page<FileDetail> pg) {
        LambdaQueryWrapper<FileDetail> query = new LambdaQueryWrapper<>();
        query.like(FileDetail::getFileAliasName, keyword);
        Page<FileDetail> fpl = fileDetailMapper.selectPage(pg, query);

        String resMsg = ResultMsgUtil.fileListMsg(fpl);

        String resultMsg = ResultMsgUtil.pageMsg(event.getMessageId(),
                resMsg,
                all,
                fpl.getTotal(),
                fpl.getPages(),
                fpl.getCurrent());

        CACHE.remove(event.getUserId());
        CACHE.put(event.getUserId(), fpl);
        KEY_CACHE.remove(event.getUserId());
        KEY_CACHE.put(event.getUserId(), keyword);

        bot.sendGroupMsg(event.getGroupId(), resultMsg, false);
    }

    /**
     * 下一页
     *
     * @param bot
     * @param event
     */

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "下一页")
    public void nextPage(Bot bot, GroupMessageEvent event) {

        if (!PermissionUtil.inGroupList(event.getGroupId())) {
            return;
        }

        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }

        if (CACHE.get(event.getUserId()) == null) {
            return;
        }

        String keyword = KEY_CACHE.get(event.getUserId());
        Page<FileDetail> files = CACHE.get(event.getUserId());
        Long all = fileDetailMapper.selectCount(null);

        if (!files.hasNext()) {
            log.info("用户 {} 已是最后一页", event.getUserId());
            MsgUtils msg = MsgUtils.builder()
                    .text("已是最后一页了\n")
                    .text("---------------\n");
            String resMsg = ResultMsgUtil.msgWithNotice(msg);
            bot.sendGroupMsg(event.getGroupId(), resMsg, false);
        }

        Page<FileDetail> pg = new Page<>(files.getCurrent() + 1, pageSize);
        pageQuery(bot, event, keyword, all, pg);

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

        if (!PermissionUtil.inGroupList(event.getGroupId())) {
            return;
        }

        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }

        int rowIndex = Integer.parseInt(matcher.group(1));

        if (!CACHE.containsKey(event.getUserId())) {
            log.info("用户 {} 未搜索文件", event.getUserId());
            return;
        }

        Page<FileDetail> fpl = CACHE.get(event.getUserId());

        if (rowIndex < 0 || rowIndex > fpl.getRecords().size()) {
            MsgUtils msg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("请输入正确的序号\n")
                    .text("---------------\n");

            String errMsg = ResultMsgUtil.msgWithNotice(msg);

            bot.sendGroupMsg(event.getGroupId(), errMsg, false);

            return;
        }

        FileDetail fdl = fpl.getRecords().get(rowIndex - 1);

        File level = FileUtil.file(rootPath, fdl.getFilePath());

        bot.uploadGroupFile(event.getGroupId(), level.getAbsolutePath(), fdl.getOriginalFileName());

    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^删除\s*(\\d+)?$")
    public void delete(Bot bot, GroupMessageEvent event, Matcher matcher) {

        if (!PermissionUtil.isAdmin(event.getUserId()) && !PermissionUtil.isWhiteUser(event.getUserId())) {
            return;
        }

        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }

        int rowIndex = Integer.parseInt(matcher.group(1).strip());

        Page<FileDetail> levels = CACHE.get(event.getUserId());

        if (levels == null) {
            log.info("用户 {} 未搜索文件", event.getUserId());
            return;
        }

        if (rowIndex < 0 || rowIndex > levels.getRecords().size()) {
            MsgUtils msg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("请输入正确的序号\n")
                    .text("---------------\n");

            String errMsg = ResultMsgUtil.msgWithNotice(msg);
            bot.sendGroupMsg(event.getGroupId(), errMsg, false);
            return;
        }

        FileDetail fdl = levels.getRecords().get(rowIndex - 1);
        File level = FileUtil.file(rootPath, fdl.getFilePath());

        if (FileUtil.del(level)) {
            MsgUtils msg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("删除文件" + level.getName() + " 成功 \n")
                    .text("---------------\n");
            String resMsg = ResultMsgUtil.msgWithNotice(msg);
            bot.sendGroupMsg(event.getGroupId(), resMsg, false);
        } else {
            MsgUtils msg = MsgUtils.builder()
                    .reply(event.getMessageId())
                    .text("删除失败，文件可能已删除\n")
                    .text("---------------\n");
            String errMsg = ResultMsgUtil.msgWithNotice(msg);
            bot.sendGroupMsg(event.getGroupId(), errMsg, false);
        }
        if (fileDetailMapper.exists(new LambdaQueryWrapper<>(FileDetail.class).eq(FileDetail::getId, fdl.getId()))) {
            fileDetailMapper.deleteById(fdl.getId());
        }
    }

}
