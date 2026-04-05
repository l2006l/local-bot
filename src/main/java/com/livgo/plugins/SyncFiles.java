package com.livgo.plugins;

import cn.hutool.core.io.FileUtil;
import com.livgo.utils.PermissionUtil;
import com.livgo.utils.ResultMsgUtil;
import com.livgo.utils.SyncFileUtil;
import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import org.springframework.stereotype.Component;

import static com.livgo.utils.PathUtil.getJarPath;

@Shiro
@Component
public class SyncFiles {

    private final String rootPath = getJarPath();

    private final String TEMP_PATH = "tempFile";

    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "文件整合")
    public void syncFiles(Bot bot, AnyMessageEvent event) {
        if (!PermissionUtil.isAdmin(event.getUserId())) {
            return;
        }
        if (PermissionUtil.isBlackUser(event.getUserId())) {
            return;
        }
        int count = SyncFileUtil.syncFiles();
        MsgUtils msg = MsgUtils.builder()
                .text("整合完成\n")
                .text("共计" + count + "个文件")
                .text("==================\n");

        FileUtil.cleanEmpty(FileUtil.file(rootPath, TEMP_PATH));

        bot.sendMsg(event, ResultMsgUtil.msgWithNotice(msg), false);
    }

}
