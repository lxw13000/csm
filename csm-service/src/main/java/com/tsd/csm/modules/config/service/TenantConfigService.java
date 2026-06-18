package com.tsd.csm.modules.config.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.modules.config.domain.TenantConfig;
import com.tsd.csm.modules.config.domain.dto.TenantConfigUpdateDTO;
import com.tsd.csm.modules.config.mapper.TenantConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 租户级配置服务（每租户一行）。读写均按 {@code TenantContext.appId} 隔离。
 * 供派单（max_concurrent）、自动完结（auto_close_minutes）等读取阈值。
 */
@Service
public class TenantConfigService extends ServiceImpl<TenantConfigMapper, TenantConfig> {

    /**
     * 取当前租户配置；不存在则按默认值惰性创建。要求租户上下文已设置。
     */
    public TenantConfig getCurrent() {
        TenantConfig config = lambdaQuery().one();
        if (config == null) {
            config = new TenantConfig();
            config.setMaxConcurrent(0);
            config.setAutoCloseMinutes(15);
            config.setNotifySound(1);
            save(config);
        }
        return config;
    }

    /**
     * 更新当前租户配置（notifySound 为空则保留原值）。
     *
     * @param dto 配置项
     * @return 更新后的配置
     */
    public TenantConfig update(TenantConfigUpdateDTO dto) {
        TenantConfig config = getCurrent();
        config.setMaxConcurrent(dto.getMaxConcurrent());
        config.setAutoCloseMinutes(dto.getAutoCloseMinutes());
        if (dto.getNotifySound() != null) {
            config.setNotifySound(dto.getNotifySound());
        }
        // ext 为 JSON 列，空串不是合法 JSON，置 null 以免 MySQL 报 "The document is empty."
        config.setExt(StringUtils.hasText(dto.getExt()) ? dto.getExt() : null);
        updateById(config);
        return config;
    }
}
