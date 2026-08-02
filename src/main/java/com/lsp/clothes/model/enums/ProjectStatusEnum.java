package com.lsp.clothes.model.enums;

import lombok.Getter;

@Getter
public enum ProjectStatusEnum {
    DRAFT("草稿", "draft"),
    APPROVED("已立项", "approved"),
    DESIGNING("设计中", "designing"),
    ACCEPTANCE("验收中", "acceptance"),
    COMPLETED("已完成", "completed"),
    CANCELLED("已取消", "cancelled");

    private final String text;
    private final String value;

    ProjectStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static boolean isValid(String value) {
        for (ProjectStatusEnum item : values()) {
            if (item.value.equals(value)) return true;
        }
        return false;
    }
}
