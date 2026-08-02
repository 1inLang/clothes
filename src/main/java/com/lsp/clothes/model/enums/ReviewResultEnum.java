package com.lsp.clothes.model.enums;

import lombok.Getter;

@Getter
public enum ReviewResultEnum {
    APPROVED("审核通过", "approved"),
    REJECTED("退回修改", "rejected");

    private final String text;
    private final String value;
    ReviewResultEnum(String text, String value) { this.text = text; this.value = value; }
}
