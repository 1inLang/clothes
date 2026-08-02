<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ChevronLeft, ChevronRight, Columns3, Download, FileText, List, Paperclip, Plus, Search, Trash2, Upload, X } from 'lucide-vue-next'
import PageHead from '../components/PageHead.vue'
import StatusPill from '../components/StatusPill.vue'
import { tasks as demoTasks } from '../demo'
import {
  designFileApi, projectApi, projectMemberApi, session, taskApi,
  type DesignFileRecord, type DesignFileType, type DesignProject, type DesignTask,
  type ProjectMember, type ProjectPriority, type TaskStatus,
} from '../api'

const statusLabels: Record<TaskStatus, string> = {
  unassigned: '待分配', pending_acceptance: '待领取', in_progress: '进行中',
  pending_review: '待审核', revision: '退回修改', completed: '已完成', cancelled: '已取消',
}
const statusFromDemo: Record<string, TaskStatus> = {
  待分配: 'unassigned', 待领取: 'pending_acceptance', 进行中: 'in_progress',
  待审核: 'pending_review', 退回修改: 'revision', 已完成: 'completed', 已取消: 'cancelled',
}
const priorityLabels: Record<ProjectPriority, string> = { high: '高', medium: '中', low: '低' }
const priorityFromDemo: Record<string, ProjectPriority> = { 高: 'high', 中: 'medium', 低: 'low' }
const columns: { label: string; statuses: TaskStatus[] }[] = [
  { label: '待分配', statuses: ['unassigned'] },
  { label: '待领取', statuses: ['pending_acceptance'] },
  { label: '进行中', statuses: ['in_progress', 'revision'] },
  { label: '待审核', statuses: ['pending_review'] },
  { label: '已完成', statuses: ['completed'] },
]

const rows = ref<DesignTask[]>([])
const projects = ref<DesignProject[]>([])
const members = ref<ProjectMember[]>([])
const fileTask = ref<DesignTask | null>(null)
const fileRows = ref<DesignFileRecord[]>([])
const selectedFile = ref<File | null>(null)
const uploadInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const fileForm = reactive<{ fileType: DesignFileType; versionNote: string }>({ fileType: 'design', versionNote: '' })
const total = ref(0)
const current = ref(1)
const pageSize = 50
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const mode = ref<'board' | 'list'>('board')
const mine = ref(false)
const modal = ref<'add' | 'edit' | 'assign' | null>(null)
const filters = reactive<{ keyword: string; projectId: string; status: '' | TaskStatus; priority: '' | ProjectPriority }>({
  keyword: '', projectId: '', status: '', priority: '',
})
const form = reactive<{
  id?: string; version?: number; projectId: string; taskCode: string; taskName: string;
  requirement: string; priority: ProjectPriority; deadline: string; assigneeId: string; reviewerId: string;
}>({ projectId: '', taskCode: '', taskName: '', requirement: '', priority: 'medium', deadline: '', assigneeId: '', reviewerId: '' })

const isReal = computed(() => !session.demo)
const canManage = computed(() => isReal.value && (session.hasRole('admin') || session.hasRole('project_manager')))
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const editableProjects = computed(() => projects.value.filter(item => ['approved', 'designing'].includes(item.status)))
const assignees = computed(() => members.value.filter(item => ['designer', 'manager'].includes(item.projectRole)))
const reviewers = computed(() => members.value.filter(item => ['reviewer', 'manager'].includes(item.projectRole)))
const canUploadCurrentFile = computed(() => {
  if (!isReal.value || !fileTask.value) return false
  if (fileForm.fileType !== 'design') return true
  return ['in_progress', 'revision'].includes(fileTask.value.status)
    && (isAssignee(fileTask.value) || canManage.value)
})

function demoRows(): DesignTask[] {
  return demoTasks.map(item => ({
    id: String(item.id), projectId: String(item.id < 104 ? 1 : 2), projectName: item.project,
    taskCode: item.code, taskName: item.name, assigneeId: '9002', assigneeName: item.assignee,
    reviewerId: '9003', reviewerName: item.reviewer, priority: priorityFromDemo[item.priority] || 'medium',
    status: statusFromDemo[item.status] || 'unassigned', deadline: item.due,
    progress: item.progress, version: 0, rejectionReason: item.status === '退回修改' ? '请调整袖口比例与面料标注' : undefined,
  }))
}

async function loadProjects() {
  if (!isReal.value) return
  try {
    const { data } = await projectApi.list({ current: 1, pageSize: 100, sortField: 'updateTime', sortOrder: 'descend' })
    projects.value = data.data.records
  } catch (error) {
    message.value = error instanceof Error ? error.message : '项目选项加载失败'
  }
}

async function load() {
  if (!isReal.value) {
    const keyword = filters.keyword.trim().toLowerCase()
    rows.value = demoRows().filter(item =>
      (!keyword || `${item.taskName}${item.taskCode}`.toLowerCase().includes(keyword)) &&
      (!filters.status || item.status === filters.status) &&
      (!filters.priority || item.priority === filters.priority))
    total.value = rows.value.length
    return
  }
  loading.value = true
  message.value = ''
  try {
    const request = {
      current: current.value, pageSize, keyword: filters.keyword.trim() || undefined,
      projectId: filters.projectId || undefined, status: filters.status || undefined,
      priority: filters.priority || undefined, sortField: 'updateTime', sortOrder: 'descend',
    }
    const { data } = mine.value ? await taskApi.mine(request) : await taskApi.list(request)
    rows.value = data.data.records
    total.value = data.data.total
  } catch (error) {
    message.value = error instanceof Error ? error.message : '任务列表加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { id: undefined, version: undefined, projectId: editableProjects.value[0]?.id || '',
    taskCode: '', taskName: '', requirement: '', priority: 'medium', deadline: '', assigneeId: '', reviewerId: '' })
}

function openAdd(projectId?: string) {
  resetForm()
  if (projectId && editableProjects.value.some(item => item.id === projectId)) form.projectId = projectId
  modal.value = 'add'
}

async function openEdit(item: DesignTask) {
  try {
    const { data } = await taskApi.get(item.id)
    const detail = data.data
    Object.assign(form, { id: detail.id, version: detail.version, projectId: detail.projectId,
      taskCode: detail.taskCode, taskName: detail.taskName, requirement: detail.requirement || '',
      priority: detail.priority, deadline: toDateInput(detail.deadline), assigneeId: '', reviewerId: '' })
    modal.value = 'edit'
  } catch (error) { message.value = error instanceof Error ? error.message : '任务详情加载失败' }
}

async function openAssign(item: DesignTask) {
  members.value = []
  try {
    const [{ data: detailData }, { data: memberData }] = await Promise.all([
      taskApi.get(item.id), projectMemberApi.list(item.projectId),
    ])
    const detail = detailData.data
    members.value = memberData.data
    Object.assign(form, { id: detail.id, version: detail.version, projectId: detail.projectId,
      taskCode: detail.taskCode, taskName: detail.taskName, assigneeId: detail.assigneeId || '',
      reviewerId: detail.reviewerId || '' })
    modal.value = 'assign'
  } catch (error) { message.value = error instanceof Error ? error.message : '任务分派信息加载失败' }
}

async function save() {
  saving.value = true
  message.value = ''
  try {
    if (modal.value === 'add') {
      const { data } = await taskApi.add({ projectId: form.projectId, taskCode: form.taskCode,
        taskName: form.taskName, requirement: form.requirement || undefined,
        priority: form.priority, deadline: normalizeDeadline(form.deadline) })
      message.value = `任务创建成功，ID：${data.data}`
    } else if (modal.value === 'edit' && form.id && form.version !== undefined) {
      await taskApi.update({ id: form.id, version: form.version, taskName: form.taskName,
        requirement: form.requirement || undefined, priority: form.priority,
        deadline: normalizeDeadline(form.deadline) })
      message.value = '任务信息更新成功'
    } else if (modal.value === 'assign' && form.id && form.version !== undefined) {
      if (!form.assigneeId || !form.reviewerId) throw new Error('请选择负责人和审核人')
      await taskApi.assign(form.id, form.version, form.assigneeId, form.reviewerId)
      message.value = '任务分派成功'
    }
    modal.value = null
    await load()
  } catch (error) { message.value = error instanceof Error ? error.message : '任务保存失败' }
  finally { saving.value = false }
}

async function perform(item: DesignTask, action: 'accept' | 'progress' | 'submit' | 'cancel') {
  try {
    if (action === 'accept') {
      if (!window.confirm(`确定领取任务“${item.taskName}”吗？`)) return
      await taskApi.accept(item.id, item.version)
    }
    if (action === 'progress') {
      const input = window.prompt('请输入任务进度（0-100）', String(item.progress))
      if (input === null) return
      const progress = Number(input)
      if (!Number.isInteger(progress) || progress < 0 || progress > 100) throw new Error('进度必须是 0 到 100 的整数')
      await taskApi.updateProgress(item.id, item.version, progress)
    }
    if (action === 'submit') {
      const note = window.prompt('填写本次提交说明（可选）')
      if (note === null) return
      await taskApi.submitReview(item.id, item.version, note.trim() || undefined)
    }
    if (action === 'cancel') {
      const reason = window.prompt('请输入取消任务的原因')
      if (reason === null) return
      if (!reason.trim()) throw new Error('取消原因不能为空')
      if (!window.confirm(`确定取消任务“${item.taskName}”吗？`)) return
      await taskApi.cancel(item.id, item.version, reason.trim())
    }
    message.value = '任务状态已更新'
    await load()
  } catch (error) { message.value = error instanceof Error ? error.message : '任务操作失败' }
}

async function openFiles(item: DesignTask) {
  fileTask.value = item
  selectedFile.value = null
  fileForm.fileType = 'design'
  fileForm.versionNote = ''
  await loadFiles()
}

async function loadFiles() {
  if (!fileTask.value) return
  try {
    const { data } = await designFileApi.list({ projectId: fileTask.value.projectId, taskId: fileTask.value.id })
    fileRows.value = data.data
  } catch (error) {
    message.value = error instanceof Error ? error.message : '文件版本加载失败'
    fileTask.value = null
  }
}

function chooseFile(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0] || null
}

async function uploadFile() {
  if (!fileTask.value || !selectedFile.value) return
  uploading.value = true
  try {
    await designFileApi.upload({ file: selectedFile.value, projectId: fileTask.value.projectId,
      taskId: fileTask.value.id, fileType: fileForm.fileType,
      versionNote: fileForm.versionNote.trim() || undefined })
    message.value = '文件新版本上传成功'
    selectedFile.value = null
    fileForm.versionNote = ''
    if (uploadInput.value) uploadInput.value.value = ''
    await loadFiles()
  } catch (error) { message.value = error instanceof Error ? error.message : '文件上传失败' }
  finally { uploading.value = false }
}

async function downloadFile(file: DesignFileRecord) {
  try {
    const { data } = await designFileApi.download(file.id)
    const url = URL.createObjectURL(data)
    const anchor = document.createElement('a')
    anchor.href = url; anchor.download = file.fileName; anchor.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (error) { message.value = error instanceof Error ? error.message : '文件下载失败' }
}

async function removeFile(file: DesignFileRecord) {
  if (!window.confirm(`确定删除文件版本 V${file.versionNo}“${file.fileName}”吗？`)) return
  try {
    await designFileApi.remove(file.id)
    message.value = '文件版本已删除'
    await loadFiles()
  } catch (error) { message.value = error instanceof Error ? error.message : '删除文件失败' }
}

function fileSize(value: number) {
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

function inColumn(statuses: TaskStatus[]) { return rows.value.filter(item => statuses.includes(item.status)) }
function isAssignee(item: DesignTask) { return item.assigneeId === session.user?.id }
function isTerminal(item: DesignTask) { return ['completed', 'cancelled'].includes(item.status) }
function toDateInput(value?: string) { return value ? value.slice(0, 16) : '' }
function normalizeDeadline(value: string) { return value ? `${value}:00` : undefined }
function deadlineText(value?: string) { return value ? value.replace('T', ' ').slice(0, 16) : '未设置' }
async function query() { current.value = 1; await load() }
async function goPage(step: number) { const next = current.value + step; if (next < 1 || next > pageCount.value) return; current.value = next; await load() }

onMounted(() => { loadProjects(); load() })
</script>

<template>
  <div>
    <PageHead eyebrow="TASK FLOW" title="设计任务" description="跟踪任务分派、领取、执行与提交审核的完整流转。">
      <button class="btn primary" :disabled="!canManage || !editableProjects.length" @click="openAdd()"><Plus/>新建设计任务</button>
    </PageHead>
    <div class="filters">
      <div><Search/><input v-model="filters.keyword" placeholder="搜索任务名称或编号" @keyup.enter="query"></div>
      <select v-model="filters.projectId" @change="query"><option value="">全部项目</option><option v-for="item in projects" :key="item.id" :value="item.id">{{ item.projectName }}</option></select>
      <select v-model="filters.status" @change="query"><option value="">全部状态</option><option v-for="(label,value) in statusLabels" :key="value" :value="value">{{ label }}</option></select>
      <select v-model="filters.priority" @change="query"><option value="">全部优先级</option><option value="high">高</option><option value="medium">中</option><option value="low">低</option></select>
      <button class="ghost" :class="{active:mine}" @click="mine=!mine;query()">{{ mine ? '我的相关任务' : '全部可见任务' }}</button><span/>
      <small>{{ session.demo ? '演示数据' : `共 ${total} 项任务` }}</small>
      <aside><button :class="{active:mode==='board'}" @click="mode='board'"><Columns3/></button><button :class="{active:mode==='list'}" @click="mode='list'"><List/></button></aside>
    </div>
    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="loading" class="panel project-empty">正在加载任务数据…</p>
    <p v-else-if="!rows.length" class="panel project-empty">暂无符合条件的设计任务</p>

    <section v-else-if="mode==='board'" class="task-board task-board-live">
      <div v-for="column in columns" :key="column.label" class="board-col">
        <header><span><i :class="column.label"/>{{ column.label }}</span><b>{{ inColumn(column.statuses).length }}</b></header>
        <article v-for="item in inColumn(column.statuses)" :key="item.id">
          <div><span class="priority" :class="priorityLabels[item.priority]">{{ priorityLabels[item.priority] }}优先级</span><small>{{ item.taskCode }}</small></div>
          <h2>{{ item.taskName }}</h2><p>{{ item.projectName || item.projectCode }}</p>
          <div class="bar"><i :style="{width:`${item.progress}%`}"/></div>
          <footer><span class="avatar tiny">{{ (item.assigneeName || '?').slice(-1) }}</span><span>{{ item.assigneeName || '待分配' }}</span><time>{{ deadlineText(item.deadline) }}</time></footer>
          <aside v-if="item.status==='revision'">审核意见：{{ item.rejectionReason || '请按审核意见修改' }}</aside>
          <div v-if="isReal" class="task-actions">
            <button @click="openFiles(item)"><Paperclip/>设计稿</button>
            <button v-if="canManage && !['pending_review','completed','cancelled'].includes(item.status)" @click="openEdit(item)">编辑</button>
            <button v-if="canManage && ['unassigned','pending_acceptance'].includes(item.status)" @click="openAssign(item)">分派</button>
            <button v-if="isAssignee(item) && item.status==='pending_acceptance'" @click="perform(item,'accept')">领取</button>
            <button v-if="isAssignee(item) && ['in_progress','revision'].includes(item.status)" @click="perform(item,'progress')">进度</button>
            <button v-if="isAssignee(item) && ['in_progress','revision'].includes(item.status)" @click="perform(item,'submit')">提交审核</button>
            <button v-if="canManage && !isTerminal(item)" class="danger-text" @click="perform(item,'cancel')">取消</button>
          </div>
        </article>
        <button v-if="canManage && column.statuses.includes('unassigned')" class="add-task" @click="openAdd(filters.projectId || undefined)"><Plus/>添加任务</button>
      </div>
    </section>

    <section v-else-if="rows.length" class="panel table-scroll">
      <table><thead><tr><th>任务</th><th>所属项目</th><th>负责人 / 审核人</th><th>状态</th><th>优先级</th><th>进度</th><th>截止时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in rows" :key="item.id"><td><strong>{{ item.taskName }}</strong><small>{{ item.taskCode }}</small></td><td>{{ item.projectName || item.projectCode }}</td><td>{{ item.assigneeName || '待分配' }} / {{ item.reviewerName || '待配置' }}</td><td><StatusPill :value="statusLabels[item.status]"/></td><td><span class="priority" :class="priorityLabels[item.priority]">{{ priorityLabels[item.priority] }}</span></td><td>{{ item.progress }}%</td><td>{{ deadlineText(item.deadline) }}</td><td><button v-if="isReal" class="text-btn" @click="openFiles(item)">设计稿</button><button v-if="canManage && !isTerminal(item)" class="text-btn" @click="openEdit(item)">编辑</button><button v-if="canManage && ['unassigned','pending_acceptance'].includes(item.status)" class="text-btn" @click="openAssign(item)">分派</button><button v-if="isAssignee(item) && item.status==='pending_acceptance'" class="text-btn" @click="perform(item,'accept')">领取</button></td></tr></tbody>
      </table>
    </section>
    <footer v-if="isReal && total" class="pagination standalone"><span>第 {{ current }} / {{ pageCount }} 页</span><button :disabled="current<=1" @click="goPage(-1)"><ChevronLeft/></button><button :disabled="current>=pageCount" @click="goPage(1)"><ChevronRight/></button></footer>

    <div v-if="modal" class="modal-wrap" @click.self="modal=null">
      <section class="modal wide"><header><div><p>DESIGN TASK</p><h2>{{ modal==='add' ? '新建设计任务' : modal==='edit' ? '编辑设计任务' : '分派设计任务' }}</h2></div><button @click="modal=null"><X/></button></header>
        <form @submit.prevent="save">
          <template v-if="modal!=='assign'">
            <div class="form-row"><label><span>所属项目 *</span><select v-model="form.projectId" :disabled="modal==='edit'" required><option value="">请选择项目</option><option v-for="item in editableProjects" :key="item.id" :value="item.id">{{ item.projectName }}</option></select></label><label><span>任务编号 *</span><input v-model.trim="form.taskCode" :disabled="modal==='edit'" required placeholder="如 TSK-260801-001"></label></div>
            <label><span>任务名称 *</span><input v-model.trim="form.taskName" required></label>
            <div class="form-row"><label><span>优先级</span><select v-model="form.priority"><option value="high">高</option><option value="medium">中</option><option value="low">低</option></select></label><label><span>截止时间</span><input v-model="form.deadline" type="datetime-local"></label></div>
            <label><span>任务要求</span><textarea v-model.trim="form.requirement" placeholder="描述设计范围、交付标准与注意事项"></textarea></label>
          </template>
          <template v-else>
            <p class="assign-title">{{ form.taskCode }} · {{ form.taskName }}</p>
            <div class="form-row"><label><span>任务负责人 *</span><select v-model="form.assigneeId" required><option value="">请选择设计师</option><option v-for="item in assignees" :key="item.id" :value="item.userId">{{ item.userName || item.userAccount }} · {{ item.projectRole }}</option></select></label><label><span>任务审核人 *</span><select v-model="form.reviewerId" required><option value="">请选择审核人</option><option v-for="item in reviewers" :key="item.id" :value="item.userId">{{ item.userName || item.userAccount }} · {{ item.projectRole }}</option></select></label></div>
            <p>负责人和审核人不能是同一人；重新分派后任务回到“待领取”。</p>
          </template>
          <footer><button type="button" class="btn" @click="modal=null">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '保存中…' : modal==='assign' ? '确认分派' : '保存任务' }}</button></footer>
        </form>
      </section>
    </div>

    <div v-if="fileTask" class="modal-wrap" @click.self="fileTask=null">
      <section class="modal wide file-modal">
        <header><div><p>DESIGN VERSIONS</p><h2>{{ fileTask.taskName }} · 文件版本</h2></div><button @click="fileTask=null"><X/></button></header>
        <div class="file-modal-body">
          <section class="file-upload-box">
            <div class="form-row">
              <label><span>文件类型</span><select v-model="fileForm.fileType"><option value="design">设计稿</option><option value="reference">参考资料</option><option value="attachment">其他附件</option></select></label>
              <label><span>选择文件（最大 50MB）</span><input ref="uploadInput" type="file" accept=".jpg,.jpeg,.png,.webp,.pdf,.ai,.psd,.sketch,.zip,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt" @change="chooseFile"></label>
            </div>
            <label><span>版本说明</span><input v-model.trim="fileForm.versionNote" placeholder="本版本修改内容或附件用途"></label>
            <div class="file-upload-footer"><small v-if="selectedFile">已选择：{{ selectedFile.name }} · {{ fileSize(selectedFile.size) }}</small><small v-else>设计稿上传后会自动生成递增版本号。</small><button class="btn primary" :disabled="!selectedFile || uploading || !canUploadCurrentFile" @click="uploadFile"><Upload/>{{ uploading ? '上传中…' : '上传新版本' }}</button></div>
            <p v-if="fileForm.fileType==='design' && !canUploadCurrentFile" class="file-warning">只有进行中或退回修改任务的负责人、项目经理可以上传设计稿。</p>
          </section>
          <div v-if="!fileRows.length" class="member-state">暂无设计稿或附件版本</div>
          <div v-else class="file-version-list">
            <article v-for="file in fileRows" :key="file.id">
              <i><FileText/></i><div><header><strong>V{{ file.versionNo }} · {{ file.fileName }}</strong><span v-if="file.submitted">审核版本</span></header><p>{{ file.versionNote || '无版本说明' }}</p><small>{{ file.uploaderName || file.uploaderId }} · {{ fileSize(file.fileSize) }} · {{ file.createTime?.replace('T',' ').slice(0,16) }}</small></div>
              <em>{{ file.fileType==='design' ? '设计稿' : file.fileType==='reference' ? '参考资料' : '附件' }}</em>
              <button title="下载" @click="downloadFile(file)"><Download/></button><button v-if="!file.submitted" class="danger" title="删除" @click="removeFile(file)"><Trash2/></button>
            </article>
          </div>
          <p class="member-help">已提交审核的版本不可删除；删除采用逻辑删除并保留物理文件用于审计恢复。</p>
        </div>
      </section>
    </div>
  </div>
</template>
