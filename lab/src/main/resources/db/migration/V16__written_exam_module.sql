-- V10__written_exam_module.sql
-- 笔试中心模块完整表结构

-- 1. 笔试表
CREATE TABLE IF NOT EXISTS t_written_exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '笔试名称',
    lab_id BIGINT NOT NULL COMMENT '所属实验室ID',
    description TEXT COMMENT '考试说明',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    duration INT NOT NULL DEFAULT 60 COMMENT '考试时长(分钟)',
    total_score INT NOT NULL DEFAULT 100 COMMENT '总分',
    pass_score INT NOT NULL DEFAULT 60 COMMENT '及格分',
    status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published/ongoing/ended',
    enable_anti_cheat TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否开启防作弊',
    enable_signature TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否开启签名确认',
    allow_retry TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许多次作答',
    max_switch_count INT NOT NULL DEFAULT 5 COMMENT '最大切屏次数(超过强制交卷)',
    created_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_lab_id (lab_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔试表';

-- 2. 题目表
CREATE TABLE IF NOT EXISTS t_written_exam_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL COMMENT '题目标题',
    content TEXT COMMENT '题目内容(支持Markdown)',
    question_type VARCHAR(30) NOT NULL COMMENT '题型: single_choice/multi_choice/judge/fill_blank/short_answer/programming',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度: easy/medium/hard',
    score INT NOT NULL DEFAULT 10 COMMENT '默认分值',
    lab_id BIGINT COMMENT '所属实验室(NULL为公共题库)',
    options JSON COMMENT '选项(选择题): [{label:"A",text:"xxx"}]',
    correct_answer VARCHAR(500) COMMENT '标准答案',
    analysis TEXT COMMENT '解析',
    input_format TEXT COMMENT '编程题输入格式',
    output_format TEXT COMMENT '编程题输出格式',
    sample_case JSON COMMENT '样例: {input:"",output:""}',
    test_cases JSON COMMENT '测试用例: [{input:"",output:"",score:10}]',
    allowed_languages JSON COMMENT '支持语言: ["java","python","c","cpp"]',
    tags JSON COMMENT '标签数组',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    created_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_lab_id (lab_id),
    INDEX idx_question_type (question_type),
    INDEX idx_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 3. 试卷题目关联表
CREATE TABLE IF NOT EXISTS t_written_exam_paper_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL COMMENT '笔试ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    score INT NOT NULL DEFAULT 10 COMMENT '本题分值(可覆盖默认)',
    sort_order INT DEFAULT 0 COMMENT '题目顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exam_question (exam_id, question_id),
    INDEX idx_exam_id (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- 4. 学生考试记录表(attempt)
CREATE TABLE IF NOT EXISTS t_written_exam_attempt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL COMMENT '笔试ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT '状态: in_progress/submitted/grading/graded/published',
    start_time DATETIME COMMENT '开始答题时间',
    submit_time DATETIME COMMENT '提交时间',
    auto_score INT DEFAULT 0 COMMENT '客观题自动得分',
    manual_score INT DEFAULT 0 COMMENT '主观题人工得分',
    total_score INT DEFAULT 0 COMMENT '总分',
    passed TINYINT(1) DEFAULT 0 COMMENT '是否通过',
    switch_count INT DEFAULT 0 COMMENT '切屏次数',
    refresh_count INT DEFAULT 0 COMMENT '刷新次数',
    graded_by BIGINT COMMENT '阅卷人ID',
    graded_time DATETIME COMMENT '阅卷时间',
    remark TEXT COMMENT '阅卷备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_exam_student (exam_id, student_id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_student_id (student_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生考试记录表';

-- 5. 学生答案表
CREATE TABLE IF NOT EXISTS t_written_exam_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL COMMENT '考试记录ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    answer TEXT COMMENT '文本答案(选择/填空)',
    code TEXT COMMENT '编程题代码',
    language VARCHAR(20) COMMENT '编程语言',
    score INT COMMENT '本题得分(阅卷后填入)',
    is_correct TINYINT(1) COMMENT '是否正确(自动判分)',
    judge_result JSON COMMENT '判题结果: {status,stdout,stderr,time,memory}',
    grader_remark VARCHAR(500) COMMENT '阅卷备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_attempt_question (attempt_id, question_id),
    INDEX idx_attempt_id (attempt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生答案表';

-- 6. 签名确认表
CREATE TABLE IF NOT EXISTS t_written_exam_signature (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL COMMENT '笔试ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    signature_name VARCHAR(50) NOT NULL COMMENT '签名姓名',
    signature_image VARCHAR(500) COMMENT '手写签名图片URL(预留)',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '浏览器UA',
    sign_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '签名时间',
    UNIQUE KEY uk_exam_student (exam_id, student_id),
    INDEX idx_exam_id (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签名确认表';

-- 7. 防作弊日志表
CREATE TABLE IF NOT EXISTS t_written_exam_cheat_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL COMMENT '笔试ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    attempt_id BIGINT COMMENT '考试记录ID',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型: tab_switch/refresh/copy/paste/focus_lost/forced_submit',
    detail TEXT COMMENT '事件详情',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exam_id (exam_id),
    INDEX idx_student_id (student_id),
    INDEX idx_attempt_id (attempt_id),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='防作弊日志表';

-- 8. 成绩表(冗余快照,发布后生成)
CREATE TABLE IF NOT EXISTS t_written_exam_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL COMMENT '笔试ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    attempt_id BIGINT NOT NULL COMMENT '考试记录ID',
    total_score INT NOT NULL DEFAULT 0 COMMENT '总分',
    passed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通过',
    rank_no INT COMMENT '排名',
    published TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
    publish_time DATETIME COMMENT '发布时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exam_student (exam_id, student_id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_total_score (total_score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

-- 9. 面试邀请表
CREATE TABLE IF NOT EXISTS interview_invitation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT COMMENT '关联笔试ID',
    lab_id BIGINT NOT NULL COMMENT '实验室ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    title VARCHAR(200) COMMENT '面试标题',
    description TEXT COMMENT '面试说明',
    interview_time DATETIME COMMENT '面试时间',
    location VARCHAR(200) COMMENT '面试地点',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/accepted/rejected/completed',
    student_confirmed TINYINT(1) DEFAULT 0 COMMENT '学生是否确认',
    confirm_time DATETIME COMMENT '确认时间',
    sent_by BIGINT COMMENT '发送人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_lab_id (lab_id),
    INDEX idx_student_id (student_id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试邀请表';

-- 10. 考试进度暂存表(支持刷新恢复)
CREATE TABLE IF NOT EXISTS written_exam_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answers_json JSON COMMENT '答案快照JSON',
    remaining_seconds INT COMMENT '剩余秒数',
    current_index INT DEFAULT 0 COMMENT '当前题目索引',
    flagged_ids JSON COMMENT '标记的题目ID列表',
    save_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exam_student (exam_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试进度暂存表';
