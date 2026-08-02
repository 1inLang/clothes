package com.lsp.clothes.model.dto.notification;

import com.lsp.clothes.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQueryRequest extends PageRequest implements Serializable {
    private String type;
    private Boolean unreadOnly;
    private static final long serialVersionUID = 1L;
}
