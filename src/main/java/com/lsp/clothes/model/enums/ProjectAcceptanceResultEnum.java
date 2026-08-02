package com.lsp.clothes.model.enums;

import lombok.Getter;

@Getter
public enum ProjectAcceptanceResultEnum {
    APPROVED("通过", "approved"),
    REJECTED("退回", "rejected");

    private final String text;
    private final String value;

    ProjectAcceptanceResultEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
}
