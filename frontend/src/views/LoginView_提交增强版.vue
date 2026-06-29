<template>
  <main class="auth-page">
    <section class="brand-panel">
      <div class="brand-block">
        <span class="brand-badge">REPORT CHECK</span>
        <h1>实验报告智能查重与分析系统</h1>
        <p>
          面向课程实验报告提交、解析、查重、风险分析与结果导出的完整教学管理系统。
        </p>
      </div>

      <div class="case-grid">
        <article v-for="item in caseItems" :key="item.title" class="case-card">
          <div class="case-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div>
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-header">
        <h2>{{ activeModeTitle }}</h2>
        <p>{{ activeModeSubtitle }}</p>
      </div>

      <el-tabs v-model="activeMode" stretch class="auth-tabs" @tab-change="clearState">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
        <el-tab-pane label="找回密码" name="forgot" />
      </el-tabs>

      <el-alert
        v-if="message.text"
        :title="message.text"
        :type="message.type"
        show-icon
        :closable="false"
        class="auth-message"
      />

      <el-form
        v-if="activeMode === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model.trim="loginForm.username"
            size="large"
            autocomplete="username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            size="large"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>

        <div class="form-row">
          <el-checkbox v-model="rememberLogin">记住用户名</el-checkbox>
          <el-button link type="primary" @click="activeMode = 'forgot'">忘记密码</el-button>
        </div>

        <el-button class="submit-button" type="primary" size="large" :loading="loading" @click="handleLogin">
          登录系统
        </el-button>
      </el-form>

      <el-form
        v-if="activeMode === 'register'"
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="注册身份" prop="roleCode">
          <el-segmented v-model="registerForm.roleCode" :options="registerRoleOptions" class="role-segment" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model.trim="registerForm.username"
              size="large"
              autocomplete="username"
              placeholder="4-20 位字母、数字或下划线"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="真实姓名" prop="realName">
            <el-input
              v-model.trim="registerForm.realName"
              size="large"
              autocomplete="name"
              placeholder="请输入真实姓名"
              :prefix-icon="UserFilled"
            />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item :label="registerForm.roleCode === 'TEACHER' ? '教师工号' : '学生学号'" prop="identityNo">
            <el-input
              v-model.trim="registerForm.identityNo"
              size="large"
              :placeholder="registerForm.roleCode === 'TEACHER' ? '例如 T2026001' : '例如 20260001'"
              :prefix-icon="Tickets"
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model.trim="registerForm.email"
              size="large"
              autocomplete="email"
              placeholder="用于通知与找回密码"
              :prefix-icon="Message"
            />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="registerForm.password"
              size="large"
              type="password"
              autocomplete="new-password"
              placeholder="至少 6 位"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              size="large"
              type="password"
              autocomplete="new-password"
              placeholder="请再次输入密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
        </div>

        <el-alert
          title="管理员账号由系统管理员在后台创建，注册入口仅开放教师和学生身份。"
          type="info"
          show-icon
          :closable="false"
          class="inline-tip"
        />

        <el-button class="submit-button" type="primary" size="large" :loading="loading" @click="handleRegister">
          提交注册
        </el-button>
      </el-form>

      <el-form
        v-if="activeMode === 'forgot'"
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model.trim="forgotForm.username"
            size="large"
            autocomplete="username"
            placeholder="请输入需要找回的用户名"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="绑定邮箱" prop="email">
          <el-input
            v-model.trim="forgotForm.email"
            size="large"
            autocomplete="email"
            placeholder="请输入注册时填写的邮箱"
            :prefix-icon="Message"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="新密码" prop="password">
            <el-input
              v-model="forgotForm.password"
              size="large"
              type="password"
              autocomplete="new-password"
              placeholder="至少 6 位"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="forgotForm.confirmPassword"
              size="large"
              type="password"
              autocomplete="new-password"
              placeholder="请再次输入新密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
        </div>

        <el-button class="submit-button" type="primary" size="large" :loading="loading" @click="handleForgotPassword">
          重置密码
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis,
  DocumentChecked,
  Lock,
  Message,
  Tickets,
  UploadFilled,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { forgotPassword, login, register } from '../api/auth'

const router = useRouter()
const activeMode = ref('login')
const loading = ref(false)
const rememberLogin = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)
const forgotFormRef = ref(null)
const message = reactive({ type: 'info', text: '' })

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  roleCode: 'STUDENT',
  username: '',
  realName: '',
  identityNo: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const forgotForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const registerRoleOptions = [
  { label: '学生', value: 'STUDENT' },
  { label: '教师', value: 'TEACHER' }
]

const caseItems = [
  {
    title: '报告提交',
    desc: '学生按实验任务提交 txt、docx、pdf 报告，系统记录解析状态与字数。',
    icon: UploadFilled
  },
  {
    title: '智能查重',
    desc: '教师发起班级报告对比，结合 TF-IDF、余弦相似度和 SimHash 给出风险等级。',
    icon: DocumentChecked
  },
  {
    title: '结果分析',
    desc: '支持查看相似句明细、相似度区间分布、风险统计和查重任务趋势。',
    icon: DataAnalysis
  }
]

const activeModeTitle = computed(() => {
  if (activeMode.value === 'register') return '创建新账号'
  if (activeMode.value === 'forgot') return '找回账号密码'
  return '账号登录'
})

const activeModeSubtitle = computed(() => {
  if (activeMode.value === 'register') return '教师和学生可自主注册，管理员账号由后台维护'
  if (activeMode.value === 'forgot') return '通过用户名和绑定邮箱完成密码重置'
  return '请输入个人账号信息进入系统'
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  roleCode: [{ required: true, message: '请选择注册身份', trigger: 'change' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '用户名需为 4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  identityNo: [{ required: true, message: '请输入学号或工号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateRegisterPassword, trigger: 'blur' }
  ]
}

const forgotRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入绑定邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateForgotPassword, trigger: 'blur' }
  ]
}

onMounted(() => {
  const savedUsername = localStorage.getItem('rememberedUsername')
  if (savedUsername) {
    loginForm.username = savedUsername
    rememberLogin.value = true
  }
})

watch(() => registerForm.roleCode, () => {
  registerForm.identityNo = ''
})

function validateRegisterPassword(rule, value, callback) {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

function validateForgotPassword(rule, value, callback) {
  if (value !== forgotForm.password) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

function clearState() {
  message.text = ''
}

function showMessage(type, text) {
  message.type = type
  message.text = text
}

async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const result = await login({
      username: loginForm.username,
      password: loginForm.password
    })
    localStorage.setItem('token', result.data.token)
    localStorage.setItem('user', JSON.stringify(result.data))
    if (rememberLogin.value) {
      localStorage.setItem('rememberedUsername', loginForm.username)
    } else {
      localStorage.removeItem('rememberedUsername')
    }
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    showMessage('error', error?.response?.data?.message || error?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      realName: registerForm.realName,
      roleCode: registerForm.roleCode,
      studentNo: registerForm.roleCode === 'STUDENT' ? registerForm.identityNo : '',
      teacherNo: registerForm.roleCode === 'TEACHER' ? registerForm.identityNo : '',
      email: registerForm.email
    })
    showMessage('success', '注册申请已提交，请返回登录页使用新账号登录')
    activeMode.value = 'login'
    loginForm.username = registerForm.username
    resetRegisterForm()
  } catch (error) {
    showMessage('error', error?.response?.data?.message || error?.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handleForgotPassword() {
  await forgotFormRef.value.validate()
  loading.value = true
  try {
    await forgotPassword({
      username: forgotForm.username,
      email: forgotForm.email,
      password: forgotForm.password
    })
    showMessage('success', '密码已重置，请使用新密码登录')
    activeMode.value = 'login'
    loginForm.username = forgotForm.username
    resetForgotForm()
  } catch (error) {
    showMessage('error', error?.response?.data?.message || error?.message || '密码重置失败，请核对用户名和邮箱')
  } finally {
    loading.value = false
  }
}

function resetRegisterForm() {
  Object.assign(registerForm, {
    roleCode: 'STUDENT',
    username: '',
    realName: '',
    identityNo: '',
    email: '',
    password: '',
    confirmPassword: ''
  })
  registerFormRef.value?.clearValidate()
}

function resetForgotForm() {
  Object.assign(forgotForm, {
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  })
  forgotFormRef.value?.clearValidate()
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(420px, 560px);
  gap: 40px;
  align-items: center;
  padding: 48px;
  background:
    linear-gradient(135deg, rgba(22, 119, 255, 0.08), rgba(31, 191, 117, 0.08)),
    #eef3f8;
  color: #172033;
}

.brand-panel {
  max-width: 760px;
}

.brand-block {
  margin-bottom: 36px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border: 1px solid rgba(22, 119, 255, 0.22);
  border-radius: 999px;
  color: #1767d1;
  background: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.brand-block h1 {
  margin: 18px 0 14px;
  font-size: 46px;
  line-height: 1.16;
  letter-spacing: 0;
}

.brand-block p {
  max-width: 640px;
  margin: 0;
  color: #526071;
  font-size: 18px;
  line-height: 1.8;
}

.case-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.case-card {
  min-height: 168px;
  padding: 20px;
  border: 1px solid rgba(139, 152, 170, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 16px 36px rgba(30, 48, 80, 0.08);
}

.case-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  margin-bottom: 16px;
  border-radius: 8px;
  color: #1677ff;
  background: rgba(22, 119, 255, 0.1);
  font-size: 21px;
}

.case-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 17px;
}

.case-card span {
  display: block;
  color: #617083;
  font-size: 14px;
  line-height: 1.7;
}

.auth-panel {
  width: 100%;
  padding: 40px;
  border: 1px solid rgba(139, 152, 170, 0.26);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 22px 60px rgba(25, 42, 70, 0.14);
}

.auth-header {
  margin-bottom: 20px;
}

.auth-header h2 {
  margin: 0 0 8px;
  font-size: 28px;
  line-height: 1.3;
  letter-spacing: 0;
}

.auth-header p {
  margin: 0;
  color: #667589;
  font-size: 15px;
}

.auth-tabs {
  margin-bottom: 20px;
}

.auth-message,
.inline-tip {
  margin-bottom: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 22px;
}

.role-segment {
  width: 100%;
}

.submit-button {
  width: 100%;
  margin-top: 6px;
  font-weight: 700;
}

:deep(.el-input__wrapper),
:deep(.el-segmented) {
  border-radius: 8px;
}

:deep(.el-form-item__label) {
  color: #2d3848;
  font-weight: 600;
}

@media (max-width: 1080px) {
  .auth-page {
    grid-template-columns: 1fr;
    padding: 32px;
  }

  .brand-panel {
    max-width: none;
  }
}

@media (max-width: 720px) {
  .auth-page {
    padding: 20px;
  }

  .brand-block h1 {
    font-size: 34px;
  }

  .brand-block p {
    font-size: 16px;
  }

  .case-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .auth-panel {
    padding: 24px;
  }
}
</style>
