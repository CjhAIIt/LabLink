export const defaultTrackId = 'backend'

export const gradPathTracks = [
  {
    id: 'backend',
    name: '后端开发与服务架构',
    shortName: 'Backend',
    practiceKeyword: 'Java 基础',
    interviewPosition: '后端开发工程师',
    preferredLanguage: 'java',
    questionBankLabel: '接口设计、数据库与服务治理'
  },
  {
    id: 'frontend',
    name: '前端工程与用户体验',
    shortName: 'Frontend',
    practiceKeyword: 'Web 逻辑',
    interviewPosition: '前端开发工程师',
    preferredLanguage: 'python',
    questionBankLabel: '交互实现、状态管理与性能优化'
  },
  {
    id: 'ai',
    name: '人工智能与模型应用',
    shortName: 'AI',
    practiceKeyword: '动态规划',
    interviewPosition: '算法工程师',
    preferredLanguage: 'python',
    questionBankLabel: '模型推理、数据处理与算法分析'
  },
  {
    id: 'embedded',
    name: '嵌入式与物联网开发',
    shortName: 'Embedded',
    practiceKeyword: '数据结构',
    interviewPosition: '嵌入式工程师',
    preferredLanguage: 'c',
    questionBankLabel: '底层控制、驱动调试与设备通信'
  },
  {
    id: 'security',
    name: '网络安全与攻防实践',
    shortName: 'Security',
    practiceKeyword: '图搜索',
    interviewPosition: '安全工程师',
    preferredLanguage: 'python',
    questionBankLabel: '风险分析、权限校验与日志审计'
  },
  {
    id: 'bigdata',
    name: '大数据平台与数据工程',
    shortName: 'Big Data',
    practiceKeyword: '队列',
    interviewPosition: '大数据开发工程师',
    preferredLanguage: 'python',
    questionBankLabel: '批流处理、数据链路与资源调度'
  },
  {
    id: 'analysis',
    name: '数据分析与商业洞察',
    shortName: 'Analytics',
    practiceKeyword: 'Python',
    interviewPosition: '数据分析师',
    preferredLanguage: 'python',
    questionBankLabel: '指标分析、实验设计与数据清洗'
  },
  {
    id: 'product',
    name: '产品策划与需求设计',
    shortName: 'Product',
    practiceKeyword: '逻辑表达',
    interviewPosition: '产品经理',
    preferredLanguage: 'python',
    questionBankLabel: '需求拆解、优先级评估与流程设计'
  },
  {
    id: 'design',
    name: 'UI/UX 设计与体验优化',
    shortName: 'Design',
    practiceKeyword: '交互设计',
    interviewPosition: '交互设计师',
    preferredLanguage: 'python',
    questionBankLabel: '版式规范、体验评估与设计系统'
  },
  {
    id: 'qa',
    name: '测试开发与质量保障',
    shortName: 'QA',
    practiceKeyword: '自动化测试',
    interviewPosition: '测试开发工程师',
    preferredLanguage: 'java',
    questionBankLabel: '边界覆盖、自动化脚本与质量控制'
  },
  {
    id: 'cloud',
    name: '云平台与运维自动化',
    shortName: 'Cloud',
    practiceKeyword: '容器调度',
    interviewPosition: '云平台工程师',
    preferredLanguage: 'python',
    questionBankLabel: '部署编排、弹性伸缩与可观测性'
  },
  {
    id: 'game',
    name: '游戏开发与实时交互',
    shortName: 'Game',
    practiceKeyword: '实时交互',
    interviewPosition: '游戏客户端工程师',
    preferredLanguage: 'cpp',
    questionBankLabel: '帧循环、状态同步与玩法逻辑'
  }
]

export function getTrackById(trackId) {
  return gradPathTracks.find((item) => item.id === trackId) || gradPathTracks[0]
}
