package com.livgo.utils;

import cn.hutool.core.io.FileUtil;
import com.mikuac.shiro.common.utils.MsgUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 回复消息工具类
 *
 * @author livlong
 * @date 2026-03-18
 */
@Component
public class ResultMsgUtil {

    public static String RUN_LOCATION;

    @Value("${run.location}")
    public void setRunLocation(String runLocation) {
        ResultMsgUtil.RUN_LOCATION = runLocation;
    }

    public static String notice() {
        File notice = FileUtil.file(RUN_LOCATION, "message.txt");
        FileUtil.touch(notice);
        return FileUtil.readUtf8String(notice).trim();
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

    public static String pageMsg(Integer messageId, String msg, Integer all, Integer resSize, Integer totalPage, Integer pageNum) {

        return MsgUtils.builder()
                .reply(messageId)
                .text("🗂️ 搜索结果列表\n")
                .text("-----------\n")
                .text(msg)
                .text("-----------\n")
                .text("📊 库中共计 " + all + " 个文件\n")
                .text(" 共" + resSize + "个结果， 当前页数 " + (pageNum + 1) + "/" + totalPage + "\n")
                .text("-----------\n")
                .text("💡 下载指令：下载 [序号]\n")
                .text("📃 翻页指令：上一页 / 下一页\n")
                .text("-----------\n")
                .text(notice())
                .build();
    }

    public static String fileListMsg(List<File> files) {
        StringBuilder resMsg = new StringBuilder();

        AtomicInteger i = new AtomicInteger(1);

        files.forEach(f -> {
            resMsg.append(i)
                    .append(".")
                    .append("💾 ")
                    .append(f.getName())
                    .append("\n📦 大小：")
                    .append(FileUtil.readableFileSize(f.length()))
                    .append("\n\n");
            i.getAndIncrement();
        });

        return resMsg.toString();
    }

    public static String msgWithNotice(MsgUtils msg) {
        return msg.text(notice())
                .build();
    }

}
