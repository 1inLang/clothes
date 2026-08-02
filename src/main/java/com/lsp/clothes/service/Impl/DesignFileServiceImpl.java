package com.lsp.clothes.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.BusinessException;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.mapper.DesignFileMapper;
import com.lsp.clothes.mapper.ProjectMemberMapper;
import com.lsp.clothes.mapper.UserMapper;
import com.lsp.clothes.model.entity.DesignFile;
import com.lsp.clothes.model.entity.DesignProject;
import com.lsp.clothes.model.entity.DesignTask;
import com.lsp.clothes.model.entity.ProjectMember;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.enums.DesignFileTypeEnum;
import com.lsp.clothes.model.enums.ProjectMemberRoleEnum;
import com.lsp.clothes.model.enums.ProjectStatusEnum;
import com.lsp.clothes.model.enums.TaskStatusEnum;
import com.lsp.clothes.model.vo.DesignFileVO;
import com.lsp.clothes.service.DesignFileService;
import com.lsp.clothes.service.DesignProjectService;
import com.lsp.clothes.service.DesignTaskService;
import com.lsp.clothes.service.FileDownload;
import com.lsp.clothes.service.UserService;
import com.lsp.clothes.storage.FileStorageService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DesignFileServiceImpl extends CrudRepository<DesignFileMapper, DesignFile>
        implements DesignFileService {

    private static final Map<String, Set<String>> ALLOWED_MIME = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")), Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")), Map.entry("webp", Set.of("image/webp")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("ai", Set.of("application/pdf", "application/postscript", "application/octet-stream")),
            Map.entry("psd", Set.of("image/vnd.adobe.photoshop", "application/octet-stream")),
            Map.entry("sketch", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream")),
            Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream")),
            Map.entry("doc", Set.of("application/msword", "application/octet-stream")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/octet-stream")),
            Map.entry("xls", Set.of("application/vnd.ms-excel", "application/octet-stream")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/zip", "application/octet-stream")),
            Map.entry("ppt", Set.of("application/vnd.ms-powerpoint", "application/octet-stream")),
            Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/zip", "application/octet-stream")),
            Map.entry("txt", Set.of("text/plain"))
    );

    @Resource private DesignProjectService designProjectService;
    @Resource private DesignTaskService designTaskService;
    @Resource private ProjectMemberMapper projectMemberMapper;
    @Resource private UserMapper userMapper;
    @Resource private UserService userService;
    @Resource private FileStorageService fileStorageService;

    @Value("${clothes.storage.max-file-size:52428800}")
    private long maxFileSize;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upload(MultipartFile file, Long projectId, Long taskId, String fileType,
                       String versionNote, User loginUser) {
        validateUpload(file, fileType);
        DesignProject project = resolveProject(projectId, taskId, loginUser);
        ThrowUtils.throwIf(List.of(ProjectStatusEnum.COMPLETED.getValue(),
                        ProjectStatusEnum.CANCELLED.getValue()).contains(project.getStatus()),
                ErrorCode.OPERATION_ERROR, "已完成或已取消项目不能上传文件");
        DesignTask task = taskId == null ? null : designTaskService.getAccessibleTask(taskId, loginUser);
        boolean manager = canManage(project, loginUser);
        if (DesignFileTypeEnum.DESIGN.getValue().equals(fileType)) {
            ThrowUtils.throwIf(task == null, ErrorCode.PARAMS_ERROR, "设计稿必须关联设计任务");
            ThrowUtils.throwIf(!manager && !loginUser.getId().equals(task.getAssigneeId()),
                    ErrorCode.NO_AUTH_ERROR, "只有任务负责人或项目经理可以上传设计稿");
            ThrowUtils.throwIf(!List.of(TaskStatusEnum.IN_PROGRESS.getValue(),
                            TaskStatusEnum.REVISION.getValue()).contains(task.getStatus()),
                    ErrorCode.OPERATION_ERROR, "只有进行中或退回修改的任务可以上传设计稿");
        }

        String fileName = safeFileName(file.getOriginalFilename());
        String extension = extension(fileName);
        int version = this.baseMapper.selectMaxVersion(project.getId(), taskId) + 1;
        String storageKey = null;
        try (InputStream input = file.getInputStream()) {
            storageKey = fileStorageService.save(input, extension);
            DesignFile record = new DesignFile();
            record.setProjectId(project.getId());
            record.setTaskId(taskId);
            record.setFileName(fileName);
            record.setStorageKey(storageKey);
            record.setFileType(fileType);
            record.setMimeType(normalizeMime(file.getContentType()));
            record.setFileSize(file.getSize());
            record.setVersionNo(version);
            record.setVersionNote(StrUtil.trim(versionNote));
            record.setUploaderId(loginUser.getId());
            if (!this.save(record)) throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存文件记录失败");
            return record.getId();
        } catch (IOException | RuntimeException exception) {
            if (storageKey != null) fileStorageService.deleteQuietly(storageKey);
            if (exception instanceof BusinessException businessException) throw businessException;
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件保存失败");
        }
    }

    @Override
    public List<DesignFileVO> listFiles(Long projectId, Long taskId, String fileType, User loginUser) {
        DesignProject project = resolveProject(projectId, taskId, loginUser);
        ThrowUtils.throwIf(StrUtil.isNotBlank(fileType) && !DesignFileTypeEnum.isValid(fileType),
                ErrorCode.PARAMS_ERROR, "文件类型不合法");
        QueryWrapper<DesignFile> wrapper = new QueryWrapper<DesignFile>()
                .eq("project_id", project.getId())
                .eq(taskId != null, "task_id", taskId)
                .isNull(taskId == null, "task_id")
                .eq(StrUtil.isNotBlank(fileType), "file_type", fileType)
                .orderByDesc("version_no", "id");
        return this.list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<DesignFileVO> listVersions(Long taskId, User loginUser) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR);
        DesignTask task = designTaskService.getAccessibleTask(taskId, loginUser);
        return listFiles(task.getProjectId(), taskId, DesignFileTypeEnum.DESIGN.getValue(), loginUser);
    }

    @Override
    public DesignFileVO getFileVO(Long id, User loginUser) {
        return toVO(getAccessibleFile(id, loginUser));
    }

    @Override
    public FileDownload download(Long id, User loginUser) {
        DesignFile file = getAccessibleFile(id, loginUser);
        org.springframework.core.io.Resource resource = fileStorageService.load(file.getStorageKey());
        return new FileDownload(resource, file.getFileName(), file.getMimeType(), file.getFileSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id, User loginUser) {
        DesignFile file = getAccessibleFile(id, loginUser);
        DesignProject project = designProjectService.getAccessibleProject(file.getProjectId(), loginUser);
        ThrowUtils.throwIf(!loginUser.getId().equals(file.getUploaderId()) && !canManage(project, loginUser),
                ErrorCode.NO_AUTH_ERROR, "只有上传人或项目经理可以删除文件");
        if (file.getTaskId() != null) {
            DesignTask task = designTaskService.getAccessibleTask(file.getTaskId(), loginUser);
            ThrowUtils.throwIf(Integer.valueOf(1).equals(file.getSubmittedFlag()),
                    ErrorCode.OPERATION_ERROR, "已提交审核的设计稿版本不能删除");
        }
        ThrowUtils.throwIf(!this.removeById(id), ErrorCode.OPERATION_ERROR, "删除文件记录失败");
        // 采用逻辑删除并保留物理文件，便于历史审核追溯和误删恢复。
    }

    private DesignFile getAccessibleFile(Long id, User loginUser) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        DesignFile file = this.getById(id);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在或已删除");
        designProjectService.getAccessibleProject(file.getProjectId(), loginUser);
        return file;
    }

    private DesignProject resolveProject(Long projectId, Long taskId, User loginUser) {
        ThrowUtils.throwIf(projectId == null && taskId == null, ErrorCode.PARAMS_ERROR, "项目或任务不能为空");
        if (taskId != null) {
            DesignTask task = designTaskService.getAccessibleTask(taskId, loginUser);
            ThrowUtils.throwIf(projectId != null && !projectId.equals(task.getProjectId()),
                    ErrorCode.PARAMS_ERROR, "任务不属于指定项目");
            return designProjectService.getAccessibleProject(task.getProjectId(), loginUser);
        }
        return designProjectService.getAccessibleProject(projectId, loginUser);
    }

    private boolean canManage(DesignProject project, User user) {
        if (userService.hasRole(user.getId(), "admin") || project.getManagerId().equals(user.getId())) return true;
        ProjectMember member = projectMemberMapper.selectOne(new QueryWrapper<ProjectMember>()
                .eq("project_id", project.getId()).eq("user_id", user.getId()).last("LIMIT 1"));
        return member != null && ProjectMemberRoleEnum.MANAGER.getValue().equals(member.getProjectRole());
    }

    private DesignFileVO toVO(DesignFile file) {
        DesignFileVO vo = new DesignFileVO();
        vo.setId(String.valueOf(file.getId()));
        vo.setProjectId(String.valueOf(file.getProjectId()));
        if (file.getTaskId() != null) vo.setTaskId(String.valueOf(file.getTaskId()));
        vo.setFileName(file.getFileName()); vo.setFileType(file.getFileType());
        vo.setMimeType(file.getMimeType()); vo.setFileSize(file.getFileSize());
        vo.setVersionNo(file.getVersionNo()); vo.setVersionNote(file.getVersionNote());
        vo.setUploaderId(String.valueOf(file.getUploaderId())); vo.setCreateTime(file.getCreateTime());
        User uploader = userMapper.selectById(file.getUploaderId());
        if (uploader != null) vo.setUploaderName(uploader.getUserName());
        vo.setSubmitted(Integer.valueOf(1).equals(file.getSubmittedFlag()));
        return vo;
    }

    private void validateUpload(MultipartFile file, String fileType) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        ThrowUtils.throwIf(!DesignFileTypeEnum.isValid(fileType), ErrorCode.PARAMS_ERROR, "文件类型不合法");
        ThrowUtils.throwIf(file.getSize() <= 0 || file.getSize() > maxFileSize,
                ErrorCode.PARAMS_ERROR, "文件大小不能超过 50MB");
        String name = safeFileName(file.getOriginalFilename());
        String ext = extension(name);
        ThrowUtils.throwIf(!ALLOWED_MIME.containsKey(ext), ErrorCode.PARAMS_ERROR, "不支持该文件扩展名");
        String mime = normalizeMime(file.getContentType());
        ThrowUtils.throwIf(!ALLOWED_MIME.get(ext).contains(mime), ErrorCode.PARAMS_ERROR, "文件扩展名与 MIME 类型不匹配");
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            ThrowUtils.throwIf(!validSignature(ext, header), ErrorCode.PARAMS_ERROR, "文件内容与扩展名不匹配");
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取上传文件失败");
        }
    }

    private boolean validSignature(String ext, byte[] h) {
        return switch (ext) {
            case "jpg", "jpeg" -> starts(h, 0xFF, 0xD8, 0xFF);
            case "png" -> starts(h, 0x89, 0x50, 0x4E, 0x47);
            case "webp" -> ascii(h, 0, "RIFF") && ascii(h, 8, "WEBP");
            case "pdf" -> ascii(h, 0, "%PDF");
            case "ai" -> ascii(h, 0, "%PDF") || ascii(h, 0, "%!PS");
            case "psd" -> ascii(h, 0, "8BPS");
            case "zip", "sketch", "docx", "xlsx", "pptx" -> starts(h, 0x50, 0x4B);
            case "doc", "xls", "ppt" -> starts(h, 0xD0, 0xCF, 0x11, 0xE0);
            case "txt" -> true;
            default -> false;
        };
    }

    private boolean starts(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) if ((bytes[i] & 0xFF) != expected[i]) return false;
        return true;
    }

    private boolean ascii(byte[] bytes, int offset, String value) {
        byte[] expected = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (bytes.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) if (bytes[offset + i] != expected[i]) return false;
        return true;
    }

    private String safeFileName(String original) {
        String value = StrUtil.blankToDefault(original, "file").replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        ThrowUtils.throwIf(value.isBlank() || value.length() > 255 || value.contains("\0"),
                ErrorCode.PARAMS_ERROR, "文件名不合法");
        return value;
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeMime(String mime) {
        if (mime == null) return "";
        int semicolon = mime.indexOf(';');
        return (semicolon < 0 ? mime : mime.substring(0, semicolon)).trim().toLowerCase(Locale.ROOT);
    }
}
