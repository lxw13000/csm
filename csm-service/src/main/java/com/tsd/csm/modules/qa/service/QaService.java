package com.tsd.csm.modules.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.ResultCode;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.security.UserContext;
import com.tsd.csm.modules.qa.domain.Qa;
import com.tsd.csm.modules.qa.domain.QaKeyword;
import com.tsd.csm.modules.qa.domain.dto.QaQuery;
import com.tsd.csm.modules.qa.domain.dto.QaSaveDTO;
import com.tsd.csm.modules.qa.domain.vo.QaVO;
import com.tsd.csm.modules.qa.mapper.QaKeywordMapper;
import com.tsd.csm.modules.qa.mapper.QaMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QA 知识库服务（本租户独立、完全隔离）。提供 CRUD 与智能问答最优匹配。
 */
@Service
public class QaService extends ServiceImpl<QaMapper, Qa> {

    private final QaKeywordMapper qaKeywordMapper;

    public QaService(QaKeywordMapper qaKeywordMapper) {
        this.qaKeywordMapper = qaKeywordMapper;
    }

    /**
     * 分页查询本租户 QA。
     * @param query 查询条件
     * @return QA 分页结果
     */
    public PageResult<QaVO> page(QaQuery query) {
        Page<Qa> page = lambdaQuery()
                .eq(query.getStatus() != null, Qa::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Qa::getQuestion, query.getKeyword())
                .orderByDesc(Qa::getId)
                .page(Page.of(query.getCurrent(), query.getSize()));
        return PageResult.of(page, this::toVO);
    }

    /**
     * QA 详情（含关联关键词）。
     * @param id QA id
     * @return QA 详情
     */
    public QaVO detail(Long id) {
        return toVO(getOwned(id));
    }

    /**
     * 新增 QA（记录创建人并写入关键词）。
     * @param dto QA 信息
     * @return 新增的 QA VO
     */
    @Transactional(rollbackFor = Exception.class)
    public QaVO create(QaSaveDTO dto) {
        Qa qa = new Qa();
        qa.setQuestion(dto.getQuestion());
        qa.setAnswer(dto.getAnswer());
        qa.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        LoginUser current = UserContext.get();
        if (current != null) {
            qa.setCreatedBy(current.getAccountId());
        }
        save(qa);
        replaceKeywords(qa.getId(), dto.getKeywords());
        return toVO(qa);
    }

    /**
     * 编辑 QA（keywords 非空时一并重设关键词）。
     * @param id QA id
     * @param dto QA 信息
     * @return 更新后的 QA VO
     */
    @Transactional(rollbackFor = Exception.class)
    public QaVO update(Long id, QaSaveDTO dto) {
        Qa qa = getOwned(id);
        qa.setQuestion(dto.getQuestion());
        qa.setAnswer(dto.getAnswer());
        if (dto.getStatus() != null) {
            qa.setStatus(dto.getStatus());
        }
        updateById(qa);
        if (dto.getKeywords() != null) {
            replaceKeywords(id, dto.getKeywords());
        }
        return toVO(qa);
    }

    /**
     * 删除 QA 并清除其关键词。
     * @param id QA id
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOwned(id);
        removeById(id);
        qaKeywordMapper.delete(new LambdaQueryWrapper<QaKeyword>().eq(QaKeyword::getQaId, id));
    }

    /**
     * 启用/停用 QA。
     * @param id QA id
     * @param status 状态：1 启用 / 0 停用
     */
    public void changeStatus(Long id, Integer status) {
        Qa qa = getOwned(id);
        qa.setStatus(status);
        updateById(qa);
    }

    /**
     * 智能问答最优匹配：先按关键词命中数排序，再以问题包含兜底，仅返回启用中的问答对；无命中返回 null。
     * 全程经租户拦截器按 app_id 隔离。
     */
    public Qa matchBest(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        List<QaKeyword> keywords = qaKeywordMapper.selectList(null);
        Map<Long, Integer> hitCount = new HashMap<>();
        for (QaKeyword keyword : keywords) {
            if (StringUtils.hasText(keyword.getKeyword()) && text.contains(keyword.getKeyword())) {
                hitCount.merge(keyword.getQaId(), 1, Integer::sum);
            }
        }
        Long bestQaId = hitCount.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (bestQaId != null) {
            Qa qa = getById(bestQaId);
            if (qa != null && qa.getStatus() != null && qa.getStatus() == 1) {
                return qa;
            }
        }
        return lambdaQuery().eq(Qa::getStatus, 1)
                .like(Qa::getQuestion, text)
                .last("limit 1")
                .one();
    }

    /** 全量替换 QA 的关键词（先清后插，跳过空白）。 */
    private void replaceKeywords(Long qaId, List<String> keywords) {
        qaKeywordMapper.delete(new LambdaQueryWrapper<QaKeyword>().eq(QaKeyword::getQaId, qaId));
        if (keywords == null) {
            return;
        }
        for (String word : keywords) {
            if (!StringUtils.hasText(word)) {
                continue;
            }
            QaKeyword keyword = new QaKeyword();
            keyword.setQaId(qaId);
            keyword.setKeyword(word.trim());
            qaKeywordMapper.insert(keyword);
        }
    }

    /** 取本租户内 QA，不存在抛 404。 */
    private Qa getOwned(Long id) {
        Qa qa = getById(id);
        if (qa == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return qa;
    }

    /** QA 实体转展示 VO（附带关联关键词）。 */
    private QaVO toVO(Qa qa) {
        QaVO vo = new QaVO();
        vo.setId(qa.getId());
        vo.setQuestion(qa.getQuestion());
        vo.setAnswer(qa.getAnswer());
        vo.setStatus(qa.getStatus());
        vo.setCreatedAt(qa.getCreatedAt());
        List<String> words = new ArrayList<>(qaKeywordMapper
                .selectList(new LambdaQueryWrapper<QaKeyword>().eq(QaKeyword::getQaId, qa.getId()))
                .stream().map(QaKeyword::getKeyword).toList());
        vo.setKeywords(words);
        return vo;
    }
}
