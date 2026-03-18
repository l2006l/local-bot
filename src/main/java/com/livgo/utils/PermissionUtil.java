package com.livgo.utils;

import com.livgo.config.GroupList;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限工具类
 *
 * @author livlong
 * @date 2026-03-18
 */
@Component
@Slf4j
public class PermissionUtil {

    @Resource
    private GroupList groupList;

    @Value("${run.autoGroup}")
    private Long auto;

    private static List<Long> GP_LIST;

    private static Long AUTO_GROUP;

    private static List<Long> ADMIN_LIST;

    @PostConstruct
    public void init() {
        PermissionUtil.GP_LIST = groupList.getGroupList();
        PermissionUtil.ADMIN_LIST = groupList.getAdmin();
        PermissionUtil.AUTO_GROUP = auto;
    }


    public static boolean inGroupList(Long groupId) {
        if (!GP_LIST.isEmpty() && !GP_LIST.contains(groupId)) {
            log.warn("群 {} 未开放该功能", groupId);
            return true;
        }
        return false;
    }

    public static boolean isAutoUpload(Long groupId) {
        return !AUTO_GROUP.equals(groupId);
    }

    public static boolean isAdmin(Long userId) {
        return !ADMIN_LIST.contains(userId);
    }

}
