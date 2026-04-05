package com.livgo.utils;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.livgo.mapper.NoticeDetailMapper;
import com.livgo.po.FileDetail;
import com.livgo.po.NoticeDetail;
import com.mikuac.shiro.common.utils.MsgUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 回复消息工具类
 *
 * @author livlong
 * @date 2026-03-18
 */
@Component
public class ResultMsgUtil {

    private static String UPLOAD = "upload";

    private final NoticeDetailMapper noticeDetailMapper;

    private static NoticeDetailMapper mapper;

    public ResultMsgUtil(NoticeDetailMapper noticeDetailMapper) {
        this.noticeDetailMapper = noticeDetailMapper;
    }

    @Value("${erupt.upload-path}")
    private void setUploadPath(String uploadPath) {
        UPLOAD = uploadPath;
    }

    @PostConstruct
    public void init() {
        mapper = noticeDetailMapper;
    }

    public static Optional<NoticeDetail> notice() {
        Page<NoticeDetail> pg = new Page<>(1, 1);
        LambdaQueryWrapper<NoticeDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NoticeDetail::getStatus, true)
                .orderByDesc(NoticeDetail::getUpdateTime);
        return mapper.selectPage(pg, queryWrapper)
                .getRecords()
                .stream()
                .findFirst();
    }

    public static MsgUtils baseMsg(Integer messageId, String msg, Long all, Long resSize, Long totalPage, Long pageNum) {
        return MsgUtils.builder()
                .reply(messageId)
                .text("🗂️ 搜索结果列表（有效时间2分钟）\n")
                .text("-----------\n")
                .text(msg)
                .text("-----------\n")
                .text("📊 库中共计 " + all + " 个文件\n")
                .text(" 共" + resSize + "个结果， 当前页数 " + pageNum + "/" + totalPage + "\n")
                .text("-----------\n")
                .text("💡 下载指令：下载 [序号]\n")
                .text("📃 翻页指令：上一页 / 下一页\n");
    }

    /**
     * 搜索结果消息构建
     *
     * @param messageId 回复的消息id
     * @param msg       搜索结果拼接处理后的字符串
     * @param all       库中总共的文件数
     * @param resSize   搜索结果数
     * @param totalPage 搜索结果总页数
     * @param pageNum   搜索结果当前页数
     * @return 消息字符串
     */

    public static String pageMsg(Integer messageId, String msg, Long all, Long resSize, Long totalPage, Long pageNum) {

        MsgUtils baseMsg = baseMsg(messageId, msg, all, resSize, totalPage, pageNum);

        NoticeDetail notice = notice().orElse(null);

        if (notice == null) {
            return baseMsg.build();
        }

        if (!notice.getContent().isEmpty() && !notice.getImage().isEmpty()) {
            return baseMsg.text("-----------\n")
                    .text(notice.getContent())
                    .img(FileUtil.file(UPLOAD, notice.getImage()).getAbsolutePath())
                    .build();
        }

        if (!notice.getContent().isEmpty()) {
            return baseMsg.text("-----------\n")
                    .text(notice.getContent())
                    .build();
        }

        if (!notice.getImage().isEmpty()) {
            return baseMsg.text("-----------\n")
                    .img(FileUtil.file(UPLOAD, notice.getImage()).getAbsolutePath())
                    .build();
        }

        return baseMsg.build();

    }

    public static String fileListMsg(Page<FileDetail> files) {
        StringBuilder resMsg = new StringBuilder();

        AtomicInteger i = new AtomicInteger(1);

        files.getRecords().forEach(f -> {
            resMsg.append(i)
                    .append(".")
                    .append("💾 ")
                    .append(f.getFileAliasName())
                    .append("\n📦 大小：")
                    .append(f.getFileSize())
                    .append("\n\n");
            i.getAndIncrement();
        });

        return resMsg.toString();
    }

    public static String msgWithNotice(MsgUtils msg) {
        NoticeDetail notice = notice().orElse(null);
        if (notice == null) {
            return msg.build();
        }

        if (!notice.getContent().isEmpty() && !notice.getImage().isEmpty()) {
            return msg.text(notice.getContent())
                    .img(FileUtil.file(UPLOAD, notice.getImage()).getAbsolutePath())
                    .build();
        }

        if (!notice.getContent().isEmpty()) {
            return msg.text(notice.getContent())
                    .build();
        }

        if (!notice.getImage().isEmpty()) {
            return msg.img(FileUtil.file(UPLOAD, notice.getImage()).getAbsolutePath())
                    .build();
        }

        return msg.build();
    }

}
