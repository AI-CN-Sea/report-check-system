<template>
  <el-container class="layout">
    <el-aside width="240px" class="sidebar">
      <div class="brand">实验报告查重系统</div>
      <el-menu :default-active="activeSection" class="menu" @select="switchSection">
        <el-menu-item v-for="item in menuItems" :key="item.key" :index="item.key">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div>{{ pageTitle }}</div>
        <div class="header-actions">
          <span>{{ currentUser.realName || currentUser.username }}</span>
          <el-tag>{{ roleName }}</el-tag>
          <el-tag :type="healthType">{{ healthText }}</el-tag>
          <el-button size="small" @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <section v-if="activeSection === 'dashboard'">
          <div class="metrics">
            <div class="metric">
              <span>课程数量</span>
              <strong>{{ stats.courseCount }}</strong>
            </div>
            <div class="metric">
              <span>实验任务</span>
              <strong>{{ stats.taskCount }}</strong>
            </div>
            <div class="metric">
              <span>{{ isStudent ? '我的报告' : '报告总数' }}</span>
              <strong>{{ stats.reportCount }}</strong>
            </div>
            <div class="metric">
              <span>{{ isStudent ? '可提交任务' : '查重任务' }}</span>
              <strong>{{ isStudent ? tasks.length : stats.checkTaskCount }}</strong>
            </div>
          </div>

          <section class="panel">
            <h2>{{ isStudent ? '我的实验任务' : '最近实验任务' }}</h2>
            <el-table :data="tasks" stripe>
              <el-table-column prop="title" label="任务名称" min-width="180" />
              <el-table-column prop="description" label="任务说明" min-width="220" show-overflow-tooltip />
              <el-table-column prop="deadline" label="截止时间" min-width="180" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag>{{ statusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </section>

        <section v-if="activeSection === 'users' && isAdmin" class="panel">
          <h2>用户管理</h2>
          <el-form :model="userForm" class="tool-form" label-width="90px">
            <el-form-item label="用户名">
              <el-input v-model="userForm.username" placeholder="输入用户名" />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="userForm.realName" placeholder="输入真实姓名" />
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="userForm.roleId" placeholder="选择角色">
                <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="初始密码">
              <el-input v-model="userForm.password" :disabled="!!editingUserId" placeholder="新增用户时必填，至少 6 位" show-password />
            </el-form-item>
            <el-form-item label="学号">
              <el-input v-model="userForm.studentNo" placeholder="学生填写" />
            </el-form-item>
            <el-form-item label="工号">
              <el-input v-model="userForm.teacherNo" placeholder="教师填写" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="userForm.email" placeholder="输入邮箱" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="userForm.status">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveUser">{{ editingUserId ? '保存修改' : '新增用户' }}</el-button>
              <el-button @click="resetUserForm">清空</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="users" stripe>
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="roleName" label="角色" width="100" />
            <el-table-column prop="studentNo" label="学号" min-width="130" />
            <el-table-column prop="teacherNo" label="工号" min-width="130" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button size="small" @click="editUser(row)">编辑</el-button>
                <el-button size="small" @click="resetPassword(row.id)">重置密码</el-button>
                <el-button size="small" type="danger" @click="removeUser(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeSection === 'courses' && isAdmin" class="panel">
          <h2>课程管理</h2>
          <el-form :model="courseForm" class="tool-form" label-width="90px">
            <el-form-item label="课程名称">
              <el-input v-model="courseForm.courseName" placeholder="输入课程名称" />
            </el-form-item>
            <el-form-item label="课程编码">
              <el-input v-model="courseForm.courseCode" placeholder="输入课程编码" />
            </el-form-item>
            <el-form-item label="任课教师">
              <el-select v-model="courseForm.teacherId" placeholder="选择教师">
                <el-option v-for="teacher in teachers" :key="teacher.id" :label="teacher.realName" :value="teacher.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="课程说明">
              <el-input v-model="courseForm.description" placeholder="输入课程说明" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveCourse">{{ editingCourseId ? '保存修改' : '新增课程' }}</el-button>
              <el-button @click="resetCourseForm">清空</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="courses" stripe>
            <el-table-column prop="courseName" label="课程名称" min-width="180" />
            <el-table-column prop="courseCode" label="课程编码" min-width="140" />
            <el-table-column prop="teacherId" label="教师" width="120">
              <template #default="{ row }">{{ teacherName(row.teacherId) }}</template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="editCourse(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeCourse(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeSection === 'classes' && isAdmin" class="panel">
          <h2>班级管理</h2>
          <el-form :model="classForm" class="tool-form" label-width="90px">
            <el-form-item label="班级名称">
              <el-input v-model="classForm.className" placeholder="输入班级名称" />
            </el-form-item>
            <el-form-item label="年级">
              <el-input v-model="classForm.grade" placeholder="例如：2023级" />
            </el-form-item>
            <el-form-item label="专业" class="full-row">
              <el-input v-model="classForm.major" placeholder="输入专业名称" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveClassInfo">{{ editingClassId ? '保存修改' : '新增班级' }}</el-button>
              <el-button @click="resetClassForm">清空</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="classes" stripe>
            <el-table-column prop="className" label="班级名称" min-width="180" />
            <el-table-column prop="grade" label="年级" width="120" />
            <el-table-column prop="major" label="专业" min-width="180" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="editClassInfo(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeClassInfo(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="sub-panel">
            <h3>班级学生分配</h3>
            <div class="toolbar">
              <el-select v-model="studentClassForm.classId" placeholder="选择班级" @change="loadStudentClasses">
                <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
              </el-select>
              <el-select v-model="studentClassForm.studentId" placeholder="选择学生">
                <el-option v-for="student in students" :key="student.id" :label="`${student.realName} (${student.studentNo || student.username})`" :value="student.id" />
              </el-select>
              <el-button type="primary" @click="addStudentToClass">添加学生</el-button>
            </div>
            <el-table :data="studentClasses" stripe>
              <el-table-column prop="className" label="班级" min-width="180" />
              <el-table-column prop="studentName" label="学生" width="120" />
              <el-table-column prop="studentNo" label="学号" width="140" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button size="small" type="danger" @click="removeStudentClass(row.id)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>

        <section v-if="activeSection === 'tasks'" class="panel">
          <h2>实验任务</h2>
          <el-form v-if="isTeacher" :model="taskForm" class="tool-form" label-width="90px">
            <el-form-item label="课程">
              <el-select v-model="taskForm.courseId" placeholder="选择课程">
                <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="班级">
              <el-select v-model="taskForm.classId" placeholder="选择班级">
                <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="任务标题">
              <el-input v-model="taskForm.title" placeholder="输入实验任务标题" />
            </el-form-item>
            <el-form-item label="截止时间">
              <el-date-picker
                v-model="taskForm.deadline"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择截止时间"
              />
            </el-form-item>
            <el-form-item label="任务说明" class="full-row">
              <el-input v-model="taskForm.description" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveTask">{{ editingTaskId ? '保存修改' : '新增任务' }}</el-button>
              <el-button @click="resetTaskForm">清空</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="tasks" stripe>
            <el-table-column prop="title" label="任务名称" min-width="180" />
            <el-table-column prop="deadline" label="截止时间" min-width="180" />
            <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column v-if="isTeacher" label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="editTask(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeTask(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeSection === 'reports' && canUseReports" class="panel">
          <h2>{{ isStudent ? '我的报告提交' : '报告上传与解析' }}</h2>
          <el-form :model="uploadForm" class="tool-form" label-width="90px">
            <el-form-item label="实验任务">
              <el-select v-model="uploadForm.taskId" placeholder="选择任务" @change="loadReports">
                <el-option v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="isTeacher" label="学生">
              <el-select v-model="uploadForm.studentId" placeholder="选择学生">
                <el-option v-for="student in uploadStudents" :key="student.id" :label="student.realName" :value="student.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="报告文件">
              <input ref="fileInputRef" type="file" accept=".txt,.docx,.pdf" @change="handleFileChange" />
              <div class="upload-tip">
                支持 txt、docx、pdf，单个文件不超过 20MB
                <span v-if="selectedFile">已选择：{{ selectedFile.name }}（{{ formatSize(selectedFile.size) }}）</span>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="uploading" @click="submitReport">
                {{ isStudent ? '提交我的报告' : '上传并解析' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div v-if="isTeacher" class="batch-upload">
            <strong>批量上传</strong>
            <span>文件名需包含学生姓名、用户名或学号，例如：李明-实验一.txt、20260002-report.docx</span>
            <input ref="batchFileInputRef" type="file" multiple accept=".txt,.docx,.pdf" @change="handleBatchFileChange" />
            <el-button :disabled="!batchFiles.length" :loading="batchUploading" @click="submitBatchReports">
              批量上传 {{ batchFiles.length ? `(${batchFiles.length})` : '' }}
            </el-button>
          </div>

          <el-table :data="reports" stripe>
            <el-table-column prop="taskTitle" label="任务" min-width="180" />
            <el-table-column v-if="isTeacher" prop="studentName" label="学生" width="110" />
            <el-table-column prop="originalName" label="文件名" min-width="180" />
            <el-table-column prop="wordCount" label="字数" width="90" />
            <el-table-column prop="parseMessage" label="解析状态" min-width="140" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" @click="openReportDetail(row.id)">预览</el-button>
                <el-button v-if="isTeacher" size="small" type="danger" @click="removeReport(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeSection === 'results' && canUseResults" class="panel">
          <h2>查重任务与结果</h2>
          <div class="toolbar">
            <el-select v-if="isTeacher" v-model="checkForm.experimentTaskId" placeholder="选择实验任务">
              <el-option v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
            </el-select>
            <el-tag v-if="isTeacher" type="info">解析成功报告：{{ selectedTaskParsedCount }} 份</el-tag>
            <el-button v-if="isTeacher" type="primary" :loading="checking" @click="startCheck">发起查重</el-button>
            <el-button @click="downloadResults">导出结果</el-button>
            <el-select v-model="selectedCheckTaskId" placeholder="筛选查重任务" clearable @change="loadResults">
              <el-option v-for="task in checkTasks" :key="task.id" :label="`${task.id} - ${task.experimentTaskTitle}`" :value="task.id" />
            </el-select>
            <el-select v-model="riskFilter" placeholder="风险等级" clearable>
              <el-option label="高风险" value="高风险" />
              <el-option label="中风险" value="中风险" />
              <el-option label="低风险" value="低风险" />
              <el-option label="正常" value="正常" />
            </el-select>
          </div>

          <div class="sub-panel">
            <h3>查重任务列表</h3>
            <el-table :data="checkTasks" stripe empty-text="暂无查重任务，请先选择实验任务并发起查重">
              <el-table-column prop="id" label="任务ID" width="90" />
              <el-table-column prop="experimentTaskTitle" label="实验任务" min-width="200" show-overflow-tooltip />
              <el-table-column prop="parsedReportCount" label="解析成功报告" width="120" />
              <el-table-column prop="reportCount" label="参与报告" width="100" />
              <el-table-column prop="resultCount" label="结果数" width="90" />
              <el-table-column prop="startTime" label="开始时间" min-width="170" />
              <el-table-column prop="endTime" label="结束时间" min-width="170" />
              <el-table-column prop="status" label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 2 ? 'success' : row.status === 3 ? 'danger' : 'warning'">
                    {{ checkTaskStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110">
                <template #default="{ row }">
                  <el-button size="small" @click="selectCheckTask(row.id)">查看结果</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <el-empty
            v-if="!filteredResults.length"
            description="当前暂无查重结果，请先上传报告并发起查重；也可以执行 database/demo_seed.sql 预置演示数据"
          />
          <el-table v-else :data="filteredResults" stripe empty-text="当前暂无查重结果，请先上传报告并发起查重">
            <el-table-column prop="sourceStudentName" label="报告 A 学生" min-width="120" />
            <el-table-column prop="sourceReportName" label="报告 A 文件" min-width="180" show-overflow-tooltip />
            <el-table-column prop="targetStudentName" label="报告 B 学生" min-width="120" />
            <el-table-column prop="targetReportName" label="报告 B 文件" min-width="180" show-overflow-tooltip />
            <el-table-column prop="cosineSimilarity" label="余弦相似度" width="130" />
            <el-table-column prop="simhashSimilarity" label="SimHash" width="110" />
            <el-table-column prop="finalSimilarity" label="综合相似度" width="130" />
            <el-table-column prop="riskLevel" label="风险等级" width="110">
              <template #default="{ row }">
                <el-tag :type="riskTagType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="openResultDetail(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeSection === 'stats' && canUseStats" class="panel">
          <h2>统计分析</h2>
          <div class="charts">
            <div ref="riskChartRef" class="chart"></div>
            <div ref="summaryChartRef" class="chart"></div>
            <div ref="similarityChartRef" class="chart"></div>
            <div ref="trendChartRef" class="chart"></div>
          </div>
        </section>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="detailVisible" title="相似句子详情" width="760px">
    <div v-if="resultDetail.result" class="detail-summary">
      <strong>{{ resultDetail.result.sourceStudentName }}</strong>
      <span>{{ resultDetail.result.sourceReportName }}</span>
      <span>与</span>
      <strong>{{ resultDetail.result.targetStudentName }}</strong>
      <span>{{ resultDetail.result.targetReportName }}</span>
      <span>综合相似度：{{ resultDetail.result.finalSimilarity }}</span>
      <el-tag :type="riskTagType(resultDetail.result.riskLevel)">{{ resultDetail.result.riskLevel }}</el-tag>
    </div>
    <el-alert
      v-if="resultDetail.detailMessage"
      :title="resultDetail.detailMessage"
      type="warning"
      show-icon
      :closable="false"
      class="dialog-alert"
    />
    <el-table
      v-loading="detailLoading"
      :data="resultDetail.similarSentences || []"
      empty-text="暂无相似句子，请确认报告文本足够完整并重新发起查重"
      stripe
    >
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="sourceSentence" label="报告 A 句子" min-width="240" />
      <el-table-column prop="targetSentence" label="报告 B 句子" min-width="240" />
      <el-table-column prop="sentenceSimilarity" label="相似度" width="100" />
    </el-table>
  </el-dialog>

  <el-dialog v-model="reportDetailVisible" title="报告解析预览" width="760px">
    <div v-if="reportDetail" class="detail-summary">
      <strong>{{ reportDetail.studentName }}</strong>
      <span>{{ reportDetail.originalName }}</span>
      <el-tag :type="reportDetail.parseStatus === 1 ? 'success' : 'danger'">{{ reportDetail.parseMessage }}</el-tag>
    </div>
    <pre class="text-preview">{{ reportDetail?.parsedText || '暂无可预览文本' }}</pre>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { getHealth } from '../api/health'
import {
  addStudentClass,
  batchUploadReports,
  createClassInfo,
  createCheckTask,
  createCourse,
  createExperimentTask,
  createUser,
  deleteClassInfo,
  deleteCourse,
  deleteExperimentTask,
  deleteReport,
  deleteStudentClass,
  deleteUser,
  exportCheckResults,
  getCheckResultDetail,
  getCheckResults,
  getCheckTasks,
  getClasses,
  getCourses,
  getDashboardStats,
  getExperimentTasks,
  getReportDetail,
  getReports,
  getRoles,
  getStudentClasses,
  getUsers,
  resetUserPassword,
  updateClassInfo,
  updateCourse,
  updateExperimentTask,
  updateUser,
  uploadReport
} from '../api/dashboard'

const router = useRouter()
const currentUser = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const activeSection = ref('dashboard')
const healthStatus = ref('checking')
const stats = ref({ courseCount: 0, taskCount: 0, userCount: 0, reportCount: 0, checkTaskCount: 0 })
const courses = ref([])
const classes = ref([])
const users = ref([])
const roles = ref([])
const studentClasses = ref([])
const tasks = ref([])
const reports = ref([])
const checkTasks = ref([])
const results = ref([])
const selectedCheckTaskId = ref(null)
const riskFilter = ref('')
const selectedFile = ref(null)
const fileInputRef = ref(null)
const batchFileInputRef = ref(null)
const batchFiles = ref([])
const uploading = ref(false)
const batchUploading = ref(false)
const checking = ref(false)
const editingTaskId = ref(null)
const editingCourseId = ref(null)
const editingClassId = ref(null)
const editingUserId = ref(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const resultDetail = ref({})
const reportDetailVisible = ref(false)
const reportDetail = ref(null)
const riskChartRef = ref(null)
const summaryChartRef = ref(null)
const similarityChartRef = ref(null)
const trendChartRef = ref(null)

const taskForm = reactive({ courseId: null, classId: null, title: '', description: '', deadline: '', status: 1, createdBy: 2 })
const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  roleId: null,
  studentNo: '',
  teacherNo: '',
  phone: '',
  email: '',
  status: 1
})
const courseForm = reactive({ courseName: '', courseCode: '', teacherId: null, description: '' })
const classForm = reactive({ className: '', grade: '', major: '' })
const studentClassForm = reactive({ classId: null, studentId: null })
const uploadForm = reactive({ taskId: null, studentId: null })
const checkForm = reactive({ experimentTaskId: null, createdBy: 2 })

const roleCode = computed(() => currentUser.value.roleCode)
const isAdmin = computed(() => roleCode.value === 'ADMIN')
const isTeacher = computed(() => roleCode.value === 'TEACHER')
const isStudent = computed(() => roleCode.value === 'STUDENT')
const canUseReports = computed(() => isTeacher.value || isStudent.value)
const canUseStats = computed(() => isAdmin.value || isTeacher.value)
const canUseResults = computed(() => isAdmin.value || isTeacher.value)
const students = computed(() => users.value.filter(user => user.roleCode === 'STUDENT'))
const teachers = computed(() => users.value.filter(user => user.roleCode === 'TEACHER'))
const uploadStudents = computed(() => {
  if (!isTeacher.value || !uploadForm.taskId) {
    return students.value
  }
  const task = tasks.value.find(item => item.id === uploadForm.taskId)
  if (!task?.classId) {
    return students.value
  }
  const studentIds = new Set(studentClasses.value
    .filter(item => item.classId === task.classId)
    .map(item => item.studentId))
  return students.value.filter(student => studentIds.has(student.id))
})
const roleName = computed(() => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[roleCode.value] || '未知角色'))
const filteredResults = computed(() => riskFilter.value
  ? results.value.filter(item => item.riskLevel === riskFilter.value)
  : results.value)
const selectedTaskParsedCount = computed(() => {
  const taskId = checkForm.experimentTaskId
  return reports.value.filter(item => item.taskId === taskId && item.parseStatus === 1).length
})

const menuItems = computed(() => {
  if (isAdmin.value) {
    return [
      { key: 'dashboard', label: '系统看板' },
      { key: 'users', label: '用户清单' },
      { key: 'courses', label: '课程管理' },
      { key: 'classes', label: '班级管理' },
      { key: 'tasks', label: '任务查看' },
      { key: 'results', label: '查重结果' },
      { key: 'stats', label: '统计分析' }
    ]
  }
  if (isTeacher.value) {
    return [
      { key: 'dashboard', label: '首页看板' },
      { key: 'tasks', label: '实验任务' },
      { key: 'reports', label: '报告上传' },
      { key: 'results', label: '查重结果' },
      { key: 'stats', label: '统计分析' }
    ]
  }
  return [
    { key: 'dashboard', label: '我的首页' },
    { key: 'tasks', label: '实验任务' },
    { key: 'reports', label: '我的报告' }
  ]
})

const pageTitle = computed(() => menuItems.value.find(item => item.key === activeSection.value)?.label || '首页')
const healthText = computed(() => healthStatus.value === 'up' ? '后端在线' : healthStatus.value === 'down' ? '后端未连接' : '检测中')
const healthType = computed(() => healthStatus.value === 'up' ? 'success' : healthStatus.value === 'down' ? 'danger' : 'info')

watch(menuItems, () => {
  if (!menuItems.value.some(item => item.key === activeSection.value)) {
    activeSection.value = menuItems.value[0].key
  }
}, { immediate: true })

watch(activeSection, () => {
  if (activeSection.value === 'stats') renderCharts()
})

watch(() => uploadForm.taskId, () => {
  if (isTeacher.value) {
    uploadForm.studentId = uploadStudents.value[0]?.id || null
  }
})

onMounted(async () => {
  await checkHealth()
  await loadAll()
})

function switchSection(section) {
  activeSection.value = section
}

async function checkHealth() {
  try {
    const result = await getHealth()
    healthStatus.value = result?.data?.status === 'UP' ? 'up' : 'down'
  } catch {
    healthStatus.value = 'down'
  }
}

async function loadAll() {
  const baseRequests = [getCourses(), getClasses(), getExperimentTasks()]
  const [courseResult, classResult, taskResult] = await Promise.all(baseRequests)
  courses.value = courseResult.data
  classes.value = classResult.data
  tasks.value = taskResult.data

  if (isAdmin.value || isTeacher.value) {
    const [statsResult, userResult, roleResult] = await Promise.all([getDashboardStats(), getUsers(), isAdmin.value ? getRoles() : Promise.resolve({ data: [] })])
    stats.value = statsResult.data
    users.value = userResult.data
    roles.value = roleResult.data
  }

  if (canUseReports.value || isAdmin.value) {
    const reportResult = await getReports()
    reports.value = reportResult.data
    if (isStudent.value) {
      stats.value = {
        ...stats.value,
        courseCount: courses.value.length,
        taskCount: tasks.value.length,
        reportCount: reports.value.length,
        checkTaskCount: 0
      }
    }
  }

  if (canUseResults.value) {
    const [checkTaskResult, resultResult] = await Promise.all([getCheckTasks(), getCheckResults()])
    checkTasks.value = checkTaskResult.data
    results.value = resultResult.data
  }

  setDefaults()
  if (isAdmin.value || isTeacher.value) {
    await loadStudentClasses()
    if (isTeacher.value) {
      uploadForm.studentId = uploadStudents.value[0]?.id || null
    }
  }
  renderCharts()
}

function setDefaults() {
  taskForm.courseId ||= courses.value[0]?.id || null
  taskForm.classId ||= classes.value[0]?.id || null
  userForm.roleId ||= roles.value[0]?.id || null
  courseForm.teacherId ||= teachers.value[0]?.id || null
  studentClassForm.classId ||= classes.value[0]?.id || null
  studentClassForm.studentId ||= students.value[0]?.id || null
  uploadForm.taskId ||= tasks.value[0]?.id || null
  uploadForm.studentId = isStudent.value ? currentUser.value.userId : (uploadForm.studentId || students.value[0]?.id || null)
  checkForm.experimentTaskId ||= tasks.value[0]?.id || null
}

async function saveUser() {
  if (!isAdmin.value) return
  if (!userForm.username || !userForm.realName || !userForm.roleId) {
    ElMessage.warning('请填写用户名、姓名和角色')
    return
  }
  if (!editingUserId.value && (!userForm.password || userForm.password.length < 6)) {
    ElMessage.warning('新增用户请设置至少 6 位初始密码')
    return
  }
  if (editingUserId.value) {
    await updateUser(editingUserId.value, userForm)
    ElMessage.success('用户已更新')
  } else {
    await createUser(userForm)
    ElMessage.success('用户已创建')
  }
  resetUserForm()
  await loadAll()
}

function editUser(row) {
  editingUserId.value = row.id
  const role = roles.value.find(item => item.roleCode === row.roleCode)
  Object.assign(userForm, {
    username: row.username,
    password: '',
    realName: row.realName,
    roleId: role?.id || null,
    studentNo: row.studentNo,
    teacherNo: row.teacherNo,
    phone: row.phone,
    email: row.email,
    status: row.status
  })
}

function resetUserForm() {
  editingUserId.value = null
  Object.assign(userForm, {
    username: '',
    password: '',
    realName: '',
    roleId: roles.value[0]?.id || null,
    studentNo: '',
    teacherNo: '',
    phone: '',
    email: '',
    status: 1
  })
}

async function resetPassword(id) {
  const { value } = await ElMessageBox.prompt('请输入新的登录密码，至少 6 位。', '重置密码', {
    inputType: 'password',
    inputPlaceholder: '新密码',
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码至少 6 位'
  })
  await resetUserPassword(id, value)
  ElMessage.success('密码已重置')
}

async function removeUser(id) {
  await ElMessageBox.confirm('确认删除该用户？有关联数据时数据库会阻止删除。', '删除确认', { type: 'warning' })
  await deleteUser(id)
  ElMessage.success('用户已删除')
  await loadAll()
}

async function saveCourse() {
  if (!isAdmin.value) return
  if (!courseForm.courseName || !courseForm.courseCode || !courseForm.teacherId) {
    ElMessage.warning('请填写课程名称、课程编码和任课教师')
    return
  }
  if (editingCourseId.value) {
    await updateCourse(editingCourseId.value, courseForm)
    ElMessage.success('课程已更新')
  } else {
    await createCourse(courseForm)
    ElMessage.success('课程已创建')
  }
  resetCourseForm()
  await loadAll()
}

function editCourse(row) {
  editingCourseId.value = row.id
  Object.assign(courseForm, {
    courseName: row.courseName,
    courseCode: row.courseCode,
    teacherId: row.teacherId,
    description: row.description
  })
}

function resetCourseForm() {
  editingCourseId.value = null
  Object.assign(courseForm, { courseName: '', courseCode: '', teacherId: teachers.value[0]?.id || null, description: '' })
}

async function removeCourse(id) {
  await ElMessageBox.confirm('确认删除该课程？若已关联实验任务，数据库会阻止删除。', '删除确认', { type: 'warning' })
  await deleteCourse(id)
  ElMessage.success('课程已删除')
  await loadAll()
}

async function saveClassInfo() {
  if (!isAdmin.value) return
  if (!classForm.className) {
    ElMessage.warning('请填写班级名称')
    return
  }
  if (editingClassId.value) {
    await updateClassInfo(editingClassId.value, classForm)
    ElMessage.success('班级已更新')
  } else {
    await createClassInfo(classForm)
    ElMessage.success('班级已创建')
  }
  resetClassForm()
  await loadAll()
}

function editClassInfo(row) {
  editingClassId.value = row.id
  Object.assign(classForm, {
    className: row.className,
    grade: row.grade,
    major: row.major
  })
}

function resetClassForm() {
  editingClassId.value = null
  Object.assign(classForm, { className: '', grade: '', major: '' })
}

async function removeClassInfo(id) {
  await ElMessageBox.confirm('确认删除该班级？若已关联实验任务，数据库会阻止删除。', '删除确认', { type: 'warning' })
  await deleteClassInfo(id)
  ElMessage.success('班级已删除')
  await loadAll()
}

async function loadStudentClasses() {
  if (!isAdmin.value && !isTeacher.value) return
  const result = await getStudentClasses(isAdmin.value ? studentClassForm.classId : null)
  studentClasses.value = result.data
}

async function addStudentToClass() {
  if (!studentClassForm.classId || !studentClassForm.studentId) {
    ElMessage.warning('请选择班级和学生')
    return
  }
  await addStudentClass(studentClassForm)
  ElMessage.success('学生已加入班级')
  await loadStudentClasses()
}

async function removeStudentClass(id) {
  await deleteStudentClass(id)
  ElMessage.success('已移除学生')
  await loadStudentClasses()
}

async function saveTask() {
  if (!isTeacher.value) return
  if (!taskForm.courseId || !taskForm.classId || !taskForm.title) {
    ElMessage.warning('请填写课程、班级和任务标题')
    return
  }
  if (editingTaskId.value) {
    await updateExperimentTask(editingTaskId.value, taskForm)
    ElMessage.success('任务已更新')
  } else {
    await createExperimentTask(taskForm)
    ElMessage.success('任务已创建')
  }
  resetTaskForm()
  await loadAll()
}

function editTask(row) {
  if (!isTeacher.value) return
  editingTaskId.value = row.id
  Object.assign(taskForm, {
    courseId: row.courseId,
    classId: row.classId,
    title: row.title,
    description: row.description,
    deadline: row.deadline,
    status: row.status,
    createdBy: row.createdBy || 2
  })
}

function resetTaskForm() {
  editingTaskId.value = null
  Object.assign(taskForm, {
    courseId: courses.value[0]?.id || null,
    classId: classes.value[0]?.id || null,
    title: '',
    description: '',
    deadline: '',
    status: 1,
    createdBy: 2
  })
}

async function removeTask(id) {
  if (!isTeacher.value) return
  await ElMessageBox.confirm('确认删除该实验任务？已关联报告时数据库会阻止删除。', '删除确认', { type: 'warning' })
  await deleteExperimentTask(id)
  ElMessage.success('任务已删除')
  await loadAll()
}

function handleFileChange(event) {
  const file = event.target.files?.[0] || null
  if (!file) {
    selectedFile.value = null
    return
  }
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!['txt', 'docx', 'pdf'].includes(ext)) {
    ElMessage.warning('只支持 txt、docx、pdf 文件')
    event.target.value = ''
    selectedFile.value = null
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 20MB')
    event.target.value = ''
    selectedFile.value = null
    return
  }
  selectedFile.value = file
}

function handleBatchFileChange(event) {
  const files = Array.from(event.target.files || [])
  const invalid = files.find(file => {
    const ext = file.name.split('.').pop()?.toLowerCase()
    return !['txt', 'docx', 'pdf'].includes(ext) || file.size > 20 * 1024 * 1024
  })
  if (invalid) {
    ElMessage.warning(`文件不符合要求：${invalid.name}`)
    event.target.value = ''
    batchFiles.value = []
    return
  }
  if (files.length > 20) {
    ElMessage.warning('一次最多批量上传 20 个文件')
    event.target.value = ''
    batchFiles.value = []
    return
  }
  batchFiles.value = files
}

async function submitReport() {
  if (!canUseReports.value) return
  if (!uploadForm.taskId || !selectedFile.value || (isTeacher.value && !uploadForm.studentId)) {
    ElMessage.warning('请选择任务、学生和报告文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('taskId', uploadForm.taskId)
    formData.append('studentId', isStudent.value ? currentUser.value.userId : uploadForm.studentId)
    formData.append('file', selectedFile.value)
    await uploadReport(formData)
    selectedFile.value = null
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
    ElMessage.success(isStudent.value ? '报告已提交' : '上传并解析完成')
    await loadAll()
  } finally {
    uploading.value = false
  }
}

async function submitBatchReports() {
  if (!isTeacher.value) return
  if (!uploadForm.taskId || !batchFiles.value.length) {
    ElMessage.warning('请选择实验任务和批量文件')
    return
  }
  batchUploading.value = true
  try {
    const formData = new FormData()
    formData.append('taskId', uploadForm.taskId)
    batchFiles.value.forEach(file => formData.append('files', file))
    await batchUploadReports(formData)
    batchFiles.value = []
    if (batchFileInputRef.value) {
      batchFileInputRef.value.value = ''
    }
    ElMessage.success('批量上传完成')
    await loadAll()
  } finally {
    batchUploading.value = false
  }
}

async function loadReports() {
  if (!canUseReports.value) return
  const result = await getReports(uploadForm.taskId)
  reports.value = result.data
}

async function removeReport(id) {
  if (!isTeacher.value) return
  await ElMessageBox.confirm('确认删除该报告记录？', '删除确认', { type: 'warning' })
  await deleteReport(id)
  ElMessage.success('报告已删除')
  await loadAll()
}

async function openReportDetail(id) {
  const result = await getReportDetail(id)
  reportDetail.value = result.data
  reportDetailVisible.value = true
}

async function startCheck() {
  if (!isTeacher.value) return
  if (!checkForm.experimentTaskId) {
    ElMessage.warning('请选择实验任务')
    return
  }
  if (selectedTaskParsedCount.value < 2) {
    ElMessage.warning('该实验任务解析成功报告不足 2 份，暂不能发起查重')
    return
  }
  checking.value = true
  try {
    const result = await createCheckTask(checkForm)
    selectedCheckTaskId.value = result.data.id
    ElMessage.success('查重完成')
    await loadAll()
    await loadResults()
  } finally {
    checking.value = false
  }
}

async function loadResults() {
  if (!canUseResults.value) return
  const result = await getCheckResults(selectedCheckTaskId.value)
  results.value = result.data
  renderCharts()
}

async function openResultDetail(id) {
  if (!canUseResults.value) return
  detailVisible.value = true
  detailLoading.value = true
  resultDetail.value = {}
  try {
    const result = await getCheckResultDetail(id)
    resultDetail.value = result.data
  } catch {
    resultDetail.value = {
      detailMessage: '相似句详情加载失败，请确认后端服务正常并重新打开详情'
    }
  } finally {
    detailLoading.value = false
  }
}

async function selectCheckTask(id) {
  selectedCheckTaskId.value = id
  await loadResults()
}

async function downloadResults() {
  if (!canUseResults.value) return
  const blob = await exportCheckResults(selectedCheckTaskId.value)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'check-results.csv'
  link.click()
  URL.revokeObjectURL(url)
}

function renderCharts() {
  if (!canUseStats.value) return
  nextTick(() => {
    if (!riskChartRef.value || !summaryChartRef.value || !similarityChartRef.value || !trendChartRef.value) return
    const riskCounts = { 高风险: 0, 中风险: 0, 低风险: 0, 正常: 0 }
    const intervalCounts = { '0-40%': 0, '40-60%': 0, '60-80%': 0, '80-100%': 0 }
    results.value.forEach(item => {
      riskCounts[item.riskLevel] = (riskCounts[item.riskLevel] || 0) + 1
      const score = Number(item.finalSimilarity || 0)
      if (score >= 0.8) intervalCounts['80-100%'] += 1
      else if (score >= 0.6) intervalCounts['60-80%'] += 1
      else if (score >= 0.4) intervalCounts['40-60%'] += 1
      else intervalCounts['0-40%'] += 1
    })
    const trendMap = {}
    checkTasks.value.forEach(item => {
      const day = String(item.createdTime || '').slice(0, 10) || '未知'
      trendMap[day] = (trendMap[day] || 0) + 1
    })
    echarts.init(riskChartRef.value).setOption({
      title: { text: '风险等级占比', left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '60%', data: Object.entries(riskCounts).map(([name, value]) => ({ name, value })) }]
    })
    echarts.init(summaryChartRef.value).setOption({
      title: { text: '核心数据统计', left: 'center' },
      tooltip: {},
      xAxis: { type: 'category', data: ['课程', '任务', '报告', '查重'] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: [stats.value.courseCount, stats.value.taskCount, stats.value.reportCount, stats.value.checkTaskCount] }]
    })
    echarts.init(similarityChartRef.value).setOption({
      title: { text: '相似度区间分布', left: 'center' },
      tooltip: {},
      xAxis: { type: 'category', data: Object.keys(intervalCounts) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: Object.values(intervalCounts) }]
    })
    echarts.init(trendChartRef.value).setOption({
      title: { text: '查重任务趋势', left: 'center' },
      tooltip: {},
      xAxis: { type: 'category', data: Object.keys(trendMap) },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: Object.values(trendMap) }]
    })
  })
}

function statusText(status) {
  if (status === 1) return '进行中'
  if (status === 2) return '已截止'
  if (status === 3) return '已归档'
  return '未知'
}

function checkTaskStatusText(status) {
  if (status === 0) return '待执行'
  if (status === 1) return '执行中'
  if (status === 2) return '已完成'
  if (status === 3) return '失败'
  return '未知'
}

function riskTagType(riskLevel) {
  if (riskLevel === '高风险') return 'danger'
  if (riskLevel === '中风险') return 'warning'
  if (riskLevel === '正常') return 'info'
  return 'success'
}

function teacherName(teacherId) {
  return teachers.value.find(item => item.id === teacherId)?.realName || teacherId
}

function formatSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>
