package com.livgo.po;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Layout;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.BoolType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.erupt.upms.handler.DictChoiceFetchHandler;


@Erupt(
        name = "权限管理",
        layout = @Layout(
                formSize = Layout.FormSize.FULL_LINE
        ),
        power = @Power(
                export = true
        )
)
@Table(name = "permission")
@Entity
@Getter
@Setter
public class Permission extends MetaModelVo {

    @EruptField(
            views = @View(
                    title = "名称",
                    desc = "权限账号的群名/用户名标识"
            ),
            edit = @Edit(
                    title = "名称",
                    desc = "权限账号的群名/用户名标识",
                    search = @Search(
                            vague = true
                    )
            )
    )
    private String name;

    @EruptField(
            views = @View(
                    title = "账号",
                    width = "10%"
            ),
            edit = @Edit(
                    title = "账号",
                    notNull = true,
                    type = EditType.NUMBER,
                    numberType = @NumberType(
                            max = 999999999999999999L,
                            min = 10000L
                    )
            )
    )
    private Long account;

    @EruptField(
            views = @View(
                    title = "身份",
                    width = "10%"
            ),
            edit = @Edit(
                    search = @Search,
                    title = "身份",
                    type = EditType.CHOICE,
                    choiceType = @ChoiceType(
                            fetchHandler = DictChoiceFetchHandler.class,
                            fetchHandlerParams = "permission"
                    ),
                    notNull = true
            )
    )
    private Integer identity;

    @EruptField(
            views = @View(
                    title = "是否启用"
            ),
            edit = @Edit(
                    title = "是否启用",
                    type = EditType.BOOLEAN,
                    boolType = @BoolType(
                            trueText = "启用",
                            falseText = "禁用"
                    )
            )
    )
    // 是否启用
    private boolean status = true;

}
