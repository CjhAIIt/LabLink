package com.lab.recruitment.service.impl;

import com.lab.recruitment.config.AppSecurityProperties;
import com.lab.recruitment.dto.JudgeCaseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在线判题的开关与限额行为测试。
 *
 * <p>判题会执行用户提交的代码，默认必须关闭；关闭时接口要返回明确的不可用状态，
 * 而不是静默执行。</p>
 */
class CodeJudgeServiceTest {

    private CodeJudgeService codeJudgeService;
    private AppSecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        securityProperties = new AppSecurityProperties();
        codeJudgeService = new CodeJudgeService();
        ReflectionTestUtils.setField(codeJudgeService, "securityProperties", securityProperties);
    }

    private static List<JudgeCaseDTO> sampleCases() {
        JudgeCaseDTO judgeCase = new JudgeCaseDTO();
        judgeCase.setInput("1 2");
        judgeCase.setOutput("3");
        return Collections.singletonList(judgeCase);
    }

    @Test
    @DisplayName("判题默认关闭")
    void shouldBeDisabledByDefault() {
        assertFalse(securityProperties.getJudge().isEnabled(),
                "在线判题必须默认关闭，避免未经评估就执行用户代码");
    }

    @Test
    @DisplayName("关闭时判题接口返回不可用")
    void shouldReturnUnavailableWhenDisabled() {
        CodeJudgeService.JudgeResult result = codeJudgeService.judge("python", "print(3)", sampleCases());

        assertFalse(result.isAvailable(), "关闭时应返回不可用");
        assertFalse(result.isSuccess());
        assertEquals(0, result.getPassedCount());
        assertTrue(result.getMessage().contains("未开启"), "应说明判题未开启：" + result.getMessage());
    }

    @Test
    @DisplayName("关闭时直接运行接口返回不可用")
    void shouldReturnUnavailableRunResultWhenDisabled() {
        CodeJudgeService.RunResult result = codeJudgeService.run("python", "print(3)", "");

        assertFalse(result.isAvailable(), "关闭时应返回不可用");
        assertTrue(result.getError() != null && result.getError().contains("未开启"),
                "应说明判题未开启：" + result.getError());
    }

    @Test
    @DisplayName("判题超时与内存上限可配置")
    void shouldExposeConfigurableLimits() {
        securityProperties.getJudge().setTimeoutSeconds(12);
        securityProperties.getJudge().setMaxMemoryMb(256);
        securityProperties.getJudge().setMaxOutputBytes(1024);

        assertEquals(12, securityProperties.getJudge().getTimeoutSeconds());
        assertEquals(256, securityProperties.getJudge().getMaxMemoryMb());
        assertEquals(1024, securityProperties.getJudge().getMaxOutputBytes());
    }

    @Test
    @DisplayName("评分按通过用例数折算")
    void shouldCalculateScoreByPassedCases() {
        CodeJudgeService.JudgeResult result = new CodeJudgeService.JudgeResult();
        result.setTotalCount(4);
        result.setPassedCount(3);

        java.math.BigDecimal score = codeJudgeService.calculateScore(100, result);

        assertEquals(0, new java.math.BigDecimal("75").compareTo(score), "应折算为 75 分，实际：" + score);
    }
}
