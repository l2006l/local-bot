package com.livgo.dtproxy;

import com.livgo.po.PermissionBot;
import com.livgo.utils.PermissionUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import xyz.erupt.annotation.fun.DataProxy;

@Service
public class PermissionBotProxy implements DataProxy<PermissionBot> {

    @Resource
    private PermissionUtil permissionUtil;

    @Override
    public void afterAdd(PermissionBot permissionBot) {
        permissionUtil.init();
    }

    @Override
    public void afterDelete(PermissionBot permissionBot) {
        permissionUtil.init();
    }

    @Override
    public void afterUpdate(PermissionBot permissionBot) {
        permissionUtil.init();
    }
}
