<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { CheckCircle2, ChevronLeft, ChevronRight, ClipboardCheck, Grid3X3, List, Plus, Search, Trash2, UserPlus, Users, X } from 'lucide-vue-next'
import PageHead from '../components/PageHead.vue'
import StatusPill from '../components/StatusPill.vue'
import { projects as demoProjects } from '../demo'
import {
  projectApi, projectMemberApi, session, userApi,
  type DesignProject, type ProjectAcceptanceRecord, type ProjectFormPayload, type ProjectMember, type ProjectMemberRole,
  type ProjectPriority, type ProjectStatus, type UserVO,
} from '../api'

const statusLabels: Record<ProjectStatus, string> = {
  draft: '草稿', approved: '已立项', designing: '设计中', acceptance: '验收中',
  completed: '已完成', cancelled: '已取消',
}
const priorityLabels: Record<ProjectPriority, string> = { low: '低', medium: '中', high: '高' }
const memberRoleLabels: Record<ProjectMemberRole, string> = {
  manager: '项目经理', designer: '设计师', reviewer: '审核人', viewer: '只读成员',
}
const statusFromDemo: Record<string, ProjectStatus> = {
  草稿: 'draft', 已立项: 'approved', 设计中: 'designing', 验收中: 'acceptance',
  已完成: 'completed', 已取消: 'cancelled',
}
const priorityFromDemo: Record<string, ProjectPriority> = { 低: 'low', 中: 'medium', 高: 'high' }

const rows = ref<DesignProject[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 9
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const mode = ref<'grid' | 'list'>('grid')
const modal = ref<'add' | 'edit' | null>(null)
const managers = ref<UserVO[]>([])
const acceptors = ref<UserVO[]>([])
const historyProject = ref<DesignProject | null>(null)
const acceptanceHistory = ref<ProjectAcceptanceRecord[]>([])
const historyLoading = ref(false)
const memberProject = ref<DesignProject | null>(null)
const memberRows = ref<ProjectMember[]>([])
const candidates = ref<UserVO[]>([])
const memberLoading = ref(false)
const candidateKeyword = ref('')
const memberForm = reactive<{ userId: string; projectRole: ProjectMemberRole }>({ userId: '', projectRole: 'designer' })
const filters = reactive<{ keyword: string; status: '' | ProjectStatus; category: string; priority: '' | ProjectPriority }>({
  keyword: '', status: '', category: '', priority: '',
})
const form = reactive<ProjectFormPayload & { id?: string; version?: number; projectCode: string }>({
  projectCode: '', projectName: '', category: '', season: '', style: '', targetAudience: '',
  requirement: '', managerId: session.user?.id, acceptorId: undefined, priority: 'medium', planStartDate: '',
  planEndDate: '', progress: 0,
})

const isReal = computed(() => !session.demo)
const canCreate = computed(() => isReal.value && (session.hasRole('admin') || session.hasRole('project_manager')))
const canManageMembers = computed(() => {
  if (!isReal.value || !session.user || !memberProject.value || isTerminal(memberProject.value)) return false
  return session.hasRole('admin')
    || memberProject.value.managerId === session.user.id
    || memberRows.value.some(item => item.userId === session.user?.id && item.projectRole === 'manager')
})
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const categories = computed(() => [...new Set(rows.value.map(item => item.category).filter(Boolean))] as string[])

function demoRows(): DesignProject[] {
  return demoProjects.map(item => ({
    id: String(item.id), projectCode: item.code, projectName: item.name, category: item.category,
    season: item.season, managerId: '9001', managerName: item.manager,
    acceptorId: '9003', acceptorName: '业务验收人',
    priority: priorityFromDemo[item.priority] || 'medium',
    status: statusFromDemo[item.status] || 'draft', progress: item.progress, version: 0,
    planEndDate: `2026-${item.due}`,
  }))
}

async function load() {
  if (!isReal.value) {
    const keyword = filters.keyword.trim().toLowerCase()
    rows.value = demoRows().filter(item =>
      (!keyword || `${item.projectName}${item.projectCode}`.toLowerCase().includes(keyword)) &&
      (!filters.status || item.status === filters.status) &&
      (!filters.category || item.category === filters.category) &&
      (!filters.priority || item.priority === filters.priority))
    total.value = rows.value.length
    return
  }
  loading.value = true
  message.value = ''
  try {
    const { data } = await projectApi.list({
      current: current.value, pageSize, keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined, category: filters.category || undefined,
      priority: filters.priority || undefined, sortField: 'updateTime', sortOrder: 'descend',
    })
    rows.value = data.data.records
    total.value = data.data.total
  } catch (error) {
    message.value = error instanceof Error ? error.message : '项目列表加载失败'
  } finally {
    loading.value = false
  }
}

async function loadManagers() {
  if (!isReal.value || !session.user) return
  managers.value = [session.user]
  if (!session.hasRole('admin')) return
  try {
    const { data } = await userApi.list({ current: 1, pageSize: 100, userRole: 'project_manager' })
    managers.value = [session.user, ...data.data.records.filter(item => item.id !== session.user?.id)]
  } catch {
    // 负责人列表加载失败时管理员仍可将自己设为负责人。
  }
}

async function loadAcceptors() {
  if (!isReal.value || (!session.hasRole('admin') && !session.hasRole('project_manager'))) return
  try {
    const { data } = await projectApi.acceptanceCandidates()
    acceptors.value = data.data
  } catch {
    acceptors.value = []
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined, version: undefined, projectCode: '', projectName: '', category: '',
    season: '', style: '', targetAudience: '', requirement: '', managerId: session.user?.id,
    acceptorId: undefined, priority: 'medium', planStartDate: '', planEndDate: '', progress: 0,
  })
}

function openAdd() {
  resetForm()
  modal.value = 'add'
}

async function openEdit(item: DesignProject) {
  if (!isReal.value) return
  message.value = ''
  try {
    const { data } = await projectApi.get(item.id)
    const detail = data.data
    Object.assign(form, {
      id: detail.id, version: detail.version, projectCode: detail.projectCode,
      projectName: detail.projectName, category: detail.category || '', season: detail.season || '',
      style: detail.style || '', targetAudience: detail.targetAudience || '',
      requirement: detail.requirement || '', managerId: detail.managerId, acceptorId: detail.acceptorId,
      priority: detail.priority, planStartDate: detail.planStartDate || '',
      planEndDate: detail.planEndDate || '', progress: detail.progress,
    })
    modal.value = 'edit'
  } catch (error) {
    message.value = error instanceof Error ? error.message : '项目详情加载失败'
  }
}

async function save() {
  saving.value = true
  message.value = ''
  try {
    const payload: ProjectFormPayload = {
      projectName: form.projectName, category: form.category || undefined,
      season: form.season || undefined, style: form.style || undefined,
      targetAudience: form.targetAudience || undefined, requirement: form.requirement || undefined,
      managerId: form.managerId, acceptorId: form.acceptorId, priority: form.priority,
      planStartDate: form.planStartDate || undefined, planEndDate: form.planEndDate || undefined,
      progress: form.progress,
    }
    if (modal.value === 'add') {
      const { data } = await projectApi.add({ ...payload, projectCode: form.projectCode })
      message.value = `项目创建成功，ID：${data.data}`
    } else if (form.id && form.version !== undefined) {
      await projectApi.update({ ...payload, id: form.id, version: form.version })
      message.value = '项目信息更新成功'
    }
    modal.value = null
    await load()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '项目保存失败'
  } finally {
    saving.value = false
  }
}

async function perform(item: DesignProject, action: 'submit' | 'start' | 'acceptance' | 'accept' | 'reject' | 'cancel') {
  if (!isReal.value) return
  let reason = ''
  if (action === 'reject') {
    const input = window.prompt('请输入验收退回原因')
    if (input === null) return
    reason = input.trim()
    if (!reason) { message.value = '验收退回原因不能为空'; return }
  }
  if (action === 'accept') {
    const input = window.prompt('请输入验收意见（可以留空）', '')
    if (input === null) return
    reason = input.trim()
  }
  if (action === 'cancel') {
    const input = window.prompt('请输入取消项目的原因')
    if (input === null) return
    reason = input.trim()
    if (!reason) { message.value = '取消原因不能为空'; return }
  }
  if (!window.confirm(action === 'cancel' ? `确定取消项目“${item.projectName}”吗？` : '确定执行此状态操作吗？')) return
  message.value = ''
  try {
    if (action === 'submit') await projectApi.submit(item.id, item.version)
    if (action === 'start') await projectApi.startDesign(item.id, item.version)
    if (action === 'acceptance') await projectApi.submitAcceptance(item.id, item.version)
    if (action === 'accept') await projectApi.accept(item.id, item.version, reason || undefined, createRequestNo())
    if (action === 'reject') await projectApi.rejectAcceptance(item.id, item.version, reason, createRequestNo())
    if (action === 'cancel') await projectApi.cancel(item.id, item.version, reason)
    message.value = '项目状态已更新'
    await load()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '状态操作失败'
  }
}

async function openAcceptanceHistory(item: DesignProject) {
  if (!isReal.value) return
  historyProject.value = item
  acceptanceHistory.value = []
  historyLoading.value = true
  try {
    const { data } = await projectApi.acceptanceHistory(item.id)
    acceptanceHistory.value = data.data
  } catch (error) {
    message.value = error instanceof Error ? error.message : '验收记录加载失败'
    historyProject.value = null
  } finally {
    historyLoading.value = false
  }
}

function createRequestNo() {
  const value = typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID() : `${Date.now()}_${Math.random().toString(36).slice(2)}`
  return `project_acceptance_${value}`
}

async function query() { current.value = 1; await load() }
async function goPage(step: number) {
  const next = current.value + step
  if (next < 1 || next > pageCount.value) return
  current.value = next
  await load()
}

async function openMembers(item: DesignProject) {
  if (!isReal.value) return
  memberProject.value = item
  memberRows.value = []
  candidates.value = []
  candidateKeyword.value = ''
  Object.assign(memberForm, { userId: '', projectRole: 'designer' })
  await loadMembers()
  if (canManageMembers.value) await loadCandidates()
}

async function loadMembers() {
  if (!memberProject.value) return
  memberLoading.value = true
  try {
    const { data } = await projectMemberApi.list(memberProject.value.id)
    memberRows.value = data.data
  } catch (error) {
    message.value = error instanceof Error ? error.message : '项目成员加载失败'
    memberProject.value = null
  } finally {
    memberLoading.value = false
  }
}

async function loadCandidates() {
  if (!memberProject.value || !canManageMembers.value) return
  try {
    const { data } = await projectMemberApi.candidates(
      memberProject.value.id, candidateKeyword.value.trim() || undefined)
    candidates.value = data.data
    if (!candidates.value.some(item => item.id === memberForm.userId)) memberForm.userId = ''
  } catch (error) {
    message.value = error instanceof Error ? error.message : '候选成员加载失败'
  }
}

async function addMember() {
  if (!memberProject.value || !memberForm.userId) return
  try {
    await projectMemberApi.add(memberProject.value.id, memberForm.userId, memberForm.projectRole)
    message.value = '项目成员添加成功'
    memberForm.userId = ''
    await Promise.all([loadMembers(), loadCandidates()])
  } catch (error) {
    message.value = error instanceof Error ? error.message : '添加项目成员失败'
  }
}

async function updateMemberRole(member: ProjectMember, event: Event) {
  const role = (event.target as HTMLSelectElement).value as ProjectMemberRole
  if (role === member.projectRole) return
  try {
    await projectMemberApi.update(member.id, role)
    message.value = '成员角色已更新'
    await loadMembers()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '更新成员角色失败'
    await loadMembers()
  }
}

async function removeMember(member: ProjectMember) {
  if (!window.confirm(`确定移除成员“${member.userName || member.userAccount}”吗？`)) return
  try {
    await projectMemberApi.remove(member.id)
    message.value = '项目成员已移除'
    await Promise.all([loadMembers(), loadCandidates()])
  } catch (error) {
    message.value = error instanceof Error ? error.message : '移除项目成员失败'
  }
}

function isTerminal(item: DesignProject) { return ['completed', 'cancelled'].includes(item.status) }
function isProjectManager(item: DesignProject) { return item.managerId === session.user?.id }
function isProjectAcceptor(item: DesignProject) { return item.acceptorId === session.user?.id }
function dateText(value?: string) { return value ? value.replace('T', ' ').slice(0, 16) : '—' }
function coverTone(index: number) { return ['clay', 'slate', 'moss', 'sand'][index % 4] }

onMounted(() => { load(); loadManagers(); loadAcceptors() })
</script>

<template>
  <div>
    <PageHead eyebrow="PROJECT PORTFOLIO" title="设计项目" description="从需求立项到完成归档，统一管理项目进度与交付。">
      <button class="btn primary" :disabled="!canCreate" @click="openAdd"><Plus/>新建设计项目</button>
    </PageHead>

    <div class="filters">
      <div><Search/><input v-model="filters.keyword" placeholder="搜索项目名称或编号" @keyup.enter="query"></div>
      <select v-model="filters.status" @change="query">
        <option value="">全部状态</option><option v-for="(label, value) in statusLabels" :key="value" :value="value">{{ label }}</option>
      </select>
      <select v-model="filters.category" @change="query">
        <option value="">全部品类</option><option v-for="item in categories" :key="item">{{ item }}</option>
      </select>
      <select v-model="filters.priority" @change="query">
        <option value="">全部优先级</option><option value="high">高</option><option value="medium">中</option><option value="low">低</option>
      </select>
      <button class="ghost" @click="query"><Search/>查询</button><span/>
      <small>{{ session.demo ? '演示数据' : `共 ${total} 个项目` }}</small>
      <aside><button :class="{active:mode==='grid'}" @click="mode='grid'"><Grid3X3/></button><button :class="{active:mode==='list'}" @click="mode='list'"><List/></button></aside>
    </div>

    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="loading" class="panel project-empty">正在加载项目数据…</p>
    <p v-else-if="!rows.length" class="panel project-empty">暂无符合条件的设计项目</p>

    <section v-else-if="mode==='grid'" class="project-grid">
      <article v-for="(item,index) in rows" :key="item.id">
        <div class="cover" :class="coverTone(index)"><span>{{ item.season || 'SEASON TBD' }}</span><div class="garment"><i/><i/><i/></div><b>{{ item.category || '品类待定' }}</b></div>
        <div class="project-body">
          <header><StatusPill :value="statusLabels[item.status]"/><span class="priority" :class="priorityLabels[item.priority]">{{ priorityLabels[item.priority] }}</span></header>
          <h2>{{ item.projectName }}</h2><p>{{ item.projectCode }} · 负责人 {{ item.managerName || item.managerId }}</p>
          <p>验收人 {{ item.acceptorName || '尚未指定' }}</p>
          <div class="progress-meta"><span>整体进度</span><b>{{ item.progress }}%</b></div><div class="bar"><i :style="{width:`${item.progress}%`}"/></div>
          <footer><span>{{ item.style || '风格待定' }}</span><time>{{ item.planEndDate || '日期待定' }}</time></footer>
          <div v-if="isReal" class="project-card-actions">
            <button class="text-btn" @click="openMembers(item)"><Users/>成员</button>
            <button class="text-btn" @click="openAcceptanceHistory(item)"><ClipboardCheck/>验收记录</button>
            <button v-if="!isTerminal(item) && (isProjectManager(item) || session.hasRole('admin'))" class="text-btn" @click="openEdit(item)">编辑</button>
            <button v-if="item.status==='draft' && isProjectManager(item)" class="text-btn" @click="perform(item,'submit')">提交立项</button>
            <button v-if="item.status==='approved' && isProjectManager(item)" class="text-btn" @click="perform(item,'start')">开始设计</button>
            <button v-if="item.status==='designing' && isProjectManager(item)" class="text-btn" @click="perform(item,'acceptance')">提交验收</button>
            <button v-if="item.status==='acceptance' && isProjectAcceptor(item)" class="text-btn" @click="perform(item,'accept')">通过验收</button>
            <button v-if="item.status==='acceptance' && isProjectAcceptor(item)" class="text-btn danger-text" @click="perform(item,'reject')">退回验收</button>
            <button v-if="!isTerminal(item) && (isProjectManager(item) || session.hasRole('admin'))" class="text-btn danger-text" @click="perform(item,'cancel')">取消</button>
          </div>
        </div>
      </article>
      <button v-if="canCreate" class="new-card" @click="openAdd"><i><Plus/></i><b>创建新项目</b><span>从设计需求开始规划</span></button>
    </section>

    <section v-else-if="rows.length" class="panel table-scroll">
      <table><thead><tr><th>项目</th><th>状态</th><th>品类 / 季节</th><th>负责人 / 验收人</th><th>进度</th><th>计划完成</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in rows" :key="item.id">
          <td><strong>{{ item.projectName }}</strong><small>{{ item.projectCode }}</small></td><td><StatusPill :value="statusLabels[item.status]"/></td>
          <td>{{ item.category || '—' }} · {{ item.season || '—' }}</td><td>{{ item.managerName || item.managerId }} / {{ item.acceptorName || '未指定' }}</td><td>{{ item.progress }}%</td><td>{{ item.planEndDate || '—' }}</td>
          <td class="project-table-actions"><button class="text-btn" :disabled="!isReal" @click="openMembers(item)">成员</button><button class="text-btn" :disabled="!isReal" @click="openAcceptanceHistory(item)">验收记录</button><button v-if="isReal && !isTerminal(item) && (isProjectManager(item) || session.hasRole('admin'))" class="text-btn" @click="openEdit(item)">编辑</button><button v-if="isReal && !isTerminal(item) && (isProjectManager(item) || session.hasRole('admin'))" class="text-btn danger-text" @click="perform(item,'cancel')">取消</button></td>
        </tr></tbody>
      </table>
    </section>

    <footer v-if="isReal && total" class="pagination standalone"><span>第 {{ current }} / {{ pageCount }} 页</span><button :disabled="current<=1" @click="goPage(-1)"><ChevronLeft/></button><button :disabled="current>=pageCount" @click="goPage(1)"><ChevronRight/></button></footer>

    <div v-if="modal" class="modal-wrap" @click.self="modal=null">
      <section class="modal wide"><header><div><p>DESIGN PROJECT</p><h2>{{ modal==='add' ? '新建设计项目' : '编辑设计项目' }}</h2></div><button @click="modal=null"><X/></button></header>
        <form @submit.prevent="save">
          <div class="form-row"><label><span>项目编号 *</span><input v-model.trim="form.projectCode" :disabled="modal==='edit'" required placeholder="如 FW26-W-014"></label><label><span>项目名称 *</span><input v-model.trim="form.projectName" required></label></div>
          <div class="form-row"><label><span>服装品类</span><input v-model.trim="form.category" placeholder="女装 / 男装 / 童装"></label><label><span>企划季节</span><input v-model.trim="form.season" placeholder="2026 秋冬"></label></div>
          <div class="form-row"><label><span>设计风格</span><input v-model.trim="form.style"></label><label><span>目标人群</span><input v-model.trim="form.targetAudience"></label></div>
          <div class="form-row"><label><span>项目负责人 *</span><select v-model="form.managerId" required><option v-for="item in managers" :key="item.id" :value="item.id">{{ item.userName || item.userAccount }}</option></select></label><label><span>项目验收人 *</span><select v-model="form.acceptorId" required><option :value="undefined">请选择验收人</option><option v-for="item in acceptors" :key="item.id" :value="item.id" :disabled="item.id===form.managerId">{{ item.userName || item.userAccount }}</option></select></label></div>
          <div class="form-row"><label><span>优先级</span><select v-model="form.priority"><option value="high">高</option><option value="medium">中</option><option value="low">低</option></select></label><span/></div>
          <div class="form-row"><label><span>计划开始日期</span><input v-model="form.planStartDate" type="date"></label><label><span>计划结束日期</span><input v-model="form.planEndDate" type="date"></label></div>
          <label v-if="modal==='edit'"><span>整体进度（0-100）</span><input v-model.number="form.progress" type="number" min="0" max="100"></label>
          <label><span>设计需求</span><textarea v-model.trim="form.requirement" placeholder="描述设计目标、范围和交付要求"></textarea></label>
          <p>项目负责人和验收人必须由不同账号担任；全部有效任务完成后，项目负责人方可提交验收。</p>
          <footer><button type="button" class="btn" @click="modal=null">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '保存中…' : '保存项目' }}</button></footer>
        </form>
      </section>
    </div>

    <div v-if="historyProject" class="modal-wrap" @click.self="historyProject=null">
      <section class="modal acceptance-modal">
        <header><div><p>PROJECT ACCEPTANCE</p><h2>{{ historyProject.projectName }} · 验收记录</h2></div><button @click="historyProject=null"><X/></button></header>
        <div class="acceptance-history">
          <p v-if="historyLoading" class="member-state">正在加载验收记录…</p>
          <p v-else-if="!acceptanceHistory.length" class="member-state">暂无验收记录</p>
          <article v-for="record in acceptanceHistory" v-else :key="record.id">
            <i :class="record.result"><CheckCircle2 v-if="record.result==='approved'"/><ClipboardCheck v-else/></i>
            <div><strong>{{ record.result === 'approved' ? '验收通过' : '验收退回' }} · {{ record.acceptorName || record.acceptorId }}</strong><p>{{ record.opinion || '未填写验收意见' }}</p><small>项目版本 {{ record.projectVersion }} · {{ dateText(record.createTime) }}</small></div>
          </article>
        </div>
      </section>
    </div>

    <div v-if="memberProject" class="modal-wrap" @click.self="memberProject=null">
      <section class="modal wide member-modal">
        <header><div><p>PROJECT TEAM</p><h2>{{ memberProject.projectName }} · 成员</h2></div><button @click="memberProject=null"><X/></button></header>
        <div class="member-modal-body">
          <section v-if="canManageMembers" class="member-add-box">
            <div class="member-candidate-search"><Search/><input v-model="candidateKeyword" placeholder="搜索候选人的姓名或账号" @keyup.enter="loadCandidates"><button class="text-btn" @click="loadCandidates">查询</button></div>
            <div class="member-add-row">
              <select v-model="memberForm.userId"><option value="">选择候选成员</option><option v-for="item in candidates" :key="item.id" :value="item.id">{{ item.userName || item.userAccount }} · {{ item.userAccount }}</option></select>
              <select v-model="memberForm.projectRole"><option v-for="(label,value) in memberRoleLabels" :key="value" :value="value">{{ label }}</option></select>
              <button class="btn primary" :disabled="!memberForm.userId" @click="addMember"><UserPlus/>添加</button>
            </div>
          </section>
          <p v-if="memberLoading" class="member-state">正在加载成员…</p>
          <p v-else-if="!memberRows.length" class="member-state">当前项目暂无成员</p>
          <div v-else class="member-list">
            <article v-for="member in memberRows" :key="member.id">
              <span class="avatar">{{ (member.userName || member.userAccount || '?').slice(-1) }}</span>
              <div><strong>{{ member.userName || '未设置姓名' }}</strong><small>{{ member.userAccount }} · {{ member.userRole }}</small></div>
              <select v-if="canManageMembers" :value="member.projectRole" @change="updateMemberRole(member,$event)"><option v-for="(label,value) in memberRoleLabels" :key="value" :value="value">{{ label }}</option></select>
              <span v-else class="role-tag">{{ memberRoleLabels[member.projectRole] }}</span>
              <button v-if="canManageMembers && member.userId!==memberProject.managerId" class="member-remove" title="移除成员" @click="removeMember(member)"><Trash2/></button>
            </article>
          </div>
          <p class="member-help">项目负责人不可直接移除；如需移除，请先在项目编辑页变更负责人。</p>
        </div>
      </section>
    </div>
  </div>
</template>
