package com.livgo.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.livgo.constant.PermissionConstant;
import com.livgo.mapper.PermissionMapper;
import com.livgo.po.Permission;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.livgo.constant.PermissionConstant.ADMIN;

/**
 * 权限工具类
 *
 * @author livlong
 * @date 2026-03-18
 */
@Component
@Slf4j
public class PermissionUtil {

    private final PermissionMapper permissionMapper;

    private static PermissionMapper mapper;

    public PermissionUtil(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @PostConstruct
    public void init() {
        mapper = permissionMapper;
    }


    /**
     * 判断是否是超管 true 超管
     *
     * @param userId
     * @return
     */
    public static boolean isAdmin(Long userId) {
        LambdaQueryWrapper<Permission> query = new LambdaQueryWrapper<>();
        query.eq(Permission::getAccount, userId)
                .eq(Permission::getIdentity, ADMIN)
                .eq(Permission::isStatus, true);
        return mapper.selectCount(query) > 0;
    }

    /**
     * 判断群是否在白名单
     *
     * @param groupId
     * @return true 是
     */
    public static boolean inGroupList(Long groupId) {
        LambdaQueryWrapper<Permission> query = new LambdaQueryWrapper<>();
        query.eq(Permission::getAccount, groupId)
                .eq(Permission::getIdentity, PermissionConstant.GROUP_WHITE_LIST)
                .eq(Permission::isStatus, true);
        return mapper.selectCount(query) > 0;
    }

    /**
     * 判断群/用户是否具有自动上传权限
     *
     * @param guId
     * @return true  是
     */
    public static boolean isAutoUpload(Long guId) {
        LambdaQueryWrapper<Permission> query = new LambdaQueryWrapper<>();
        query.eq(Permission::getAccount, guId)
                .eq(Permission::getIdentity, PermissionConstant.AUTO_UPDATE_GROUP)
                .eq(Permission::isStatus, true);
        return mapper.selectCount(query) > 0;
    }

    /**
     * 判断用户是否在白名单
     *
     * @param userId
     * @return true 是
     */
    public static boolean isWhiteUser(Long userId) {
        LambdaQueryWrapper<Permission> query = new LambdaQueryWrapper<>();
        query.eq(Permission::getAccount, userId)
                .eq(Permission::getIdentity, PermissionConstant.USER_WHITE_LIST)
                .eq(Permission::isStatus, true);
        return mapper.selectCount(query) > 0;
    }

    /**
     * 判断用户是否在黑名单
     *
     * @param userId
     * @return
     */
    public static boolean isBlackUser(Long userId) {
        LambdaQueryWrapper<Permission> query = new LambdaQueryWrapper<>();
        query.eq(Permission::getAccount, userId)
                .eq(Permission::getIdentity, PermissionConstant.USER_BLACK_LIST)
                .eq(Permission::isStatus, true);
        return mapper.selectCount(query) > 0;
    }

}
