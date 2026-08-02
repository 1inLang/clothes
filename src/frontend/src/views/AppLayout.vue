<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell, BriefcaseBusiness, ChevronDown, ClipboardCheck, FileClock,
  LayoutDashboard, LogOut, Menu, Search, ShieldCheck, Shirt, Users, X,
} from 'lucide-vue-next'
import { notificationApi, session, userApi } from '../api'

const route = useRoute()
const router = useRouter()
const open = ref(false)
const unreadCount = ref(0)
let unreadTimer: number | undefined
const user = computed(() => session.user)
const roleLabels: Record<string, string> = {
  admin: '系统管理员', project_manager: '项目经理', project_acceptor: '项目验收人', designer: '设计师',
  reviewer: '审核人', user: '普通成员',
}
const role = computed(() => {
  const roles = user.value?.userRoles?.length ? user.value.userRoles : [user.value?.userRole || 'user']
  return roles.map(item => roleLabels[item] || item).join(' / ')
})

const groups = computed(() => [
  { label: '工作空间', items: [
    { text: '工作台', path: '/dashboard', icon: LayoutDashboard },
    { text: '设计项目', path: '/projects', icon: BriefcaseBusiness },
    { text: '设计任务', path: '/tasks', icon: Shirt },
    { text: '审核中心', path: '/reviews', icon: ClipboardCheck },
  ] },
  { label: '组织与权限', items: [
    { text: '用户管理', path: '/users', icon: Users },
    { text: '角色权限', path: '/roles', icon: ShieldCheck },
  ] },
  { label: '系统', items: [
    { text: '消息通知', path: '/notifications', icon: Bell, count: unreadCount.value },
    { text: '审计日志', path: '/audit', icon: FileClock },
  ] },
])

function active(path: string) { return route.path === path }
async function logout() {
  try { if (!session.demo) await userApi.logout() } catch { /* clear local state regardless */ }
  session.clear()
  router.push('/login')
}
async function loadUnreadCount() {
  if (session.demo) { unreadCount.value = 2; return }
  try { unreadCount.value = (await notificationApi.unreadCount()).data.data } catch { unreadCount.value = 0 }
}
function updateUnreadCount(event: Event) {
  unreadCount.value = Number((event as CustomEvent<number>).detail || 0)
}
onMounted(() => {
  loadUnreadCount()
  unreadTimer = window.setInterval(loadUnreadCount, 60000)
  window.addEventListener('notification-count-changed', updateUnreadCount)
})
onBeforeUnmount(() => {
  if (unreadTimer) window.clearInterval(unreadTimer)
  window.removeEventListener('notification-count-changed', updateUnreadCount)
})
</script>

<template>
  <div class="shell">
    <div v-if="open" class="scrim" @click="open=false" />
    <aside :class="{ open }">
      <div class="brand"><span><Shirt /></span><div><strong>织序</strong><small>DESIGN FLOW</small></div><button @click="open=false"><X /></button></div>
      <nav>
        <section v-for="group in groups" :key="group.label">
          <p>{{ group.label }}</p>
          <router-link v-for="item in group.items" :key="item.path" :to="item.path" :class="{active:active(item.path)}" @click="open=false">
            <component :is="item.icon" /><span>{{ item.text }}</span><b v-if="item.count">{{ item.count }}</b>
          </router-link>
        </section>
      </nav>
      <div class="sidebar-bottom">
        <div class="season"><span>当前企划季</span><strong>2026 秋冬</strong><i><b /></i><small>整体进度 62%</small></div>
        <button class="user-mini" @click="logout"><span class="avatar">{{ user?.userName?.slice(-1) }}</span><span><strong>{{ user?.userName }}</strong><small>{{ role }}</small></span><LogOut /></button>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <button class="mobile-menu" @click="open=true"><Menu /></button>
        <div class="search"><Search /><input placeholder="搜索项目、任务或成员…" /><kbd>⌘ K</kbd></div>
        <div class="top-actions"><span v-if="session.demo" class="demo-badge">演示模式</span><router-link to="/notifications" class="notify"><Bell /><i v-if="unreadCount" /></router-link><span class="avatar small">{{ user?.userName?.slice(-1) }}</span><ChevronDown /></div>
      </header>
      <main><router-view /></main>
    </div>
  </div>
</template>
