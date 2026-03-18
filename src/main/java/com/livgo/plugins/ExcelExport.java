package com.livgo.plugins;

import cn.hutool.core.io.FileUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import com.livgo.po.FilePo;
import com.livgo.utils.PermissionUtil;
import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导出
 *
 * @author livlong
 * @date 2026-03-18
 */
@Shiro
@Component
public class ExcelExport {

    @Value("${run.location}")
    private String runLocation;

    private static final String TEMP_FILE = "tempFile";

    private static final String LEVELS = "levels";

    /**
     *  获取文件列表（导出excel并发送）
     *
     * @param bot   机器人
     * @param event 活动
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "文件列表")
    public void list(Bot bot, AnyMessageEvent event) {
        if (event.getGroupId() != null) {
            if (PermissionUtil.inGroupList(event.getGroupId())) {
                return;
            }
        }
        List<FilePo> fpo = new ArrayList<>();

        File excel = FileUtil.file(runLocation, TEMP_FILE, "文件列表.xlsx");

        FileUtil.del(excel);

        BigExcelWriter writer = ExcelUtil.getBigWriter(excel);

        writer.addHeaderAlias("name", "文件名");
        writer.addHeaderAlias("size", "文件大小");
        writer.addHeaderAlias("path", "文件路径");
        writer.setOnlyAlias(true);

        FileUtil.walkFiles(FileUtil.file(runLocation, LEVELS), f -> {
            FilePo po = new FilePo();
            po.setName(f.getName());
            po.setSize(FileUtil.readableFileSize(f));
            po.setPath(FileUtil.subPath(runLocation, f));
            fpo.add(po);
        });

        writer.write(fpo);
        writer.close();

        if (event.getGroupId() == null) {
            bot.uploadPrivateFile(event.getUserId(), excel.getAbsolutePath(), excel.getName());
            return;
        }

        bot.uploadGroupFile(event.getGroupId(), excel.getAbsolutePath(), excel.getName());

        FileUtil.del(excel);

    }

}
