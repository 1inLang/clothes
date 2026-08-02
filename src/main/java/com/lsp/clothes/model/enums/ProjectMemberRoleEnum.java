package com.lsp.clothes.model.enums;

import lombok.Getter;

@Getter
public enum ProjectMemberRoleEnum {
    MANAGER("项目经理", "manager"),
    DESIGNER("设计师", "designer"),
    REVIEWER("审核人", "reviewer"),
    VIEWER("只读成员", "viewer");

    private final String text;
    private final String value;

    ProjectMemberRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static boolean isValid(String value) {
        for (ProjectMemberRoleEnum item : values()) {
            if (item.value.equals(value)) return true;
        }
        return false;
    }

    public static String getTextByValue(String value) {
        for (ProjectMemberRoleEnum item : values()) {
            if (item.value.equals(value)) return item.text;
        }
        return value;
    }
}
