package com.tsd.csm.modules.file.controller;

import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.file.domain.vo.UploadVO;
import com.tsd.csm.modules.file.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传：客服端 / 用户端发送图片、视频等多媒体消息前先上传，得到地址后作为消息内容发送。
 *
 * <p>路径 {@code /api/file/**} 经 AuthInterceptor 鉴权，客服 token 与 H5 会话凭证均可访问。
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传单个文件。
     * @param file 文件
     * @return 上传结果（含可访问 url）
     */
    @PostMapping("/upload")
    public R<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(fileStorageService.store(file, TenantContext.getAppId()));
    }
}
