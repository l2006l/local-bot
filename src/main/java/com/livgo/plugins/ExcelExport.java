package com.livgo.plugins;

import cn.hutool.core.io.FileUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import com.livgo.mapper.FileDetailMapper;
import com.livgo.po.FileDetail;
import com.livgo.utils.PermissionUtil;
import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.livgo.utils.PathUtil.getJarPath;

@Shiro
@Component
public class ExcelExport {

    @Resource
    private FileDetailMapper mapper;

    private static String rootPath = getJarPath();

    private static String TEMP_FILE = "tempFile";

    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "文件列表")
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void list(Bot bot, AnyMessageEvent event) {
        if (PermissionUtil.isBlackUser(bot.getSelfId(), event.getUserId())) {
            return;
        }
        List<FileDetail> fdl = new ArrayList<>();

        File excel = FileUtil.file(rootPath, TEMP_FILE, "文件列表.xlsx");

        FileUtil.del(excel);

        BigExcelWriter writer = ExcelUtil.getBigWriter(excel);

        writer.setOnlyAlias(true);

        writer.addHeaderAlias("fileAliasName", "文件名");
        writer.addHeaderAlias("originalGroupId", "来源群聊");
        writer.addHeaderAlias("fileSize", "文件大小");
        writer.addHeaderAlias("fileMd5", "文件md5");
        writer.addHeaderAlias("fileUploadTime", "文件上传时间");

        //查询数据
        mapper.selectList(null, resultContext -> {
            fdl.add(resultContext.getResultObject());
            if (fdl.size() >= 500) {
                writer.write(fdl);
                fdl.clear();
            }
        });

        if (!fdl.isEmpty()) {
            writer.write(fdl);
        }

        writer.close();

        if (event.getGroupId() == null) {
            bot.uploadPrivateFile(event.getUserId(), excel.getAbsolutePath(), excel.getName());
            return;
        }

        bot.uploadGroupFile(event.getGroupId(), excel.getAbsolutePath(), excel.getName());

        FileUtil.del(excel);

    }

}
