package com.lsp.clothes.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.dto.review.ReviewActionRequest;
import com.lsp.clothes.model.dto.task.TaskQueryRequest;
import com.lsp.clothes.model.entity.ReviewRecord;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.ReviewDetailVO;
import com.lsp.clothes.model.vo.ReviewRecordVO;
import com.lsp.clothes.model.vo.TaskVO;

import java.util.List;

public interface ReviewService extends IRepository<ReviewRecord> {
    Page<TaskVO> pendingPage(TaskQueryRequest request, User loginUser);
    ReviewDetailVO getDetail(Long taskId, User loginUser);
    List<ReviewRecordVO> getHistory(Long taskId, User loginUser);
    void approve(ReviewActionRequest request, User loginUser);
    void reject(ReviewActionRequest request, User loginUser);
}
