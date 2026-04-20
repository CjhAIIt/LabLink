package com.lab.recruitment.config;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.dto.ExamOptionDTO;
import com.lab.recruitment.dto.JudgeCaseDTO;
import com.lab.recruitment.entity.GrowthPracticeQuestion;
import com.lab.recruitment.mapper.GrowthPracticeQuestionMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(3)
@ConditionalOnProperty(value = "app.modules.growth-center.enabled", havingValue = "true")
public class GrowthPracticeQuestionDataInitializer implements CommandLineRunner {

    private static final String QUESTION_SINGLE_CHOICE = "single_choice";
    private static final String QUESTION_FILL_BLANK = "fill_blank";
    private static final String QUESTION_PROGRAMMING = "programming";

    private final GrowthPracticeQuestionMapper growthPracticeQuestionMapper;
    private final JdbcTemplate jdbcTemplate;

    public GrowthPracticeQuestionDataInitializer(GrowthPracticeQuestionMapper growthPracticeQuestionMapper,
                                                 JdbcTemplate jdbcTemplate) {
        this.growthPracticeQuestionMapper = growthPracticeQuestionMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Long creatorId = findSeedCreatorId();
        if (creatorId == null) {
            System.out.println("Skip growth question seed: no available user found.");
            return;
        }

        int inserted = 0;
        for (GrowthPracticeQuestion question : buildSeedQuestions(creatorId)) {
            if (exists(question.getTrackCode(), question.getQuestionType(), question.getTitle())) {
                continue;
            }
            growthPracticeQuestionMapper.insert(question);
            inserted++;
        }

        System.out.println("Growth practice question seed completed, inserted " + inserted + " questions.");
    }

    private Long findSeedCreatorId() {
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM t_user " +
                        "ORDER BY CASE WHEN role IN ('super_admin', 'admin') THEN 0 ELSE 1 END, id LIMIT 1",
                (rs, rowNum) -> rs.getLong(1)
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    private boolean exists(String trackCode, String questionType, String title) {
        QueryWrapper<GrowthPracticeQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("track_code", trackCode)
                .eq("question_type", questionType)
                .eq("title", title);
        return growthPracticeQuestionMapper.selectCount(queryWrapper) > 0;
    }

    private List<GrowthPracticeQuestion> buildSeedQuestions(Long creatorId) {
        List<GrowthPracticeQuestion> questions = new ArrayList<>();

        questions.add(singleChoice(creatorId, "common", "简单", "算法复杂度判断",
                "下列时间复杂度中，随着输入规模增大增长速度最慢的是哪一个？",
                "B", "先排除指数级和平方级，再比较线性对数与线性复杂度。",
                tags("算法基础", "复杂度"),
                "O(n^2)", "O(log n)", "O(n log n)", "O(2^n)"));
        questions.add(fillBlank(creatorId, "common", "简单", "二分查找前提",
                "使用二分查找前，数据通常需要满足 ______ 。",
                answers("有序", "已排序"), "先确认能否根据中间值排除一半区间。",
                tags("算法基础", "二分查找")));
        questions.add(singleChoice(creatorId, "common", "简单", "HTTP 创建资源状态码",
                "接口成功创建一个新资源时，最常用的 HTTP 状态码是哪个？",
                "B", "区分“查询成功”和“创建成功”的语义。",
                tags("通用基础", "HTTP"),
                "200", "201", "204", "304"));
        questions.add(fillBlank(creatorId, "common", "简单", "哈希表核心结构",
                "哈希表通过将 ______ 映射到数组下标来提高查询效率。",
                answers("键", "key"), "从键值对的“键”去理解哈希函数的作用。",
                tags("数据结构", "哈希表")));
        questions.add(programming(creatorId, "common", "简单", "数组元素求和",
                "给定一个长度为 n 的整数数组，请输出所有元素之和。",
                "第一行输入一个整数 n。\n第二行输入 n 个整数。",
                "输出一个整数，表示数组元素之和。",
                "5\n1 2 3 4 5\n",
                "15\n",
                "注意整数累加时不要漏掉负数。",
                tags("入门", "数组"),
                cases("5\n1 2 3 4 5\n", "15\n", "4\n10 -3 7 1\n", "15\n")));
        questions.add(programming(creatorId, "common", "简单", "数组极差",
                "给定一个长度为 n 的整数数组，输出最大值与最小值的差值。",
                "第一行输入一个整数 n。\n第二行输入 n 个整数。",
                "输出一个整数，表示数组中的最大值减最小值。",
                "6\n8 3 10 6 15 4\n",
                "12\n",
                "一次遍历同时维护当前最小值和最大值即可。",
                tags("入门", "遍历"),
                cases("6\n8 3 10 6 15 4\n", "12\n", "3\n5 5 5\n", "0\n")));

        questions.add(singleChoice(creatorId, "backend", "中等", "后端接口幂等设计",
                "支付回调接口需要避免重复扣款，下列哪种方案最稳妥？",
                "C", "关注“同一业务请求多次到达时”的去重手段。",
                tags("后端", "接口设计", "事务"),
                "只在前端按钮上做防重复点击",
                "每次回调都新建一条支付记录",
                "使用业务幂等键并配合唯一约束",
                "将接口超时时间调大"));
        questions.add(fillBlank(creatorId, "backend", "简单", "数据库索引命中规则",
                "在联合索引 (lab_id, status, create_time) 中，查询条件只包含前两列时，仍然符合最左 ______ 匹配原则。",
                answers("前缀", "最左前缀"), "回忆联合索引从左到右依次生效的规则。",
                tags("后端", "数据库", "索引")));
        questions.add(programming(creatorId, "backend", "中等", "接口日志窗口峰值",
                "给定连续 n 分钟的接口请求量，求长度为 k 的连续时间窗口内的最大请求总数。",
                "第一行输入两个整数 n 和 k。\n第二行输入 n 个整数，表示每分钟请求量。",
                "输出一个整数，表示长度为 k 的窗口内最大请求总数。",
                "8 3\n4 2 7 1 6 3 5 2\n",
                "14\n",
                "固定长度窗口优先考虑滑动窗口而不是每次重新求和。",
                tags("后端", "滑动窗口", "日志分析"),
                cases("8 3\n4 2 7 1 6 3 5 2\n", "14\n", "5 2\n1 9 3 4 2\n", "12\n")));

        questions.add(singleChoice(creatorId, "frontend", "中等", "前端状态管理原则",
                "大型前端应用中，下列哪种做法最有利于状态可追踪与调试？",
                "B", "从状态是否集中、变化是否可记录来判断。",
                tags("前端", "状态管理"),
                "所有组件都各自维护一份接口返回数据",
                "重要共享状态集中管理，并让更新路径可追踪",
                "任何状态都放到 localStorage 中",
                "频繁使用深拷贝覆盖整个页面数据"));
        questions.add(fillBlank(creatorId, "frontend", "简单", "输入联想请求优化",
                "在搜索框联想场景中，通常使用 ______ 来减少用户连续输入带来的重复请求。",
                answers("防抖", "debounce"), "关注“停止输入一小段时间后再触发”的行为。",
                tags("前端", "性能优化")));
        questions.add(programming(creatorId, "frontend", "简单", "渲染队列去重",
                "给定一段按时间顺序记录的组件状态编号，请删除相邻重复状态后输出最终状态数量。",
                "第一行输入一个整数 n。\n第二行输入 n 个整数，表示状态编号。",
                "输出一个整数，表示去掉相邻重复状态后的状态数量。",
                "8\n1 1 2 2 3 1 1 4\n",
                "5\n",
                "只需要关心相邻元素是否相同，不必全局去重。",
                tags("前端", "数组", "模拟"),
                cases("8\n1 1 2 2 3 1 1 4\n", "5\n", "5\n7 7 7 7 7\n", "1\n")));

        questions.add(singleChoice(creatorId, "ai", "中等", "模型评估集选择",
                "训练完成后，为了公平评估模型泛化能力，最适合查看哪一部分数据上的指标？",
                "C", "不要用参与训练的数据直接做最终评价。",
                tags("AI", "机器学习"),
                "训练集",
                "训练集和测试集混合后平均",
                "独立测试集",
                "只看损失函数是否下降"));
        questions.add(fillBlank(creatorId, "ai", "简单", "过拟合处理",
                "当模型在训练集表现很好、测试集表现较差时，常称其发生了 ______ 。",
                answers("过拟合"), "关注训练集与测试集表现不一致的典型现象。",
                tags("AI", "机器学习", "泛化")));
        questions.add(programming(creatorId, "ai", "中等", "训练收益最大区间",
                "给定每轮训练带来的收益变化值，请求出收益总和最大的连续区间和。",
                "第一行输入一个整数 n。\n第二行输入 n 个整数，表示每轮收益变化。",
                "输出一个整数，表示收益总和最大的连续区间和。",
                "8\n-2 1 -3 4 -1 2 1 -5\n",
                "6\n",
                "这是典型的连续子段最优问题，可用动态规划或贪心解决。",
                tags("AI", "动态规划", "连续子数组"),
                cases("8\n-2 1 -3 4 -1 2 1 -5\n", "6\n", "5\n1 2 3 4 5\n", "15\n")));

        questions.add(singleChoice(creatorId, "embedded", "中等", "嵌入式实时性关注点",
                "在嵌入式控制程序中，哪项最直接影响任务调度的实时性？",
                "A", "优先考虑任务是否能在规定时间内完成。",
                tags("嵌入式", "实时系统"),
                "任务响应时间与执行周期",
                "注释是否详细",
                "变量名是否够长",
                "界面主题颜色"));
        questions.add(fillBlank(creatorId, "embedded", "简单", "C 语言头文件",
                "在 C 语言中，标准输入输出函数通常声明在 ______ 头文件中。",
                answers("stdio.h", "<stdio.h>"), "想到 printf 和 scanf 来回忆对应头文件。",
                tags("嵌入式", "C语言")));
        questions.add(programming(creatorId, "embedded", "简单", "传感器超阈值计数",
                "给定 n 次传感器读数和一个安全阈值 limit，请统计有多少次读数严格大于该阈值。",
                "第一行输入两个整数 n 和 limit。\n第二行输入 n 个整数，表示读数。",
                "输出一个整数，表示超过阈值的读数次数。",
                "7 50\n45 60 52 49 80 50 77\n",
                "4\n",
                "这是一次顺序扫描统计题，注意比较条件是“严格大于”。",
                tags("嵌入式", "C语言", "遍历"),
                cases("7 50\n45 60 52 49 80 50 77\n", "4\n", "4 10\n1 2 3 4\n", "0\n")));

        questions.add(singleChoice(creatorId, "security", "中等", "最小权限原则",
                "最小权限原则的核心目标是什么？",
                "B", "关注“只给完成当前工作所需的最少权限”。",
                tags("安全", "权限控制"),
                "让所有用户都拥有管理员权限",
                "仅授予完成当前任务所需的最小权限",
                "将所有日志都永久保存",
                "关闭所有接口访问"));
        questions.add(fillBlank(creatorId, "security", "简单", "常见漏洞缩写",
                "跨站脚本攻击常用缩写是 ______ 。",
                answers("XSS", "xss"), "想想最常见的前端注入型漏洞。",
                tags("安全", "Web安全")));
        questions.add(programming(creatorId, "security", "中等", "权限掩码校验",
                "给定 n 个用户权限值和一个目标掩码 mask，请统计有多少个权限值满足 (value & mask) == mask。",
                "第一行输入两个整数 n 和 mask。\n第二行输入 n 个整数，表示权限值。",
                "输出一个整数，表示满足条件的权限值个数。",
                "5 6\n7 2 14 15 4\n",
                "3\n",
                "位运算题，重点是判断某个权限值是否完整包含目标掩码。",
                tags("安全", "位运算", "权限控制"),
                cases("5 6\n7 2 14 15 4\n", "3\n", "4 1\n0 2 4 8\n", "0\n")));

        questions.add(singleChoice(creatorId, "bigdata", "中等", "批处理与流处理",
                "下列哪种场景最适合流处理而不是离线批处理？",
                "D", "关注是否需要低延迟、持续不断地处理数据。",
                tags("大数据", "流处理"),
                "每月汇总历史账单",
                "每季度重新训练画像模型",
                "每天凌晨统一跑报表",
                "实时监控日志并在异常时立即告警"));
        questions.add(fillBlank(creatorId, "bigdata", "简单", "消息队列解耦",
                "在大数据链路中，使用消息队列常见目的是削峰填谷和 ______ 系统耦合。",
                answers("降低", "解耦"), "想想消息队列在系统之间的典型作用。",
                tags("大数据", "消息队列")));
        questions.add(programming(creatorId, "bigdata", "中等", "分片任务峰值负载",
                "给定连续 n 个时间片的分片任务量，求长度为 k 的连续时间片内的最大任务总量。",
                "第一行输入两个整数 n 和 k。\n第二行输入 n 个整数，表示每个时间片的任务量。",
                "输出一个整数，表示长度为 k 的窗口中的最大任务总量。",
                "7 2\n8 3 5 4 9 1 6\n",
                "13\n",
                "这是固定窗口求最值问题，注意窗口滑动时只增减边界元素。",
                tags("大数据", "滑动窗口", "调度"),
                cases("7 2\n8 3 5 4 9 1 6\n", "13\n", "6 4\n1 1 1 1 1 1\n", "4\n")));

        questions.add(singleChoice(creatorId, "analysis", "中等", "指标设计原则",
                "为了比较不同渠道的转化效果，哪种指标最具有可比性？",
                "C", "优先选择能消除样本规模差异的比率型指标。",
                tags("数据分析", "指标体系"),
                "每个渠道的总点击次数",
                "每个渠道的总曝光次数",
                "每个渠道的转化率",
                "每个渠道的运营同学人数"));
        questions.add(fillBlank(creatorId, "analysis", "简单", "数据清洗操作",
                "将缺失值、异常值和重复值处理后再分析，这一过程通常称为数据 ______ 。",
                answers("清洗"), "想想正式建模和分析前的第一步准备工作。",
                tags("数据分析", "数据治理")));
        questions.add(programming(creatorId, "analysis", "简单", "连续增长天数",
                "给定 n 天的指标值，求最长连续严格增长天数长度。",
                "第一行输入一个整数 n。\n第二行输入 n 个整数，表示每天的指标值。",
                "输出一个整数，表示最长连续严格增长天数长度。",
                "7\n10 12 15 11 13 14 18\n",
                "4\n",
                "连续增长要求后一天的值严格大于前一天。",
                tags("数据分析", "数组", "连续区间"),
                cases("7\n10 12 15 11 13 14 18\n", "4\n", "5\n9 8 7 6 5\n", "1\n")));

        questions.add(singleChoice(creatorId, "product", "中等", "需求优先级判断",
                "面对多个候选需求时，产品经理最先需要明确的是什么？",
                "A", "先看价值、目标和资源约束，再决定排期。",
                tags("产品", "需求管理"),
                "业务目标与用户价值",
                "页面背景色",
                "会议室容量",
                "谁先在群里发消息"));
        questions.add(fillBlank(creatorId, "product", "简单", "用户故事模板",
                "常见用户故事模板是：作为某类用户，我想要完成某件事，以便获得某种 ______ 。",
                answers("价值", "收益"), "回忆用户故事强调“做这件事后得到什么”。",
                tags("产品", "需求文档")));
        questions.add(programming(creatorId, "product", "中等", "需求价值最大化",
                "给定 n 个需求的价值分数和可投入名额 m，请输出价值最高的 m 个需求总分。",
                "第一行输入两个整数 n 和 m。\n第二行输入 n 个整数，表示各需求的价值分数。",
                "输出一个整数，表示价值最高的 m 个需求总分。",
                "6 3\n8 4 9 5 10 6\n",
                "27\n",
                "可以先排序再取前 m 个分值。",
                tags("产品", "排序", "优先级"),
                cases("6 3\n8 4 9 5 10 6\n", "27\n", "5 2\n1 2 3 4 5\n", "9\n")));

        questions.add(singleChoice(creatorId, "design", "中等", "设计系统价值",
                "建设设计系统的核心收益是什么？",
                "D", "关注复用、一致性和协同效率。",
                tags("设计", "设计系统"),
                "让每个页面完全不同",
                "减少颜色对比度",
                "取消组件复用",
                "提升规范一致性并降低重复设计成本"));
        questions.add(fillBlank(creatorId, "design", "简单", "界面对齐原则",
                "在视觉设计中，让元素沿同一参考线排列通常称为 ______ 。",
                answers("对齐"), "想想四大版式基础原则之一。",
                tags("设计", "版式")));
        questions.add(programming(creatorId, "design", "中等", "栅格行数计算",
                "给定若干模块宽度和每行最大容量 limit，按输入顺序摆放模块，求最少需要多少行。",
                "第一行输入两个整数 n 和 limit。\n第二行输入 n 个整数，表示各模块宽度。",
                "输出一个整数，表示最少需要的行数。",
                "6 10\n3 4 5 2 6 4\n",
                "3\n",
                "顺序不可改变，当前行放不下时再开新行。",
                tags("设计", "模拟", "布局"),
                cases("6 10\n3 4 5 2 6 4\n", "3\n", "4 8\n2 2 2 2\n", "1\n")));

        questions.add(singleChoice(creatorId, "qa", "中等", "测试用例设计",
                "为了更高效地发现输入边界相关的问题，优先应使用哪种测试思想？",
                "C", "关注最小值、最大值和刚刚越界的位置。",
                tags("测试", "用例设计"),
                "随机把字段都清空",
                "只测试最常见的正常流程",
                "边界值分析",
                "只看产品原型图"));
        questions.add(fillBlank(creatorId, "qa", "简单", "自动化测试目标",
                "自动化测试更适合覆盖高频、重复且规则明确的 ______ 场景。",
                answers("回归", "回归测试"), "想想每次上线都需要重复验证的部分。",
                tags("测试", "自动化")));
        questions.add(programming(creatorId, "qa", "简单", "连续失败批次",
                "给定一批测试结果，1 表示通过、0 表示失败。请输出最长连续失败批次长度。",
                "第一行输入一个整数 n。\n第二行输入 n 个 0 或 1。",
                "输出一个整数，表示最长连续失败批次长度。",
                "10\n1 0 0 1 0 0 0 1 1 0\n",
                "3\n",
                "顺序扫描时统计连续 0 的长度即可。",
                tags("测试", "数组", "统计"),
                cases("10\n1 0 0 1 0 0 0 1 1 0\n", "3\n", "5\n1 1 1 1 1\n", "0\n")));

        questions.add(singleChoice(creatorId, "cloud", "中等", "云原生可观测性",
                "为了快速定位线上故障，下列哪组数据最能构成完整的可观测性基础？",
                "B", "观察监控、日志和链路追踪是否同时具备。",
                tags("云平台", "可观测性"),
                "只看 CPU 占用",
                "指标、日志和链路追踪",
                "只看报警短信",
                "只看容器名称"));
        questions.add(fillBlank(creatorId, "cloud", "简单", "容器编排平台",
                "目前最常见的容器编排平台名称是 ______ 。",
                answers("Kubernetes", "kubernetes", "K8s", "k8s"), "想想日常说的 K8s 全称。",
                tags("云平台", "容器")));
        questions.add(programming(creatorId, "cloud", "中等", "实例扩缩容峰值",
                "给定每分钟请求负载和单实例承载上限 limit，求整段时间内最多需要多少个实例。",
                "第一行输入两个整数 n 和 limit。\n第二行输入 n 个整数，表示每分钟负载。",
                "输出一个整数，表示峰值时刻所需的最少实例数。",
                "5 10\n8 12 25 7 19\n",
                "3\n",
                "每个时刻需要的实例数等于向上取整(load / limit)。",
                tags("云平台", "模拟", "扩缩容"),
                cases("5 10\n8 12 25 7 19\n", "3\n", "4 5\n1 2 3 4\n", "1\n")));

        questions.add(singleChoice(creatorId, "game", "中等", "游戏主循环关注点",
                "游戏主循环中最关键的职责之一是什么？",
                "A", "从输入、更新和渲染三个阶段去思考。",
                tags("游戏", "实时交互"),
                "按固定节奏驱动状态更新与渲染",
                "只负责读取配置文件",
                "只在退出时保存数据",
                "让所有逻辑都在网络线程中运行"));
        questions.add(fillBlank(creatorId, "game", "简单", "帧率单位",
                "游戏里常说的 FPS，全称是 Frames Per ______ 。",
                answers("Second", "second", "秒"), "想想每秒显示多少帧。",
                tags("游戏", "基础概念")));
        questions.add(programming(creatorId, "game", "简单", "角色连击统计",
                "给定一串按时间顺序记录的伤害值，正数表示命中，0 或负数表示连击中断。请输出最长连续命中长度。",
                "第一行输入一个整数 n。\n第二行输入 n 个整数，表示连续动作的伤害值。",
                "输出一个整数，表示最长连续命中长度。",
                "9\n10 12 -1 8 6 3 0 5 7\n",
                "3\n",
                "当伤害值小于等于 0 时，当前连击长度应归零。",
                tags("游戏", "数组", "连续区间"),
                cases("9\n10 12 -1 8 6 3 0 5 7\n", "3\n", "4\n-1 -2 -3 -4\n", "0\n")));

        appendExtendedSeedQuestions(questions, creatorId);
        return questions;
    }

    private void appendExtendedSeedQuestions(List<GrowthPracticeQuestion> questions, Long creatorId) {
        questions.add(singleChoice(creatorId, "common", "简单", "队列访问顺序",
                "队列这一数据结构最典型的访问顺序是什么？",
                "A", "从入队和出队发生的先后关系去理解。",
                tags("数据结构", "队列"),
                "先进先出", "后进先出", "随机访问", "只进不出"));
        questions.add(fillBlank(creatorId, "common", "简单", "栈的特性",
                "栈这种数据结构最典型的访问顺序是后进 ______ 。",
                answers("先出"), "想到函数调用栈的弹出顺序。",
                tags("数据结构", "栈")));
        questions.add(programming(creatorId, "common", "简单", "统计偶数个数",
                "给定 n 个整数，请统计其中有多少个偶数。",
                "第一行输入一个整数 n。\n第二行输入 n 个整数。",
                "输出一个整数，表示偶数的个数。",
                "6\n1 2 3 4 5 6\n",
                "3\n",
                "遍历数组时用 value % 2 == 0 判断偶数。",
                tags("入门", "遍历", "统计"),
                cases("6\n1 2 3 4 5 6\n", "3\n", "5\n7 9 11 13 15\n", "0\n")));

        questions.add(singleChoice(creatorId, "backend", "中等", "缓存穿透治理",
                "为了降低缓存穿透带来的数据库压力，下列哪种方案更合适？",
                "B", "关注“请求的数据根本不存在”时如何减少反复落库。",
                tags("后端", "缓存", "高并发"),
                "把数据库超时时间调大",
                "对空结果做短期缓存并校验参数合法性",
                "把所有接口都改成同步串行执行",
                "每次请求前先重启缓存服务"));
        questions.add(fillBlank(creatorId, "backend", "简单", "事务四大特性",
                "数据库事务常说的四大特性缩写是 ______ 。",
                answers("ACID", "acid"), "回忆原子性、一致性、隔离性、持久性。",
                tags("后端", "数据库", "事务")));
        questions.add(programming(creatorId, "backend", "中等", "慢请求计数",
                "给定 n 条接口耗时记录和一个阈值 limit，请统计有多少条请求耗时大于等于该阈值。",
                "第一行输入两个整数 n 和 limit。\n第二行输入 n 个整数，表示请求耗时。",
                "输出一个整数，表示慢请求数量。",
                "7 200\n120 220 180 305 90 210 199\n",
                "3\n",
                "本题本质是一次顺序扫描计数，注意比较条件是大于等于。",
                tags("后端", "日志分析", "统计"),
                cases("7 200\n120 220 180 305 90 210 199\n", "3\n", "4 100\n100 100 100 100\n", "4\n")));

        questions.add(singleChoice(creatorId, "frontend", "中等", "虚拟 DOM 价值",
                "前端框架中引入虚拟 DOM 的主要价值之一是什么？",
                "C", "关注视图更新时如何减少直接操作真实 DOM 的成本。",
                tags("前端", "框架原理"),
                "让所有组件都不需要状态",
                "把 CSS 自动改成响应式布局",
                "通过差异比对减少不必要的真实 DOM 更新",
                "让接口请求永远并行执行"));
        questions.add(fillBlank(creatorId, "frontend", "简单", "懒加载目标",
                "为了提升首屏体验，通常会把非关键资源延后 ______ 。",
                answers("加载"), "想想图片懒加载和路由懒加载的共同点。",
                tags("前端", "性能优化")));
        questions.add(programming(creatorId, "frontend", "简单", "高度波动统计",
                "给定连续 n 次渲染的容器高度和阈值 limit，请统计相邻两次高度差绝对值大于 limit 的次数。",
                "第一行输入两个整数 n 和 limit。\n第二行输入 n 个整数，表示每次渲染高度。",
                "输出一个整数，表示高度波动次数。",
                "6 20\n100 130 118 160 170 120\n",
                "3\n",
                "按顺序比较相邻两项的差值绝对值即可。",
                tags("前端", "数组", "模拟"),
                cases("6 20\n100 130 118 160 170 120\n", "3\n", "5 10\n50 55 58 60 63\n", "0\n")));

        questions.add(singleChoice(creatorId, "ai", "中等", "精确率含义",
                "在二分类任务中，精确率更关注下面哪种含义？",
                "D", "从“被预测为正类的样本里有多少是真的正类”去理解。",
                tags("AI", "模型评估"),
                "所有样本中预测正确的比例",
                "真实正类中被找出的比例",
                "训练损失下降的速度",
                "预测为正类的样本中真正为正类的比例"));
        questions.add(fillBlank(creatorId, "ai", "简单", "监督学习要素",
                "监督学习通常依赖带有 ______ 的训练样本。",
                answers("标签", "label", "labels"), "想想分类和回归训练时必须提供什么信息。",
                tags("AI", "机器学习", "基础概念")));
        questions.add(programming(creatorId, "ai", "中等", "最长连续正确预测",
                "给定 n 天的模型预测结果，1 表示预测正确，0 表示预测错误。请输出最长连续正确预测天数。",
                "第一行输入一个整数 n。\n第二行输入 n 个 0 或 1。",
                "输出一个整数，表示最长连续正确预测天数。",
                "9\n1 1 0 1 1 1 0 1 1\n",
                "3\n",
                "扫描数组时维护当前连续 1 的长度和历史最大值。",
                tags("AI", "数组", "连续区间"),
                cases("9\n1 1 0 1 1 1 0 1 1\n", "3\n", "4\n0 0 0 0\n", "0\n")));

        questions.add(singleChoice(creatorId, "security", "中等", "SQL 注入防护方式",
                "下列哪种做法最能有效降低 SQL 注入风险？",
                "B", "重点看是否把用户输入与 SQL 结构做了隔离。",
                tags("安全", "Web安全", "数据库"),
                "把 SQL 语句写得更长",
                "使用参数化查询或预编译语句",
                "把数据库端口改成其他数字",
                "只在前端页面做输入必填校验"));
        questions.add(fillBlank(creatorId, "security", "简单", "跨站请求伪造简称",
                "跨站请求伪造常用缩写是 ______ 。",
                answers("CSRF", "csrf"), "和 XSS 区分开，这个漏洞利用的是用户已登录状态。",
                tags("安全", "Web安全")));
        questions.add(programming(creatorId, "security", "简单", "异常登录告警计数",
                "给定 n 天的异常登录次数和告警阈值 limit，请统计触发告警的天数。",
                "第一行输入两个整数 n 和 limit。\n第二行输入 n 个整数，表示每天的异常登录次数。",
                "输出一个整数，表示触发告警的天数。",
                "6 5\n2 5 7 1 8 5\n",
                "4\n",
                "只要当天次数大于等于阈值就记为一次告警。",
                tags("安全", "日志审计", "统计"),
                cases("6 5\n2 5 7 1 8 5\n", "4\n", "5 10\n1 2 3 4 5\n", "0\n")));

        questions.add(singleChoice(creatorId, "cloud", "中等", "灰度发布目标",
                "灰度发布最核心的目标是什么？",
                "A", "关注在控制风险的前提下逐步验证新版本。",
                tags("云平台", "发布策略"),
                "逐步放量验证新版本并控制上线风险",
                "让所有实例同时重启",
                "取消监控与回滚机制",
                "只保留一个生产节点"));
        questions.add(fillBlank(creatorId, "cloud", "简单", "服务发现组件",
                "在微服务体系中，用于服务注册与发现的组件常被称为服务 ______ 。",
                answers("注册中心", "registry", "发现中心"), "想想客户端如何知道服务实例地址。",
                tags("云平台", "微服务")));
        questions.add(programming(creatorId, "cloud", "中等", "连续失败实例批次",
                "给定某批实例按时间顺序记录的健康检查结果，1 表示成功，0 表示失败。请输出最长连续失败长度。",
                "第一行输入一个整数 n。\n第二行输入 n 个 0 或 1。",
                "输出一个整数，表示最长连续失败长度。",
                "10\n1 0 0 1 0 0 0 1 1 0\n",
                "3\n",
                "按顺序统计连续 0 的长度，遇到 1 时归零。",
                tags("云平台", "健康检查", "连续区间"),
                cases("10\n1 0 0 1 0 0 0 1 1 0\n", "3\n", "5\n1 1 1 1 1\n", "0\n")));
    }

    private GrowthPracticeQuestion singleChoice(Long creatorId,
                                                String trackCode,
                                                String difficulty,
                                                String title,
                                                String content,
                                                String correctAnswer,
                                                String analysisHint,
                                                List<String> tags,
                                                String... optionTexts) {
        GrowthPracticeQuestion question = baseQuestion(creatorId, trackCode, QUESTION_SINGLE_CHOICE, difficulty, title, content, analysisHint, tags);
        List<ExamOptionDTO> options = new ArrayList<>();
        for (int index = 0; index < optionTexts.length; index++) {
            ExamOptionDTO option = new ExamOptionDTO();
            option.setLabel(String.valueOf((char) ('A' + index)));
            option.setText(optionTexts[index]);
            options.add(option);
        }
        question.setOptionsJson(JSON.toJSONString(options));
        question.setAnswerConfig(correctAnswer);
        return question;
    }

    private GrowthPracticeQuestion fillBlank(Long creatorId,
                                             String trackCode,
                                             String difficulty,
                                             String title,
                                             String content,
                                             List<String> acceptableAnswers,
                                             String analysisHint,
                                             List<String> tags) {
        GrowthPracticeQuestion question = baseQuestion(creatorId, trackCode, QUESTION_FILL_BLANK, difficulty, title, content, analysisHint, tags);
        question.setAnswerConfig(JSON.toJSONString(acceptableAnswers));
        return question;
    }

    private GrowthPracticeQuestion programming(Long creatorId,
                                               String trackCode,
                                               String difficulty,
                                               String title,
                                               String content,
                                               String inputFormat,
                                               String outputFormat,
                                               String sampleInput,
                                               String sampleOutput,
                                               String analysisHint,
                                               List<String> tags,
                                               List<JudgeCaseDTO> judgeCases) {
        GrowthPracticeQuestion question = baseQuestion(creatorId, trackCode, QUESTION_PROGRAMMING, difficulty, title, content, analysisHint, tags);
        question.setInputFormat(inputFormat);
        question.setOutputFormat(outputFormat);
        Map<String, String> sampleCase = new LinkedHashMap<>();
        sampleCase.put("input", sampleInput);
        sampleCase.put("output", sampleOutput);
        question.setSampleCaseJson(JSON.toJSONString(sampleCase));
        question.setProgramLanguages(JSON.toJSONString(Arrays.asList("c", "cpp", "java", "python")));
        question.setJudgeCaseJson(JSON.toJSONString(judgeCases));
        return question;
    }

    private GrowthPracticeQuestion baseQuestion(Long creatorId,
                                                String trackCode,
                                                String questionType,
                                                String difficulty,
                                                String title,
                                                String content,
                                                String analysisHint,
                                                List<String> tags) {
        GrowthPracticeQuestion question = new GrowthPracticeQuestion();
        question.setCreatorId(creatorId);
        question.setTrackCode(trackCode);
        question.setQuestionType(questionType);
        question.setDifficulty(difficulty);
        question.setTitle(title);
        question.setContent(content);
        question.setAnalysisHint(analysisHint);
        question.setTagsJson(JSON.toJSONString(tags));
        question.setStatus(1);
        return question;
    }

    private List<String> tags(String... values) {
        return Arrays.asList(values);
    }

    private List<String> answers(String... values) {
        return Arrays.asList(values);
    }

    private List<JudgeCaseDTO> cases(String... pairs) {
        List<JudgeCaseDTO> result = new ArrayList<>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            JudgeCaseDTO judgeCase = new JudgeCaseDTO();
            judgeCase.setInput(pairs[index]);
            judgeCase.setOutput(pairs[index + 1]);
            result.add(judgeCase);
        }
        return result;
    }
}
