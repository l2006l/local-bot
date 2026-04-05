package com.livgo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.livgo.dtproxy.FileDetailProxy;
import com.livgo.handler.FileDetailOriginAll;
import com.livgo.handler.FileDetailOriginPart;
import com.livgo.handler.FileDetailSyncHandler;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Layout;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.Readonly;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;

import java.time.LocalDateTime;

@Erupt(
        name = "文件详情",
        layout = @Layout(
                formSize = Layout.FormSize.FULL_LINE
        ),
        power = @Power(
                export = true
        ),
        dataProxy = FileDetailProxy.class,
        rowOperation = {
                @RowOperation(
                        title = "文件整合",
                        mode = RowOperation.Mode.BUTTON,
                        tip = "点击后可将tempFile文件夹下的文件合并到levels下",
                        operationHandler = FileDetailSyncHandler.class,
                        callHint = "确定要将tempFile文件夹下的文件合并到levels吗？\n tempFile中的文件将不会保留"
                ),
                @RowOperation(
                        title = "部分转出",
                        tip = "将选择的文件转出到origin-part文件夹下",
                        operationHandler = FileDetailOriginPart.class,
                        callHint = "确定要将所选文件转出吗？\n 请确保磁盘空间充足"
                ),
                @RowOperation(
                        title = "文件转出",
                        mode = RowOperation.Mode.BUTTON,
                        tip = "将所有文件转出到origin-all文件夹下",
                        operationHandler = FileDetailOriginAll.class,
                        callHint = "确定要将所有文件转出吗？\n 请确保磁盘空间充足"
                )
        }
)
@Table(name = "file_detail")
@Entity
@Getter
@Setter
public class FileDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EruptField(
            views = @View(
                    show = false,
                    title = "id"
            )
    )
    @Column(name = "id")
    private Long id;

    @Transient
    @EruptField(
            edit = @Edit(
                    title = "文件",
                    type = EditType.ATTACHMENT,
                    readonly = @Readonly(
                            add = false
                    )
            )
    )
    @TableField(exist = false)
    private String tempPath;

    @EruptField(
            views = @View(title = "文件名")
    )
    private String fileName;

    @EruptField(
            views = @View(title = "文件原名"),
            edit = @Edit(
                    title = "文件原名",
                    search = @Search(
                            vague = true
                    ),
                    readonly = @Readonly()
            )
    )
    private String originalFileName;

    @EruptField(
            views = @View(title = "文件别名"),
            edit = @Edit(
                    title = "文件别名",
                    search = @Search(
                            vague = true
                    )
            )
    )
    private String fileAliasName;

    @EruptField(
            views = @View(
                    title = "来源群聊"
            ),
            edit = @Edit(
                    title = "来源群聊",
                    type = EditType.NUMBER,
                    numberType = @NumberType(
                            min = 10000L,
                            max = 999999999999999999L
                    ),
                    search = @Search(),
                    readonly = @Readonly(
                            add = false
                    )
            )
    )
    private Long originalGroupId;

    @EruptField(
            views = @View(title = "文件大小"),
            edit = @Edit(
                    title = "文件大小",
                    readonly = @Readonly()
            )
    )
    private String fileSize;

    @EruptField(
            views = @View(title = "文件md5"),
            edit = @Edit(
                    title = "文件md5",
                    readonly = @Readonly()
            )
    )
    private String fileMd5;

    @EruptField(
            views = @View(
                    title = "文件路径",
                    show = false
            ),
            edit = @Edit(
                    title = "文件路径",
                    readonly = @Readonly()
            )
    )
    private String filePath;

    @EruptField(
            views = @View(
                    title = "文件上传时间",
                    sortable = true
            ),
            edit = @Edit(
                    title = "文件上传时间",
                    type = EditType.DATE,
                    readonly = @Readonly()
            )
    )
    private LocalDateTime fileUploadTime;

}
