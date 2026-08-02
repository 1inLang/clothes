package com.lsp.clothes.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.entity.DesignFile;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.DesignFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DesignFileService extends IRepository<DesignFile> {
    Long upload(MultipartFile file, Long projectId, Long taskId, String fileType,
                String versionNote, User loginUser);
    List<DesignFileVO> listFiles(Long projectId, Long taskId, String fileType, User loginUser);
    List<DesignFileVO> listVersions(Long taskId, User loginUser);
    DesignFileVO getFileVO(Long id, User loginUser);
    FileDownload download(Long id, User loginUser);
    void deleteFile(Long id, User loginUser);
}
