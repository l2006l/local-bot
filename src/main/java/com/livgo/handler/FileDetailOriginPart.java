package com.livgo.handler;

import cn.hutool.core.date.DateUtil;
import com.livgo.po.FileDetail;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.livgo.utils.DoToOrigin.doToOrigin;

public class FileDetailOriginPart implements OperationHandler<FileDetail, Void> {

    private static final String ORIGIN = "origin-part";

    @Override
    public String exec(List<FileDetail> data, Void unused, String[] param) {
        if (data.isEmpty()) {
            return "window.msg.warning('请选择需要转出的文件')";
        }
        AtomicInteger count = new AtomicInteger();
        String dateStr = DateUtil.format(DateUtil.date(), "yyyy-MM-dd");
        for (FileDetail fd : data) {
            doToOrigin(fd, dateStr, count, ORIGIN);
        }
        String msg = "整合完成，共处理" + count + "个文件";
        return String.format("window.msg.info('%s')", msg);
    }

}
