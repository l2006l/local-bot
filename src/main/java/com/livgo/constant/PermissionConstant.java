package com.livgo.constant;

import org.springframework.stereotype.Component;

@Component
public class PermissionConstant {

    public static final Long ADMIN = 1L;

    public static final Long GROUP_WHITE_LIST = 2L;

    public static final Long USER_WHITE_LIST = 3L;

    public static final Long AUTO_UPDATE_GROUP = 4L;

    public static final Long USER_BLACK_LIST = 5L;

}
