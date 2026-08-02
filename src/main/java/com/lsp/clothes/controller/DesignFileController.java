package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.DeleteRequest;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.DesignFileVO;
import com.lsp.clothes.service.DesignFileService;
import com.lsp.clothes.service.FileDownload;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/file")
public class DesignFileController {

    @Resource private DesignFileService designFileService;
    @Resource private UserService userService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckPermission("design:upload")
    public BaseResponse<Long> upload(@RequestPart("file") MultipartFile file,
                                     @RequestParam Long projectId,
                                     @RequestParam(required = false) Long taskId,
                                     @RequestParam(defaultValue = "design") String fileType,
                                     @RequestParam(required = false) String versionNote) {
        return ResultUtils.success(designFileService.upload(file, projectId, taskId,
                fileType, versionNote, loginUser()));
    }

    @GetMapping("/list")
    @SaCheckPermission("design:view")
    public BaseResponse<List<DesignFileVO>> list(@RequestParam(required = false) Long projectId,
                                                 @RequestParam(required = false) Long taskId,
                                                 @RequestParam(required = false) String fileType) {
        return ResultUtils.success(designFileService.listFiles(projectId, taskId, fileType, loginUser()));
    }

    @GetMapping("/version/list")
    @SaCheckPermission("design:view")
    public BaseResponse<List<DesignFileVO>> versions(@RequestParam long taskId) {
        return ResultUtils.success(designFileService.listVersions(taskId, loginUser()));
    }

    @GetMapping("/get")
    @SaCheckPermission("design:view")
    public BaseResponse<DesignFileVO> get(@RequestParam long id) {
        return ResultUtils.success(designFileService.getFileVO(id, loginUser()));
    }

    @GetMapping("/download")
    @SaCheckPermission("design:download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@RequestParam long id) {
        FileDownload file = designFileService.download(id, loginUser());
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (Exception ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    @PostMapping("/delete")
    @SaCheckPermission("design:delete")
    public BaseResponse<Boolean> delete(@org.springframework.web.bind.annotation.RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null,
                ErrorCode.PARAMS_ERROR, "文件 ID 不能为空");
        designFileService.deleteFile(request.getId(), loginUser());
        return ResultUtils.success(true);
    }

    private User loginUser() { return userService.getLoginUser(null); }
}
