<template>
  <div class="login">
    <section class="login-intro">
      <div class="brand-mark">AI</div>
      <p>Enterprise Rental Platform</p>
      <h1>智能AI房屋租赁系统</h1>
      <span>房源审核、委托协作、租赁沟通、合同确认与 AI 知识库统一工作台。</span>
      <div class="intro-grid">
        <strong>合规审核</strong>
        <strong>双向撮合</strong>
        <strong>智能助手</strong>
        <strong>合同协作</strong>
      </div>
    </section>

    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="form-head">
        <p>安全登录</p>
        <h2>{{ title }}</h2>
        <span>业务用户可自助注册；审核员和超级管理员继续由平台后台分配。</span>
      </div>
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号">
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item v-if="captchaEnabled" prop="code">
        <el-input
          v-model="loginForm.code"
          size="large"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" class="login-code-img" @click="getCode" />
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin: 0 0 18px 0;">记住密码</el-checkbox>

      <div v-if="devLoginEnabled" class="quick-login">
        <span>开发账号</span>
        <div class="quick-login__buttons">
          <el-button
            v-for="account in devAccounts"
            :key="account.username"
            size="small"
            plain
            @click="useDevAccount(account)"
          >
            {{ account.label }}
          </el-button>
        </div>
      </div>

      <div class="register-entry">
        <span>没有账号？</span>
        <router-link class="link-type" to="/register">注册业务账号</router-link>
      </div>

      <el-form-item style="width: 100%;">
        <el-button :loading="loading" size="large" type="primary" style="width: 100%;" @click.prevent="handleLogin">
          <span v-if="!loading">登录</span>
          <span v-else>登录中...</span>
        </el-button>
      </el-form-item>
    </el-form>
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'
import usePermissionStore from '@/store/modules/permission'
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const devAccounts = [
  { label: '超级后台', username: 'admin', password: 'admin123' },
  { label: '审核员', username: 'auditor_test', password: 'admin123' },
  { label: '租户', username: 'tenant_test', password: 'admin123' },
  { label: '户主', username: 'owner_test', password: 'admin123' },
  { label: '中介', username: 'agent_test', password: 'admin123' }
]

const loginForm = ref({
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
})

const loginRules = {
  username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
  password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

const codeUrl = ref('')
const loading = ref(false)
const captchaEnabled = ref(true)
const redirect = ref(undefined)
const devLoginEnabled = import.meta.env.VITE_DEV_LOGIN === 'true'

watch(
  route,
  (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
  },
  { immediate: true }
)

function getHomeByRole(roles) {
  const roleList = roles || []
  if (roleList.includes('admin')) {
    return '/index'
  }
  if (roleList.includes('auditor')) {
    return '/portal/auditor'
  }
  return '/portal/index'
}

function useDevAccount(account) {
  loginForm.value.username = account.username
  loginForm.value.password = account.password
}

function handleLogin() {
  proxy.$refs.loginRef.validate((valid) => {
    if (!valid) {
      return
    }

    loading.value = true

    if (loginForm.value.rememberMe) {
      Cookies.set('username', loginForm.value.username, { expires: 30 })
      Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
      Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 })
    } else {
      Cookies.remove('username')
      Cookies.remove('password')
      Cookies.remove('rememberMe')
    }

    userStore.login(loginForm.value).then(async () => {
      await userStore.getInfo()
      await permissionStore.generateRoutes()
      const query = route.query
      const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
        if (cur !== 'redirect') {
          acc[cur] = query[cur]
        }
        return acc
      }, {})
      router.push({ path: redirect.value || getHomeByRole(userStore.roles), query: otherQueryParams })
    }).catch(() => {
      loading.value = false
      if (captchaEnabled.value) {
        getCode()
      }
    })
  })
}

function getCode() {
  getCodeImg().then((res) => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
    code: '',
    uuid: ''
  }
}

getCode()
getCookie()
</script>

<style lang="scss" scoped>
.login {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 72px;
  height: 100%;
  padding: 56px 8vw;
  background:
    radial-gradient(circle at 18% 16%, rgba(26, 115, 232, 0.14), transparent 30%),
    radial-gradient(circle at 82% 22%, rgba(147, 52, 230, 0.12), transparent 28%),
    linear-gradient(180deg, #ffffff 0%, #f8fafd 48%, #f0f4f9 100%);
  overflow: hidden;
}

.login-intro {
  max-width: 620px;
  color: var(--sr-text);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  margin-bottom: 28px;
  color: var(--sr-primary);
  background: #e8f0fe;
  border: 1px solid rgba(11, 87, 208, 0.16);
  border-radius: 999px;
  font-weight: 700;
}

.login-intro p {
  margin: 0 0 12px;
  color: var(--sr-muted);
  font-size: 13px;
  text-transform: uppercase;
}

.login-intro h1 {
  margin: 0;
  color: transparent;
  background: linear-gradient(90deg, #1a73e8 0%, #9334e6 48%, #007b83 100%);
  background-clip: text;
  font-size: 48px;
  line-height: 1.18;
  letter-spacing: 0;
}

.login-intro > span {
  display: block;
  max-width: 560px;
  margin-top: 18px;
  color: var(--sr-muted);
  line-height: 1.8;
}

.intro-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 30px;
}

.intro-grid strong {
  padding: 12px 10px;
  color: var(--sr-text);
  text-align: center;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--sr-line);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.login-form {
  flex: 0 0 420px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.9);
  width: 420px;
  padding: 32px 30px 14px;
  z-index: 1;
  border: 1px solid var(--sr-line);
  box-shadow: 0 24px 80px rgba(60, 64, 67, 0.18);
  backdrop-filter: blur(24px);
}

.form-head {
  margin-bottom: 26px;
}

.form-head p {
  margin: 0 0 8px;
  color: var(--sr-primary);
  font-size: 13px;
  font-weight: 600;
}

.form-head h2 {
  margin: 0;
  color: var(--sr-text-strong);
  font-size: 24px;
  letter-spacing: 0;
}

.form-head span {
  display: block;
  margin-top: 10px;
  color: var(--sr-muted);
  line-height: 1.6;
}

.login-form .el-input {
  height: 44px;
}

.login-form .el-input input {
  height: 44px;
}

.input-icon {
  height: 43px;
  width: 14px;
  margin-left: 0;
}

.login-code {
  width: 33%;
  height: 44px;
  float: right;
}

.login-code img {
  cursor: pointer;
  vertical-align: middle;
}

.quick-login {
  padding: 12px;
  margin-bottom: 18px;
  background: #f8fafd;
  border: 1px solid var(--sr-line);
  border-radius: 20px;
}

.quick-login > span {
  display: block;
  margin-bottom: 8px;
  color: var(--sr-subtle);
  font-size: 12px;
}

.quick-login__buttons {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.quick-login__buttons .el-button {
  width: 100%;
  margin-left: 0;
  padding-right: 0;
  padding-left: 0;
}

.register-entry {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin: 0 0 18px;
  color: var(--sr-muted);
  font-size: 13px;
}

.register-entry .link-type {
  color: var(--sr-primary);
  text-decoration: none;
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: var(--sr-subtle);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 0;
}

.login-code-img {
  height: 44px;
  padding-left: 12px;
  border-radius: 14px;
}

html.dark .login {
  background:
    radial-gradient(circle at 18% 16%, rgba(26, 115, 232, 0.14), transparent 30%),
    radial-gradient(circle at 82% 22%, rgba(147, 52, 230, 0.12), transparent 28%),
    linear-gradient(180deg, #ffffff 0%, #f8fafd 48%, #f0f4f9 100%);
}

html.dark .login-form {
  background: rgba(255, 255, 255, 0.9) !important;
  box-shadow: 0 24px 80px rgba(60, 64, 67, 0.18);
}

@media (max-width: 980px) {
  .login {
    justify-content: center;
    padding: 28px 18px 56px;
  }

  .login-intro {
    display: none;
  }

  .login-form {
    flex: 1;
    width: 100%;
    max-width: 420px;
  }
}
</style>
