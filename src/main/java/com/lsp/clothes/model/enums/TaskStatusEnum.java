package com.lsp.clothes.model.enums;

import lombok.Getter;

@Getter
public enum TaskStatusEnum {
    UNASSIGNED("待分配", "unassigned"),
    PENDING_ACCEPTANCE("待领取", "pending_acceptance"),
    IN_PROGRESS("进行中", "in_progress"),
    PENDING_REVIEW("待审核", "pending_review"),
    REVISION("退回修改", "revision"),
    COMPLETED("已完成", "completed"),
    CANCELLED("已取消", "cancelled");

    private final String text;
    private final String value;

    TaskStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static boolean isValid(String value) {
        for (TaskStatusEnum item : values()) if (item.value.equals(value)) return true;
        return false;
    }
}
