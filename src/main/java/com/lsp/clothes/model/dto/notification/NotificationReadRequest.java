package com.lsp.clothes.model.dto.notification;

import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationReadRequest implements Serializable {
    private Long id;
    private static final long serialVersionUID = 1L;
}
