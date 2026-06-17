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

    public PageResult<QaVO> page(QaQuery query) {
        Page<Qa> page = lambdaQuery()
                .eq(query.getStatus() != null, Qa::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Qa::getQuestion, query.getKeyword())
                .orderByDesc(Qa::getId)
                .page(Page.of(query.getCurrent(), query.getSize()));
        return PageResult.of(page, this::toVO);
    }

    public QaVO detail(Long id) {
        return toVO(getOwned(id));
    }

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

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOwned(id);
        removeById(id);
        qaKeywordMapper.delete(new LambdaQueryWrapper<QaKeyword>().eq(QaKeyword::getQaId, id));
    }

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

    private Qa getOwned(Long id) {
        Qa qa = getById(id);
        if (qa == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return qa;
    }

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
