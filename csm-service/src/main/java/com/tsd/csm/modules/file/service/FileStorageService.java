package com.tsd.csm.modules.file.service;

import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.config.CsmProperties;
import com.tsd.csm.modules.file.domain.vo.UploadVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储服务：保存上传文件到 {@code csm.upload.dir/appId/yyyyMM/uuid.ext}，
 * 返回经 {@code /files/**} 访问的相对地址（聊天消息以该地址作为内容）。
 *
 * <p>多媒体消息（图片/视频/其他）即「上传得到地址 → 作为消息 content 发送」，
 * 与 service 的 ContentType（1 文本 / 2 图片 / 3 其他多媒体）配合。
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path baseDir;

    public FileStorageService(CsmProperties properties) {
        this.baseDir = Paths.get(properties.getUpload().getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            log.warn("创建上传根目录失败 dir={}, err={}", baseDir, e.getMessage());
        }
    }

    /** 上传根目录的绝对路径（供静态资源映射）。 */
    public Path getBaseDir() {
        return baseDir;
    }

    /**
     * 保存上传文件，返回可访问地址。
     * @param file 上传文件
     * @param appId 租户标识（用于目录隔离），可空
     * @return 上传结果（含相对 url）
     */
    public UploadVO store(MultipartFile file, String appId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空");
        }
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
        }
        String safeApp = (appId == null || appId.isBlank())
                ? "common" : appId.replaceAll("[^A-Za-z0-9_-]", "_");
        String strDay = LocalDate.now().format(DAY);
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = baseDir.resolve(safeApp).resolve(strDay);
        Path target = dir.resolve(storedName);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("保存上传文件失败 target={}, err={}", target, e.getMessage());
            throw new BizException("文件保存失败");
        }

        UploadVO vo = new UploadVO();
        vo.setUrl("/files/" + safeApp + "/" + strDay + "/" + storedName);
        vo.setName(original);
        vo.setSize(file.getSize());
        return vo;
    }
}
