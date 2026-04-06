package com.livgo.po;

import com.livgo.dtproxy.PermissionBotProxy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Layout;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.MetaModelUpdateVo;
import xyz.erupt.jpa.model.MetaModelVo;

import java.util.Set;

@Erupt(
        name = "权限管理",
        layout = @Layout(
                formSize = Layout.FormSize.FULL_LINE
        ),
        power = @Power(
                export = true
        ),
        dataProxy = PermissionBotProxy.class
)
@Table(name = "permission_bot")
@Entity
@Getter
@Setter
public class PermissionBot extends MetaModelUpdateVo {

    @EruptField(
            views = @View(
                    title = "机器人名称",
                    desc = "机器人账号名称",
                    width = "15%"
            ),
            edit = @Edit(
                    title = "机器人名称",
                    desc = "机器人账号名称"
            )
    )
    private String name;

    @EruptField(
            views = @View(
                    title = "机器人账号",
                    desc = "机器人账号"
            ),
            edit = @Edit(
                    title = "机器人账号",
                    desc = "机器人账号",
                    notNull = true,
                    type = EditType.NUMBER,
                    numberType = @NumberType(
                            max = 999999999999999999L,
                            min = 10000L
                    )
            )
    )
    private Long botAccount;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "permission_bot_id")
    @OrderBy
    @EruptField(
            edit = @Edit(
                    title = "权限管理",
                    type = EditType.TAB_TABLE_ADD
            )
    )
    private Set<Permission> permissions;

}
