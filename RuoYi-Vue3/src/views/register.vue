<template>
  <div class="register">
    <section class="register-intro">
      <div class="brand-mark">AI</div>
      <p>Business Account</p>
      <h1>注册业务账号</h1>
      <span>注册后直接进入租户、户主或中介工作台。审核员和超级管理员仍由平台后台分配，不开放自助注册。</span>
      <div class="intro-grid">
        <strong>租户找房</strong>
        <strong>户主发布</strong>
        <strong>中介承接</strong>
        <strong>AI 辅助</strong>
      </div>
    </section>

    <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
      <div class="form-head">
        <p>业务注册</p>
        <h2>{{ title }}</h2>
        <span>请先选择你的业务身份，注册后将只看到该身份对应的工作台。</span>
      </div>

      <el-form-item prop="registerRole">
        <el-select v-model="registerForm.registerRole" size="large" placeholder="选择业务身份">
          <el-option label="租户" value="user" />
          <el-option label="户主" value="owner" />
          <el-option label="中介" value="agent" />
        </el-select>
      </el-form-item>

      <div class="form-grid two">
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="账号"
          >
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="nickName">
          <el-input
            v-model="registerForm.nickName"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="昵称"
          />
        </el-form-item>
      </div>

      <div class="form-grid two">
        <el-form-item prop="realName">
          <el-input
            v-model="registerForm.realName"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="真实姓名"
          />
        </el-form-item>
        <el-form-item prop="phonenumber">
          <el-input
            v-model="registerForm.phonenumber"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="手机号"
          />
        </el-form-item>
      </div>

      <el-form-item prop="email">
        <el-input
          v-model="registerForm.email"
          type="text"
          size="large"
          auto-complete="off"
          placeholder="邮箱"
        />
      </el-form-item>

      <el-form-item prop="password" :rules="registerPwdValidator">
        <el-input
          v-model="registerForm.password"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>

      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="确认密码"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>

      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          size="large"
          v-model="registerForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="register-code">
          <img :src="codeUrl" @click="getCode" class="register-code-img" />
        </div>
      </el-form-item>

      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="large"
          type="primary"
          style="width:100%;"
          @click.prevent="handleRegister"
        >
          <span v-if="!loading">注册并进入业务台</span>
          <span v-else>注册中...</span>
        </el-button>
        <div class="register-links">
          <router-link class="link-type" :to="'/login'">使用已有账户登录</router-link>
        </div>
      </el-form-item>
    </el-form>

    <div class="el-register-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus"
import { getCodeImg, register } from "@/api/login"
import defaultSettings from '@/settings'
import { usePasswordRule } from "@/utils/passwordRule"

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()
const { registerPwdValidator } = usePasswordRule()

const registerForm = ref({
  username: "",
  nickName: "",
  realName: "",
  phonenumber: "",
  email: "",
  registerRole: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: ""
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const registerRules = {
  registerRole: [
    { required: true, trigger: "change", message: "请选择业务身份" }
  ],
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: "用户账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  nickName: [
    { required: true, trigger: "blur", message: "请输入昵称" }
  ],
  realName: [
    { required: true, trigger: "blur", message: "请输入真实姓名" }
  ],
  phonenumber: [
    { required: true, trigger: "blur", message: "请输入手机号" },
    { pattern: /^1\d{10}$/, trigger: "blur", message: "请输入有效的 11 位手机号" }
  ],
  email: [
    { required: true, trigger: "blur", message: "请输入邮箱" },
    { type: "email", trigger: ["blur", "change"], message: "请输入有效邮箱地址" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        const username = registerForm.value.username
        ElMessageBox.alert("<font color='red'>账号 " + username + " 注册成功，请登录后进入对应业务工作台。</font>", "系统提示", {
          dangerouslyUseHTMLString: true,
          type: "success",
        }).then(() => {
          router.push("/login")
        }).catch(() => {})
      }).catch(() => {
        loading.value = false
        if (captchaEnabled) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      registerForm.value.uuid = res.uuid
    }
  })
}

getCode()
</script>

<style lang='scss' scoped>
.register {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  gap: 56px;
  padding: 48px 8vw;
  background:
    linear-gradient(120deg, rgba(12, 24, 38, 0.9), rgba(18, 64, 70, 0.76)),
    url("../assets/images/login-background.jpg");
  background-size: cover;
}

.register-intro {
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

.register-intro p {
  margin: 0 0 12px;
  color: #b8f1df;
  font-size: 13px;
  text-transform: uppercase;
}

.register-intro h1 {
  margin: 0;
  font-size: 42px;
  line-height: 1.18;
}

.register-intro > span {
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

.register-form {
  flex: 0 0 460px;
  width: 460px;
  padding: 30px 30px 12px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 8px;
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
}

.form-head span {
  display: block;
  margin-top: 10px;
  color: #667085;
  line-height: 1.6;
}

.register-form .el-input,
.register-form :deep(.el-select),
.register-form :deep(.el-input__wrapper) {
  height: 40px;
}

.register-form .el-input input {
  height: 40px;
}

.input-icon {
  height: 39px;
  width: 14px;
  margin-left: 0;
}

.form-grid {
  display: grid;
  gap: 12px;
}

.form-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.register-code {
  width: 33%;
  height: 40px;
  float: right;
}

.register-code img {
  cursor: pointer;
  vertical-align: middle;
}

.register-links {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.link-type {
  color: #0f766e;
  text-decoration: none;
}

.el-register-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-size: 12px;
  letter-spacing: 0;
}

.register-code-img {
  height: 40px;
  padding-left: 12px;
}

@media (max-width: 980px) {
  .register {
    justify-content: center;
    padding: 28px 18px 56px;
  }

  .register-intro {
    display: none;
  }

  .register-form {
    flex: 1;
    width: 100%;
    max-width: 460px;
  }

  .form-grid.two,
  .intro-grid {
    grid-template-columns: 1fr;
  }
}
</style>
