<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ChevronLeft, ChevronRight, Search, UserPlus, X } from 'lucide-vue-next'
import PageHead from '../components/PageHead.vue'
import StatusPill from '../components/StatusPill.vue'
import { members } from '../demo'
import {
  roleApi, session, userApi,
  type SysRole, type UserRole, type UserVO,
} from '../api'

type RowUser = UserVO & { department?: string; status?: string; lastLogin?: string }

const isAdmin = session.hasRole('admin') && !session.demo
const rows = ref<RowUser[]>([])
const loading = ref(false)
const message = ref('')
const total = ref(0)
const current = ref(1)
const pageSize = 10
const filters = reactive({ keyword: '', userRole: '' })
const modal = ref<'add' | 'edit' | null>(null)
const saving = ref(false)
const form = reactive<{
  id?: string
  userAccount: string
  userName: string
  roleCodes: UserRole[]
  userProfile: string
}>({ userAccount: '', userName: '', roleCodes: ['designer'], userProfile: '' })

const availableRoles = ref<SysRole[]>([
  { id: '1', roleName: '系统管理员', roleCode: 'admin', sortOrder: 10, status: 1 },
  { id: '2', roleName: '项目经理', roleCode: 'project_manager', sortOrder: 20, status: 1 },
  { id: '6', roleName: '项目验收人', roleCode: 'project_acceptor', sortOrder: 25, status: 1 },
  { id: '3', roleName: '设计师', roleCode: 'designer', sortOrder: 30, status: 1 },
  { id: '4', roleName: '审核人', roleCode: 'reviewer', sortOrder: 40, status: 1 },
  { id: '5', roleName: '普通成员', roleCode: 'user', sortOrder: 50, status: 1 },
])

const roleLabels: Record<string, string> = {
  admin: '系统管理员',
  project_manager: '项目经理',
  project_acceptor: '项目验收人',
  designer: '设计师',
  reviewer: '审核人',
  user: '普通成员',
}

async function loadRoleOptions() {
  if (!isAdmin) return
  try {
    const { data } = await roleApi.list({
      current: 1,
      pageSize: 100,
      status: 1,
      sortField: 'sortOrder',
      sortOrder: 'ascend',
    })
    availableRoles.value = data.data.records
    for (const role of availableRoles.value) roleLabels[role.roleCode] = role.roleName
  } catch {
    // 角色列表加载失败时保留内置角色，用户列表仍可正常使用。
  }
}

function demoRows(): RowUser[] {
  return members.map((item) => ({
    id: String(item.id),
    userAccount: item.account,
    userName: item.name,
    userRole: item.roleValue as UserRole,
    userRoles: [item.roleValue as UserRole],
    department: item.department,
    status: item.status,
    lastLogin: item.lastLogin,
  }))
}

async function load() {
  if (!isAdmin) {
    rows.value = demoRows()
    total.value = rows.value.length
    return
  }
  loading.value = true
  message.value = ''
  try {
    const keyword = filters.keyword.trim()
    const { data } = await userApi.list({
      current: current.value,
      pageSize,
      userName: keyword || undefined,
      userAccount: keyword || undefined,
      userRole: filters.userRole || undefined,
    })
    rows.value = data.data.records
    total.value = data.data.total
  } catch (e) {
    message.value = e instanceof Error ? e.message : '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

function openAdd() {
  Object.assign(form, { id: undefined, userAccount: '', userName: '', roleCodes: ['designer'], userProfile: '' })
  modal.value = 'add'
}

async function openEdit(user: RowUser) {
  let detail: RowUser = user
  if (isAdmin) {
    try {
      const { data } = await userApi.get(user.id)
      detail = data.data
    } catch (e) {
      message.value = e instanceof Error ? e.message : '用户详情加载失败'
      return
    }
  }
  Object.assign(form, {
    id: detail.id,
    userAccount: detail.userAccount,
    userName: detail.userName,
    roleCodes: detail.userRoles?.length ? [...detail.userRoles] : [detail.userRole],
    userProfile: detail.userProfile || '',
  })
  modal.value = 'edit'
}

async function save() {
  message.value = ''
  if (!form.roleCodes.length) { message.value = '用户至少需要一个角色'; return }
  saving.value = true
  try {
    if (modal.value === 'add') {
      const { data } = await userApi.add({ ...form, roleCodes: form.roleCodes })
      message.value = `用户创建成功，ID：${data.data}`
    } else if (form.id) {
      await userApi.update({
        id: form.id,
        userName: form.userName,
        roleCodes: form.roleCodes,
        userProfile: form.userProfile,
      })
      message.value = '用户信息更新成功'
    }
    modal.value = null
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function remove(user: RowUser) {
  if (!window.confirm(`确定删除用户“${user.userName || user.userAccount}”吗？`)) return
  try {
    await userApi.remove(user.id)
    message.value = '用户删除成功'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '删除失败'
  }
}

async function goPage(step: number) {
  const next = current.value + step
  if (next < 1 || next > Math.max(1, Math.ceil(total.value / pageSize))) return
  current.value = next
  await load()
}

onMounted(() => {
  load()
  loadRoleOptions()
})
</script>

<template>
  <div>
    <PageHead eyebrow="ORGANIZATION" title="用户管理" description="维护企业成员、账号状态与基础角色。">
      <button class="btn primary" :disabled="!isAdmin" @click="openAdd"><UserPlus/>新增用户</button>
    </PageHead>

    <div class="filters">
      <div><Search/><input v-model="filters.keyword" placeholder="搜索姓名或账号" @keyup.enter="current=1;load()"></div>
      <select v-model="filters.userRole" @change="current=1;load()">
        <option value="">全部角色</option>
        <option v-for="role in availableRoles" :key="role.id" :value="role.roleCode">{{ role.roleName }}</option>
      </select>
      <button class="ghost" @click="current=1;load()"><Search/>查询</button>
      <span/>
      <small>{{ isAdmin ? `共 ${total} 位用户` : '演示数据 · 管理员登录后读取真实列表' }}</small>
    </div>

    <p v-if="message" class="notice">{{ message }}</p>
    <section class="panel table-scroll">
      <table>
        <thead><tr><th>成员</th><th>账号</th><th>角色</th><th>简介</th><th>状态</th><th>最后登录</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="table-state">正在加载用户数据…</td></tr>
          <tr v-else-if="!rows.length"><td colspan="7" class="table-state">暂无符合条件的用户</td></tr>
          <tr v-for="user in rows" :key="user.id">
            <td><div class="member"><span class="avatar">{{ (user.userName || user.userAccount).slice(-1) }}</span><strong>{{ user.userName || '未设置姓名' }}</strong></div></td>
            <td>{{ user.userAccount }}</td>
            <td><div class="user-role-list"><span v-for="role in (user.userRoles?.length ? user.userRoles : [user.userRole])" :key="role" class="role-tag">{{ roleLabels[role] || role }}</span></div></td>
            <td class="profile-cell">{{ user.userProfile || user.department || '—' }}</td>
            <td><StatusPill :value="user.status || '正常'"/></td>
            <td>{{ user.lastLogin || '—' }}</td>
            <td>
              <button class="text-btn" :disabled="!isAdmin" @click="openEdit(user)">编辑</button>
              <button class="text-btn danger-text" :disabled="!isAdmin" @click="remove(user)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <footer v-if="isAdmin" class="pagination">
        <span>第 {{ current }} / {{ Math.max(1, Math.ceil(total / pageSize)) }} 页</span>
        <button :disabled="current <= 1" @click="goPage(-1)"><ChevronLeft/></button>
        <button :disabled="current >= Math.ceil(total / pageSize)" @click="goPage(1)"><ChevronRight/></button>
      </footer>
    </section>

    <div v-if="modal" class="modal-wrap">
      <div class="modal">
        <header>
          <div><p>{{ modal === 'add' ? 'ADD MEMBER' : 'EDIT MEMBER' }}</p><h2>{{ modal === 'add' ? '新增用户' : '编辑用户' }}</h2></div>
          <button @click="modal=null"><X/></button>
        </header>
        <form @submit.prevent="save">
          <label><span>登录账号</span><input v-model.trim="form.userAccount" required minlength="6" :disabled="modal==='edit'" placeholder="至少 6 位"></label>
          <label><span>用户姓名</span><input v-model.trim="form.userName" required placeholder="请输入姓名"></label>
          <label><span>用户角色（可多选）</span><div class="role-checks"><label v-for="role in availableRoles" :key="role.id"><input v-model="form.roleCodes" type="checkbox" :value="role.roleCode"><span>{{ role.roleName }}</span></label></div></label>
          <label><span>用户简介</span><textarea v-model="form.userProfile" placeholder="部门、岗位或职责说明"/></label>
          <p v-if="modal==='add'">初始密码由后端设置为 12345678，建议首次登录后立即修改。</p>
          <footer><button type="button" class="btn" @click="modal=null">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '正在保存…' : '确认保存' }}</button></footer>
        </form>
      </div>
    </div>
  </div>
</template>
