package com.example.aichat.dto.audit;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 内容审核结果 DTO。
 *
 * 由 ContentAuditService 同步审核后返回，写路径据此决定是否放行：
 *   - passed = true：内容放行；
 *   - passed = false：写路径应抛出 BusinessException(SocialErrorCode.CONTENT_VIOLATION)。
 *
 * riskLevel 取命中词库中的最高风险等级（HIGH > MEDIUM > LOW）。
 */
@Data
public class AuditResult {

    /** 是否通过审核。 */
    private boolean passed;

    /** 命中的敏感词列表（去重后）。 */
    private List<String> matchedWords = new ArrayList<>();

    /** 风险等级：LOW / MEDIUM / HIGH；通过时为 null 或 LOW。 */
    private String riskLevel;

    /** 命中原因说明（不回显用户输入原文，仅列出命中词类别）。 */
    private String reason;

    public static AuditResult pass() {
        AuditResult r = new AuditResult();
        r.setPassed(true);
        r.setRiskLevel("LOW");
        return r;
    }

    public static AuditResult reject(List<String> matchedWords, String riskLevel, String reason) {
        AuditResult r = new AuditResult();
        r.setPassed(false);
        r.setMatchedWords(matchedWords == null ? new ArrayList<>() : matchedWords);
        r.setRiskLevel(riskLevel);
        r.setReason(reason);
        return r;
    }
}
