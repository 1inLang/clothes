package com.lsp.clothes.model.enums;

import lombok.Getter;

@Getter
public enum DesignFileTypeEnum {
    DESIGN("设计稿", "design"),
    REFERENCE("参考资料", "reference"),
    ATTACHMENT("其他附件", "attachment");

    private final String text;
    private final String value;

    DesignFileTypeEnum(String text, String value) { this.text = text; this.value = value; }

    public static boolean isValid(String value) {
        for (DesignFileTypeEnum item : values()) if (item.value.equals(value)) return true;
        return false;
    }
}
