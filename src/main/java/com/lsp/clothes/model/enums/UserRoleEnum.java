package com.lsp.clothes.model.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("普通用户", "user"),
    DESIGNER("设计师", "designer"),
    REVIEWER("审核人", "reviewer"),
    PROJECT_ACCEPTOR("项目验收人", "project_acceptor"),
    PROJECT_MANAGER("项目经理", "project_manager"),
    ADMIN("管理员", "admin");

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (UserRoleEnum role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }

        return null;
    }
}
