<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Eye, EyeOff, LockKeyhole, Shirt } from 'lucide-vue-next'
import { session, userApi } from '../api'

const router = useRouter()
const mode = ref<'login' | 'register'>('login')
const show = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')
const loginForm = reactive({ userAccount: '', userPassword: '' })
const registerForm = reactive({ userAccount: '', userPassword: '', checkPassword: '' })

function switchMode(next: 'login' | 'register') {
  mode.value = next
  error.value = ''
  success.value = ''
}

async function login() {
  error.value = ''
  if (!loginForm.userAccount || !loginForm.userPassword) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  try {
    const { data } = await userApi.login(loginForm)
    session.user = data.data
    localStorage.removeItem('demo-mode')
    router.push('/dashboard')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败，请检查账号和密码'
  } finally {
    loading.value = false
  }
}

async function register() {
  error.value = ''
  success.value = ''
  if (registerForm.userAccount.length < 6) {
    error.value = '账号长度不能少于 6 位'
    return
  }
  if (registerForm.userPassword.length < 8) {
    error.value = '密码长度不能少于 8 位'
    return
  }
  if (registerForm.userPassword !== registerForm.checkPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await userApi.register(registerForm)
    loginForm.userAccount = registerForm.userAccount
    loginForm.userPassword = ''
    success.value = '注册成功，请使用新账号登录'
    mode.value = 'login'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '注册失败'
  } finally {
    loading.value = false
  }
}

function demo() {
  session.enterDemo()
  router.push('/dashboard')
}
</script>

<template>
  <div class="login">
    <section class="login-visual">
      <div class="visual-brand"><span><Shirt /></span><strong>织序</strong><small>DESIGN FLOW</small></div>
      <div class="sketch-card">
        <div class="coat"><i class="body"/><i class="neck"/><i class="left"/><i class="right"/><i class="line one"/><i class="line two"/></div>
        <span>STYLE NO. FW26-014</span>
      </div>
      <div class="swatches"><i/><i/><i/><i/><i/><i/><small>COLOR & MATERIAL STUDY</small></div>
      <div class="visual-copy">
        <p>FROM BRIEF TO ARCHIVE</p>
        <h1>让每一件作品，<br>都有清晰的来路。</h1>
        <i/>
        <span>项目、任务、设计稿与审核记录，在同一条时间线上有序发生。</span>
      </div>
      <footer><span>2026 AUTUMN / WINTER</span><span>WORKFLOW SYSTEM · 01</span></footer>
    </section>

    <section class="login-form">
      <div class="form-box">
        <div class="mobile-brand"><Shirt /><strong>织序</strong></div>
        <div class="auth-tabs">
          <button :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
          <button :class="{ active: mode === 'register' }" @click="switchMode('register')">注册账号</button>
        </div>

        <template v-if="mode === 'login'">
          <p class="eyebrow">WELCOME BACK</p>
          <h2>登录工作空间</h2>
          <p class="intro">继续管理你的设计项目与协作流程。</p>
          <form @submit.prevent="login">
            <label><span>账号</span><input v-model.trim="loginForm.userAccount" autocomplete="username" placeholder="请输入用户名"></label>
            <label>
              <span>密码</span>
              <div class="password">
                <input v-model="loginForm.userPassword" :type="show ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入登录密码">
                <button type="button" :aria-label="show ? '隐藏密码' : '显示密码'" @click="show = !show"><EyeOff v-if="show"/><Eye v-else/></button>
              </div>
            </label>
            <div class="login-options"><label><input type="checkbox" checked> 保持登录</label><button type="button">忘记密码？</button></div>
            <p v-if="success" class="auth-success">{{ success }}</p>
            <p v-if="error" class="error">{{ error }}</p>
            <button class="submit" :disabled="loading"><span>{{ loading ? '正在登录…' : '进入工作台' }}</span><ArrowRight /></button>
          </form>
          <div class="demo-entry"><span>尚未准备完整后端数据？</span><button @click="demo">进入项目经理演示</button></div>
        </template>

        <template v-else>
          <p class="eyebrow">CREATE ACCOUNT</p>
          <h2>注册新账号</h2>
          <p class="intro">创建普通成员账号，注册后即可登录工作空间。</p>
          <form @submit.prevent="register">
            <label><span>登录账号</span><input v-model.trim="registerForm.userAccount" autocomplete="username" minlength="6" placeholder="至少 6 位字符"></label>
            <label>
              <span>登录密码</span>
              <div class="password">
                <input v-model="registerForm.userPassword" :type="show ? 'text' : 'password'" autocomplete="new-password" minlength="8" placeholder="至少 8 位字符">
                <button type="button" :aria-label="show ? '隐藏密码' : '显示密码'" @click="show = !show"><EyeOff v-if="show"/><Eye v-else/></button>
              </div>
            </label>
            <label><span>确认密码</span><input v-model="registerForm.checkPassword" type="password" autocomplete="new-password" minlength="8" placeholder="请再次输入密码"></label>
            <p v-if="error" class="error">{{ error }}</p>
            <button class="submit" :disabled="loading"><span>{{ loading ? '正在注册…' : '创建账号' }}</span><ArrowRight /></button>
          </form>
          <div class="register-note">注册账号默认获得普通用户角色，其他角色由管理员分配。</div>
        </template>

        <div class="safe"><LockKeyhole /> 会话由 Sa-Token 安全保护</div>
      </div>
      <footer>© 2026 织序 · 服装设计业务流程管理系统</footer>
    </section>
  </div>
</template>
