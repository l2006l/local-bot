package com.livgo.po;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.Vis;
import xyz.erupt.annotation.sub_erupt.Layout;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.AttachmentType;
import xyz.erupt.annotation.sub_field.sub_edit.BoolType;
import xyz.erupt.annotation.vis.CardView;
import xyz.erupt.jpa.model.MetaModelUpdateVo;

@Erupt(
        name = "公告详情",
        layout = @Layout(
                formSize = Layout.FormSize.FULL_LINE
        ),
        power = @Power(
                export = true
        ),
        visRawTable = false,
        vis = @Vis(
                title = "公告详情",
                cardView = @CardView(coverField = "image"),
                type = Vis.Type.CARD,
                fields = {"image"}
        ),
        orderBy = "NoticeDetail.updateTime desc"
)
@Table(name = "notice_detail")
@Entity
@Getter
@Setter
public class NoticeDetail extends MetaModelUpdateVo {

    @EruptField(
            views = @View(title = "公告内容"),
            edit = @Edit(
                    title = "公告内容",
                    notNull = true,
                    type = EditType.TEXTAREA
            )
    )
    private String content;

    @EruptField(
            views = @View(
                    title = "公告图片",
                    show = false
            ),
            edit = @Edit(
                    title = "公告图片",
                    type = EditType.ATTACHMENT,
                    attachmentType = @AttachmentType(
                            type = AttachmentType.Type.IMAGE
                    )
            )
    )
    private String image;

    //是否启用
    @EruptField(
            views = @View(title = "状态"),
            edit = @Edit(
                    title = "是否激活",
                    boolType = @BoolType(trueText = "激活", falseText = "禁用"),
                    desc = "机器人发消息时会获取更新日期最新且状态为激活的公告"
            )
    )
    private Boolean status = true;


}
