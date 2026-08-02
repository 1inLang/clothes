package com.lsp.clothes.model.enums;

public enum ProjectPriorityEnum {
    LOW("low"), MEDIUM("medium"), HIGH("high");

    private final String value;

    ProjectPriorityEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String value) {
        for (ProjectPriorityEnum item : values()) {
            if (item.value.equals(value)) return true;
        }
        return false;
    }
}
