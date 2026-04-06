package com.livgo.utils;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.livgo.po.Permission;
import com.livgo.po.PermissionBot;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.jpa.dao.EruptDao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.livgo.constant.PermissionConstant.*;

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
    private EruptDao eruptDao;

    private static final ConcurrentHashSet<Set<Long>> CACHE = new ConcurrentHashSet<>();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        CACHE.clear();
        List<PermissionBot> list = eruptDao.lambdaQuery(PermissionBot.class)
                .with(PermissionBot::getPermissions)
                .with()
                .list();

        for (PermissionBot permissionBot : list) {
            for (Permission permission : permissionBot.getPermissions()) {
                if (!permission.isStatus()) {
                    continue;
                }
                Set<Long> set = new HashSet<>();
                set.add(permissionBot.getBotAccount());
                set.add(permission.getUserAccount());
                set.add(permission.getIdentity());
                CACHE.add(set);
            }
        }
    }


    /**
     * 判断是否是超管 true 超管
     *
     * @param userId
     * @return
     */
    public static boolean isAdmin(Long botId, Long userId) {
        Set<Long> set = new HashSet<>();
        set.add(botId);
        set.add(userId);
        set.add(ADMIN);
        return CACHE.contains(set);
    }

    /**
     * 判断群是否在白名单
     *
     * @param groupId
     * @return true 是
     */
    public static boolean inGroupList(Long botId, Long groupId) {
        Set<Long> set = new HashSet<>();
        set.add(botId);
        set.add(groupId);
        set.add(GROUP_WHITE_LIST);
        return CACHE.contains(set);
    }

    /**
     * 判断群/用户是否具有自动上传权限
     *
     * @param guId
     * @return true  是
     */
    public static boolean isAutoUpload(Long botId, Long guId) {
        Set<Long> set = new HashSet<>();
        set.add(botId);
        set.add(guId);
        set.add(AUTO_UPDATE_GROUP);
        return CACHE.contains(set);
    }

    /**
     * 判断用户是否在白名单
     *
     * @param userId
     * @return true 是
     */
    public static boolean isWhiteUser(Long botId, Long userId) {
        Set<Long> set = new HashSet<>();
        set.add(botId);
        set.add(userId);
        set.add(USER_WHITE_LIST);
        return CACHE.contains(set);
    }

    /**
     * 判断用户是否在黑名单
     *
     * @param userId
     * @return
     */
    public static boolean isBlackUser(Long botId, Long userId) {
        Set<Long> set = new HashSet<>();
        set.add(botId);
        set.add(userId);
        set.add(USER_BLACK_LIST);
        return CACHE.contains(set);
    }

}
