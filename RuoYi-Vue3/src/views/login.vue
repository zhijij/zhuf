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
  gap: 56px;
  height: 100%;
  padding: 48px 8vw;
  background:
    linear-gradient(120deg, rgba(12, 24, 38, 0.9), rgba(18, 64, 70, 0.76)),
    url('../assets/images/login-background.jpg');
  background-size: cover;
  background-position: center;
}

.login-intro {
  max-width: 620px;
  color: #fff;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 28px;
  color: #12252b;
  background: #b8f1df;
  border-radius: 8px;
  font-weight: 800;
}

.login-intro p {
  margin: 0 0 12px;
  color: #b8f1df;
  font-size: 13px;
  text-transform: uppercase;
}

.login-intro h1 {
  margin: 0;
  font-size: 42px;
  line-height: 1.18;
  letter-spacing: 0;
}

.login-intro > span {
  display: block;
  max-width: 560px;
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.78);
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
  color: #e9fbf5;
  text-align: center;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  font-size: 13px;
}

.login-form {
  flex: 0 0 420px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  width: 420px;
  padding: 30px 30px 12px;
  z-index: 1;
  box-shadow: 0 28px 80px rgba(10, 24, 38, 0.32);
}

.form-head {
  margin-bottom: 26px;
}

.form-head p {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
}

.form-head h2 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  letter-spacing: 0;
}

.form-head span {
  display: block;
  margin-top: 10px;
  color: #667085;
  line-height: 1.6;
}

.login-form .el-input {
  height: 40px;
}

.login-form .el-input input {
  height: 40px;
}

.input-icon {
  height: 39px;
  width: 14px;
  margin-left: 0;
}

.login-code {
  width: 33%;
  height: 40px;
  float: right;
}

.login-code img {
  cursor: pointer;
  vertical-align: middle;
}

.quick-login {
  padding: 10px 12px;
  margin-bottom: 18px;
  background: #f8fafb;
  border: 1px solid #dfe5ea;
  border-radius: 6px;
}

.quick-login > span {
  display: block;
  margin-bottom: 8px;
  color: #606266;
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
  color: #667085;
  font-size: 13px;
}

.register-entry .link-type {
  color: #0f766e;
  text-decoration: none;
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.74);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 0;
}

.login-code-img {
  height: 40px;
  padding-left: 12px;
}

html.dark .login {
  background-image: linear-gradient(rgba(0, 0, 0, 0.68), rgba(0, 0, 0, 0.68)), url('../assets/images/login-background.jpg');
}

html.dark .login-form {
  background: var(--el-bg-color-overlay) !important;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);
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
