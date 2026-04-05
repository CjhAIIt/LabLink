import { mkdirSync, writeFileSync, existsSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'

const baseUrl = process.env.SMOKE_BASE_URL || 'http://localhost:8081'
const dbUrl =
  process.env.DB_URL ||
  'jdbc:mysql://localhost:3306/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true'
const dbUsername = process.env.DB_USERNAME || 'root'
const dbPassword = process.env.DB_PASSWORD || 'cjh041217'
const runId = new Date().toISOString().replace(/\D/g, '').slice(0, 14)
const shortId = runId.slice(-8)

const state = {
  tokens: {},
  labId: null,
  adminId: null,
  studentId: null,
  questionId: null,
  submissionId: null,
  deliveryId: null,
  equipmentId: null,
  borrowId: null,
  exitApplicationId: null
}

const report = {
  runId,
  startedAt: new Date().toISOString(),
  baseUrl,
  rolesCovered: [],
  steps: []
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function buildUrl(path, query) {
  const url = new URL(path, baseUrl)
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value))
      }
    }
  }
  return url
}

async function api(method, path, options = {}) {
  const headers = {
    Accept: 'application/json',
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.token ? { Authorization: `Bearer ${options.token}` } : {})
  }

  const response = await fetch(buildUrl(path, options.query), {
    method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  })

  const text = await response.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch (error) {
    data = { rawText: text }
  }

  return {
    status: response.status,
    data,
    text
  }
}

function compact(value) {
  if (value == null) {
    return value
  }
  const text = JSON.stringify(value)
  if (text.length <= 800) {
    return value
  }
  return { summary: text.slice(0, 800) + '...' }
}

function requireSuccess(result, label) {
  if (result.status !== 200 || !result.data || result.data.code !== 200) {
    const message = result.data?.message || result.data?.msg || result.text || `HTTP ${result.status}`
    throw new Error(`${label} failed: ${message}`)
  }
  return result.data.data
}

function findRecord(pageLike, predicate) {
  const records = pageLike?.records || pageLike?.list || []
  return records.find(predicate)
}

async function step(role, name, action) {
  const startedAt = Date.now()
  try {
    const details = await action()
    report.steps.push({
      role,
      name,
      status: 'passed',
      durationMs: Date.now() - startedAt,
      details: compact(details)
    })
    if (!report.rolesCovered.includes(role)) {
      report.rolesCovered.push(role)
    }
    return details
  } catch (error) {
    report.steps.push({
      role,
      name,
      status: 'failed',
      durationMs: Date.now() - startedAt,
      error: error instanceof Error ? error.message : String(error)
    })
    throw error
  }
}

function findMysqlConnectorJar() {
  const root = join(process.env.USERPROFILE || '', '.m2', 'repository', 'com', 'mysql', 'mysql-connector-j')
  if (!existsSync(root)) {
    return null
  }
  const versions = readdirSync(root).sort().reverse()
  for (const version of versions) {
    const jar = join(root, version, `mysql-connector-j-${version}.jar`)
    if (existsSync(jar)) {
      return jar
    }
  }
  return null
}

function runCleanup() {
  if (!state.labId && !state.adminId && !state.studentId && !state.questionId) {
    return { skipped: true, reason: 'no temporary data created' }
  }

  const connectorJar = findMysqlConnectorJar()
  if (!connectorJar) {
    throw new Error('mysql connector jar not found in local Maven repository')
  }

  const args = [
    '--class-path',
    connectorJar,
    join(process.cwd(), 'scripts', 'jdbc-cleanup.java'),
    '--db-url',
    dbUrl,
    '--db-username',
    dbUsername,
    '--db-password',
    dbPassword
  ]

  if (state.labId) {
    args.push('--lab-id', String(state.labId))
  }
  for (const userId of [state.adminId, state.studentId]) {
    if (userId) {
      args.push('--user-id', String(userId))
    }
  }
  if (state.questionId) {
    args.push('--question-id', String(state.questionId))
  }

  const result = spawnSync('java', args, {
    encoding: 'utf8',
    cwd: process.cwd()
  })

  if (result.status !== 0) {
    throw new Error((result.stderr || result.stdout || 'cleanup failed').trim())
  }

  return {
    skipped: false,
    stdout: (result.stdout || '').trim()
  }
}

async function main() {
  const labName = `Cloud联调实验室-${shortId}`
  const adminUsername = `cadm${shortId}`
  const studentUsername = `cstu${shortId}`
  const adminPassword = 'Test123456'
  const studentPassword = 'Test123456'
  const adminPhone = `139${shortId}`
  const studentPhone = `138${shortId}`
  const studentCode = runId.slice(-10)
  const questionTitle = `云部署填空题-${shortId}`
  const equipmentSerial = `EQ-${runId}`
  let assessmentQuestionList = []

  const superAdminLogin = await step('super_admin', 'login', async () => {
    const result = await api('POST', '/auth/login', {
      body: { username: 'super_admin', password: '123456' }
    })
    const data = requireSuccess(result, 'super admin login')
    state.tokens.superAdmin = data.token
    return { username: data.username, role: data.role, id: data.id }
  })

  await step('super_admin', 'read own profile', async () => {
    const result = await api('GET', '/user/info', { token: state.tokens.superAdmin })
    const data = requireSuccess(result, 'super admin profile')
    return { id: data.id, username: data.username, role: data.role }
  })

  await step('super_admin', 'list admins', async () => {
    const result = await api('GET', '/admin/list', {
      token: state.tokens.superAdmin,
      query: { pageNum: 1, pageSize: 5 }
    })
    const data = requireSuccess(result, 'admin list')
    return { total: data.total, sampleCount: (data.records || []).length }
  })

  await step('super_admin', 'create temporary lab', async () => {
    const result = await api('POST', '/labs', {
      token: state.tokens.superAdmin,
      body: {
        labName,
        labDesc: '用于云部署与角色冒烟测试',
        requireSkill: 'Java, Vue, Docker, CI/CD',
        recruitNum: 3,
        currentNum: 0,
        status: 1,
        foundingDate: '2024-09',
        awards: '校级项目一等奖',
        basicInfo: '临时测试实验室',
        advisors: '测试导师',
        currentAdmins: '待创建'
      }
    })
    requireSuccess(result, 'create lab')

    const listResult = await api('GET', '/labs/list', {
      query: { pageNum: 1, pageSize: 50, labName }
    })
    const listData = requireSuccess(listResult, 'query created lab')
    const lab = findRecord(listData, (item) => item.labName === labName)
    if (!lab) {
      throw new Error('temporary lab not found after creation')
    }
    state.labId = lab.id
    return { labId: state.labId, labName }
  })

  await step('super_admin', 'create temporary lab admin', async () => {
    const result = await api('POST', '/user/admin/add', {
      token: state.tokens.superAdmin,
      body: {
        username: adminUsername,
        password: adminPassword,
        realName: '云端测试管理员',
        email: `${adminUsername}@example.com`,
        phone: adminPhone,
        labId: state.labId,
        canEdit: 1
      }
    })
    requireSuccess(result, 'create admin')

    const adminsResult = await api('GET', '/user/admin/list', {
      token: state.tokens.superAdmin
    })
    const admins = requireSuccess(adminsResult, 'query admin list')
    const admin = (admins || []).find((item) => item.username === adminUsername)
    if (!admin) {
      throw new Error('temporary admin not found after creation')
    }
    state.adminId = admin.id
    return { adminId: state.adminId, username: adminUsername }
  })

  await step('admin', 'login', async () => {
    const result = await api('POST', '/auth/login', {
      body: { username: adminUsername, password: adminPassword }
    })
    const data = requireSuccess(result, 'admin login')
    state.tokens.admin = data.token
    return { username: data.username, role: data.role, labId: data.labId }
  })

  await step('super_admin', 'verify lab-admin binding', async () => {
    const result = await api('GET', '/labs/list-with-admin', { token: state.tokens.superAdmin })
    const data = requireSuccess(result, 'labs with admin')
    const row = (data || []).find((item) => item.lab?.id === state.labId)
    if (!row || row.admin?.username !== adminUsername) {
      throw new Error('lab-admin binding is incorrect')
    }
    return { labId: state.labId, adminUsername: row.admin.username }
  })

  await step('admin', 'open question bank', async () => {
    const result = await api('GET', '/growth-center/admin/question-bank', {
      token: state.tokens.admin,
      query: { pageNum: 1, pageSize: 5 }
    })
    const data = requireSuccess(result, 'admin question bank')
    return { total: data.total, sampleCount: (data.records || []).length }
  })

  await step('admin', 'create shared fill-blank question', async () => {
    const result = await api('POST', '/growth-center/admin/question-bank', {
      token: state.tokens.admin,
      body: {
        questionType: 'fill_blank',
        trackCode: 'common',
        title: questionTitle,
        content: 'Docker Compose historical default file name is ________.yml',
        difficulty: 'easy',
        acceptableAnswers: ['docker-compose'],
        tags: ['cloud', 'docker', 'compose'],
        analysisHint: 'Compose v1 historical file name'
      }
    })
    const data = requireSuccess(result, 'create fill blank question')
    state.questionId = data.id
    return { questionId: state.questionId, title: questionTitle }
  })

  await step('admin', 'configure written exam from shared bank', async () => {
    const now = new Date()
    const startTime = new Date(now.getTime() - 5 * 60 * 1000)
    const endTime = new Date(now.getTime() + 24 * 60 * 60 * 1000)

    const result = await api('POST', '/written-exam/admin/config', {
      token: state.tokens.admin,
      body: {
        recruitmentOpen: true,
        title: `云端联调笔试-${shortId}`,
        description: '仅用于四角色冒烟测试',
        startTime: formatDateTime(startTime),
        endTime: formatDateTime(endTime),
        passScore: 60,
        questions: [
          {
            bankQuestionId: state.questionId,
            score: 100,
            sortOrder: 1
          }
        ]
      }
    })
    requireSuccess(result, 'save written exam config')

    const configResult = await api('GET', '/written-exam/admin/config', { token: state.tokens.admin })
    const config = requireSuccess(configResult, 'read written exam config')
    return {
      examId: config.exam?.id,
      questionCount: (config.questions || []).length,
      labId: config.lab?.id
    }
  })

  await step('student', 'register ordinary user', async () => {
    const result = await api('POST', '/auth/register', {
      body: {
        username: studentCode,
        password: studentPassword,
        realName: '云端测试学生',
        studentId: studentCode,
        college: '计算机学院',
        major: '软件工程',
        grade: '大二',
        phone: studentPhone,
        email: `${studentUsername}@example.com`
      }
    })
    requireSuccess(result, 'student registration')
    return { username: studentCode, studentId: studentCode }
  })

  await step('student', 'login as ordinary user', async () => {
    const result = await api('POST', '/auth/login', {
      body: { username: studentCode, password: studentPassword }
    })
    const data = requireSuccess(result, 'student login')
    state.tokens.student = data.token
    state.studentId = data.id
    return { userId: state.studentId, role: data.role, username: data.username }
  })

  const assessmentQuestions = await step('student', 'load 20-question growth assessment', async () => {
    const result = await api('GET', '/growth-center/assessment/questions', { token: state.tokens.student })
    const data = requireSuccess(result, 'assessment questions')
    if ((data.questions || []).length !== 20) {
      throw new Error(`expected 20 assessment questions, got ${(data.questions || []).length}`)
    }
    assessmentQuestionList = data.questions
    return { versionNo: data.versionNo, questionCount: data.questions.length }
  })

  const submittedAssessment = await step('student', 'submit growth assessment', async () => {
    const answers = assessmentQuestionList.map((question, index) => {
      const option = question.options[index % question.options.length]
      return {
        questionId: question.id,
        optionKey: option.optionKey
      }
    })

    const result = await api('POST', '/growth-center/assessment/submit', {
      token: state.tokens.student,
      body: {
        versionNo: assessmentQuestions.versionNo,
        answers
      }
    })
    const data = requireSuccess(result, 'submit assessment')
    return {
      topTrackCodes: data.topTrackCodes,
      answerCount: data.answerCount,
      summary: data.summary
    }
  })

  const topTrackCode = submittedAssessment.topTrackCodes?.[0]

  await step('student', 'practice shared question from growth center', async () => {
    const listResult = await api('GET', '/growth-center/practice/questions', {
      token: state.tokens.student,
      query: {
        pageNum: 1,
        pageSize: 20,
        trackCode: topTrackCode,
        keyword: questionTitle
      }
    })
    const listData = requireSuccess(listResult, 'practice question list')
    const question = findRecord(listData, (item) => item.id === state.questionId)
    if (!question) {
      throw new Error('temporary practice question not visible to student')
    }

    const submitResult = await api('POST', '/growth-center/practice/submit', {
      token: state.tokens.student,
      body: {
        questionId: state.questionId,
        answer: 'docker-compose'
      }
    })
    const submitData = requireSuccess(submitResult, 'practice submit')
    if (!submitData.correct) {
      throw new Error('practice question answer should have been correct')
    }
    return { questionId: state.questionId, correct: submitData.correct }
  })

  await step('student', 'read local growth question list', async () => {
    const result = await api('GET', '/growth-center/practice/questions', {
      token: state.tokens.student,
      query: { pageNum: 1, pageSize: 3, trackCode: topTrackCode }
    })
    const data = requireSuccess(result, 'growth question list')
    const total = data.total ?? 0
    if (!total) {
      throw new Error('growth question bank returned no questions')
    }
    return { total, pageNum: data.pageNum || 1 }
  })

  const examSnapshot = await step('student', 'enter written exam', async () => {
    const result = await api('GET', `/written-exam/student/exam/${state.labId}`, {
      token: state.tokens.student
    })
    const data = requireSuccess(result, 'student exam')
    if ((data.questions || []).length !== 1) {
      throw new Error(`expected 1 configured exam question, got ${(data.questions || []).length}`)
    }
    return {
      questionId: data.questions?.[0]?.id,
      questionCount: data.questions.length
    }
  })

  await step('student', 'submit written exam', async () => {
    const result = await api('POST', '/written-exam/student/submit', {
      token: state.tokens.student,
      body: {
        labId: state.labId,
        answers: [
          {
            questionId: examSnapshot.questionId,
            answer: 'docker-compose'
          }
        ]
      }
    })
    const data = requireSuccess(result, 'submit written exam')
    state.submissionId = data.submissionId
    return {
      submissionId: state.submissionId,
      score: data.score,
      status: data.status
    }
  })

  await step('admin', 'review written exam as passed', async () => {
    const reviewResult = await api('POST', '/written-exam/admin/review', {
      token: state.tokens.admin,
      body: {
        submissionId: state.submissionId,
        status: 2,
        adminRemark: '自动化通过'
      }
    })
    requireSuccess(reviewResult, 'review written exam')
    return { submissionId: state.submissionId, reviewStatus: 2 }
  })

  await step('student', 'deliver resume after passing exam', async () => {
    const result = await api('POST', '/delivery/deliver', {
      token: state.tokens.student,
      body: {
        labId: state.labId,
        skillTags: 'Docker,Java,Vue',
        studyProgress: '已完成成长测评与笔试',
        attachmentUrl: '/uploads/test_resume.txt'
      }
    })
    requireSuccess(result, 'delivery submit')

    const myResult = await api('GET', '/delivery/my', {
      token: state.tokens.student,
      query: { pageNum: 1, pageSize: 10 }
    })
    const myData = requireSuccess(myResult, 'my deliveries')
    const delivery = findRecord(myData, (item) => item.labId === state.labId)
    if (!delivery) {
      throw new Error('new delivery not found in student list')
    }
    state.deliveryId = delivery.id
    return { deliveryId: state.deliveryId, auditStatus: delivery.auditStatus }
  })

  await step('admin', 'approve and admit delivery', async () => {
    const auditResult = await api('POST', `/delivery/audit/${state.deliveryId}`, {
      token: state.tokens.admin,
      query: { auditStatus: 1, auditRemark: '通过初筛' }
    })
    requireSuccess(auditResult, 'audit delivery')

    const admitResult = await api('POST', `/delivery/admit/${state.deliveryId}`, {
      token: state.tokens.admin
    })
    requireSuccess(admitResult, 'admit delivery')
    return { deliveryId: state.deliveryId, admitted: true }
  })

  await step('student', 'confirm offer and become lab member', async () => {
    const confirmResult = await api('POST', `/delivery/confirm/${state.deliveryId}`, {
      token: state.tokens.student
    })
    requireSuccess(confirmResult, 'confirm offer')

    const profileResult = await api('GET', '/user/info', { token: state.tokens.student })
    const profile = requireSuccess(profileResult, 'member profile')
    if (profile.labId !== state.labId) {
      throw new Error('student did not become a formal lab member')
    }
    return { userId: profile.id, labId: profile.labId }
  })

  await step('formal_member', 'use lab-space and attendance features', async () => {
    const overviewResult = await api('GET', '/lab-space/overview', { token: state.tokens.student })
    const overview = requireSuccess(overviewResult, 'lab overview')

    const today = formatDateTime(new Date()).slice(0, 10)
    const attendanceConfirmResult = await api('POST', '/lab-space/attendance/confirm', {
      token: state.tokens.admin,
      body: {
        userId: state.studentId,
        status: 1,
        attendanceDate: today,
        reason: '云端联调签到'
      }
    })
    requireSuccess(attendanceConfirmResult, 'attendance confirm')

    const attendanceResult = await api('GET', '/lab-space/attendance/my', {
      token: state.tokens.student,
      query: { pageNum: 1, pageSize: 10 }
    })
    const attendance = requireSuccess(attendanceResult, 'attendance history')
    return {
      memberCount: overview.memberCount,
      attendanceTotal: attendance.total
    }
  })

  await step('admin', 'manage equipment for the lab', async () => {
    const addResult = await api('POST', '/equipment/add', {
      token: state.tokens.admin,
      body: {
        name: '云联调测试笔记本',
        type: 'Laptop',
        serialNumber: equipmentSerial,
        imageUrl: '',
        description: '仅用于自动化验证',
        status: 0
      }
    })
    requireSuccess(addResult, 'add equipment')

    const listResult = await api('GET', '/equipment/list', {
      token: state.tokens.admin,
      query: { pageNum: 1, pageSize: 20, name: '云联调测试笔记本' }
    })
    const listData = requireSuccess(listResult, 'equipment list')
    const equipment = findRecord(listData, (item) => item.serialNumber === equipmentSerial)
    if (!equipment) {
      throw new Error('new equipment not found')
    }
    state.equipmentId = equipment.id
    return { equipmentId: state.equipmentId, status: equipment.status }
  })

  await step('formal_member', 'borrow and return equipment', async () => {
    const borrowResult = await api('POST', '/equipment/borrow', {
      token: state.tokens.student,
      body: {
        equipmentId: state.equipmentId,
        reason: '容器化联调测试'
      }
    })
    requireSuccess(borrowResult, 'borrow equipment')

    const myBorrowResult = await api('GET', '/equipment/borrow/my', {
      token: state.tokens.student,
      query: { pageNum: 1, pageSize: 10 }
    })
    const myBorrowData = requireSuccess(myBorrowResult, 'my borrow list')
    const borrowRecord = findRecord(myBorrowData, (item) => item.equipmentId === state.equipmentId)
    if (!borrowRecord) {
      throw new Error('borrow record not found')
    }
    state.borrowId = borrowRecord.id

    const auditResult = await api('POST', '/equipment/borrow/audit', {
      token: state.tokens.admin,
      body: { id: state.borrowId, status: 1 }
    })
    requireSuccess(auditResult, 'audit borrow')

    const returnResult = await api('POST', '/equipment/borrow/return', {
      token: state.tokens.admin,
      body: { id: state.borrowId }
    })
    requireSuccess(returnResult, 'return equipment')

    return { borrowId: state.borrowId, equipmentId: state.equipmentId }
  })

  await step('formal_member', 'submit leave request and exit lab', async () => {
    const submitResult = await api('POST', '/lab-space/exit-application', {
      token: state.tokens.student,
      body: { reason: '测试结束，清理成员关系' }
    })
    requireSuccess(submitResult, 'submit exit application')

    const myResult = await api('GET', '/lab-space/exit-application/my', {
      token: state.tokens.student,
      query: { pageNum: 1, pageSize: 10 }
    })
    const myData = requireSuccess(myResult, 'my exit applications')
    const exitApplication = findRecord(myData, (item) => item.labId === state.labId)
    if (!exitApplication) {
      throw new Error('exit application not found')
    }
    state.exitApplicationId = exitApplication.id

    const auditResult = await api('POST', '/lab-space/exit-application/audit', {
      token: state.tokens.admin,
      body: {
        id: state.exitApplicationId,
        status: 1,
        auditRemark: '测试通过后退出'
      }
    })
    requireSuccess(auditResult, 'audit exit application')

    const profileResult = await api('GET', '/user/info', { token: state.tokens.student })
    const profile = requireSuccess(profileResult, 'profile after exit')
    if (profile.labId !== null) {
      throw new Error('student should have been removed from lab after exit approval')
    }
    return { exitApplicationId: state.exitApplicationId, finalLabId: profile.labId }
  })

  report.summary = {
    totalSteps: report.steps.length,
    passedSteps: report.steps.filter((item) => item.status === 'passed').length,
    failedSteps: report.steps.filter((item) => item.status === 'failed').length,
    superAdminId: superAdminLogin.id,
    tempLabId: state.labId,
    tempAdminId: state.adminId,
    tempStudentId: state.studentId,
    tempQuestionId: state.questionId
  }
}

async function run() {
  mkdirSync(join(process.cwd(), 'logs'), { recursive: true })

  try {
    await main()
  } finally {
    try {
      report.cleanup = runCleanup()
    } catch (error) {
      report.cleanup = {
        skipped: false,
        error: error instanceof Error ? error.message : String(error)
      }
    }

    report.finishedAt = new Date().toISOString()
    writeFileSync(
      join(process.cwd(), 'logs', 'role-smoke-report.json'),
      JSON.stringify(report, null, 2)
    )
  }
}

run().catch((error) => {
  report.error = error instanceof Error ? error.message : String(error)
  process.exitCode = 1
})
