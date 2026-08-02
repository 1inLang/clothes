package com.lsp.clothes.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ProjectMemberVO implements Serializable {
    private String id;
    private String projectId;
    private String userId;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userRole;
    private List<String> userRoles;
    private String projectRole;
    private Date joinTime;
    private static final long serialVersionUID = 1L;
}
