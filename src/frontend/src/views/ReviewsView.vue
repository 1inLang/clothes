<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Check, ChevronRight, Clock3, Download, FileText, RotateCcw, Search } from 'lucide-vue-next'
import PageHead from '../components/PageHead.vue'
import { reviews as demoReviews } from '../demo'
import {
  designFileApi, reviewApi, session,
  type DesignTask, type ReviewDetail,
} from '../api'

const rows = ref<DesignTask[]>([])
const selectedId = ref('')
const detail = ref<ReviewDetail | null>(null)
const keyword = ref('')
const opinion = ref('')
const message = ref('')
const loading = ref(false)
const deciding = ref(false)
const previewUrl = ref('')

function demoRows(): DesignTask[] {
  return demoReviews.map(item => ({
    id: String(item.id), projectId: '1', projectName: item.project,
    taskCode: `REV-DEMO-${item.id}`, taskName: item.task,
    assigneeId: '9002', assigneeName: item.designer, reviewerId: session.user?.id,
    reviewerName: session.user?.userName, priority: item.priority === '高' ? 'high' : 'medium',
    status: 'pending_review', progress: 100, version: 1, updateTime: '2026-08-01T10:26:00',
    submittedFileId: String(item.id),
  }))
}

async function loadPending() {
  loading.value = true
  message.value = ''
  try {
    if (session.demo) {
      const text = keyword.value.trim()
      rows.value = demoRows().filter(item => !text || `${item.taskName}${item.taskCode}`.includes(text))
    } else {
      const { data } = await reviewApi.pending({ current: 1, pageSize: 50,
        keyword: keyword.value.trim() || undefined, sortField: 'updateTime', sortOrder: 'ascend' })
      rows.value = data.data.records
    }
    const next = rows.value.some(item => item.id === selectedId.value) ? selectedId.value : rows.value[0]?.id || ''
    if (next) await selectTask(next)
    else { selectedId.value = ''; detail.value = null; clearPreview() }
  } catch (error) { message.value = error instanceof Error ? error.message : '待审核任务加载失败' }
  finally { loading.value = false }
}

async function selectTask(taskId: string) {
  selectedId.value = taskId
  opinion.value = ''
  clearPreview()
  if (session.demo) {
    const task = rows.value.find(item => item.id === taskId)!
    detail.value = { task, submittedFile: { id: taskId, projectId: task.projectId,
      taskId, fileName: `${task.taskName}.pdf`, fileType: 'design', mimeType: 'application/pdf',
      fileSize: 2480000, versionNo: Number(taskId), uploaderId: task.assigneeId || '9002',
      uploaderName: task.assigneeName, submitted: true }, history: [] }
    return
  }
  try {
    const { data } = await reviewApi.detail(taskId)
    detail.value = data.data
    if (detail.value.submittedFile?.mimeType.startsWith('image/')) {
      const response = await designFileApi.download(detail.value.submittedFile.id)
      previewUrl.value = URL.createObjectURL(response.data)
    }
  } catch (error) { message.value = error instanceof Error ? error.message : '审核详情加载失败' }
}

async function decide(result: 'approved' | 'rejected') {
  if (!detail.value?.submittedFile) { message.value = '当前任务没有有效审核版本'; return }
  if (result === 'rejected' && !opinion.value.trim()) { message.value = '退回修改时必须填写审核意见'; return }
  if (session.demo) { message.value = result === 'approved' ? '演示：审核已通过' : '演示：已退回设计师修改'; return }
  if (!window.confirm(result === 'approved' ? '确定审核通过该设计稿吗？' : '确定退回该设计稿修改吗？')) return
  deciding.value = true
  try {
    const payload = { taskId: detail.value.task.id, taskVersion: detail.value.task.version,
      versionNo: detail.value.submittedFile.versionNo, opinion: opinion.value.trim() || undefined,
      requestNo: createRequestNo() }
    if (result === 'approved') await reviewApi.approve(payload)
    else await reviewApi.reject(payload)
    message.value = result === 'approved' ? '审核已通过，任务已完成' : '已退回修改，任务已回到设计师工作台'
    await loadPending()
  } catch (error) { message.value = error instanceof Error ? error.message : '审核操作失败' }
  finally { deciding.value = false }
}

async function downloadSubmitted() {
  const file = detail.value?.submittedFile
  if (!file || session.demo) return
  try {
    const { data } = await designFileApi.download(file.id)
    const url = URL.createObjectURL(data)
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = file.fileName; anchor.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (error) { message.value = error instanceof Error ? error.message : '设计稿下载失败' }
}

function createRequestNo() {
  const value = typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : `${Date.now()}_${Math.random().toString(36).slice(2)}`
  return `review_${value}`
}
function clearPreview() { if (previewUrl.value) URL.revokeObjectURL(previewUrl.value); previewUrl.value = '' }
function priorityLabel(value: string) { return value === 'high' ? '高' : value === 'low' ? '低' : '中' }
function fileSize(value: number) { return value < 1048576 ? `${(value / 1024).toFixed(1)} KB` : `${(value / 1048576).toFixed(1)} MB` }
function dateText(value?: string) { return value ? value.replace('T', ' ').slice(0, 16) : '—' }

onMounted(loadPending)
onBeforeUnmount(clearPreview)
</script>

<template>
  <div>
    <PageHead eyebrow="REVIEW CENTER" title="审核中心" description="查看已锁定的设计稿版本，完成通过或退回并保留审核记录。">
      <span class="review-wait"><Clock3/>{{ rows.length }} 项等待处理</span>
    </PageHead>
    <p v-if="message" class="notice">{{ message }}</p>
    <section class="review-layout">
      <aside class="panel review-list">
        <div class="review-search"><Search/><input v-model="keyword" placeholder="搜索待审核任务" @keyup.enter="loadPending"></div>
        <p v-if="loading" class="review-empty">正在加载待审核任务…</p>
        <p v-else-if="!rows.length" class="review-empty">当前没有等待你审核的任务</p>
        <button v-for="task in rows" :key="task.id" :class="{active:selectedId===task.id}" @click="selectTask(task.id)">
          <div><span class="priority" :class="priorityLabel(task.priority)">{{ priorityLabel(task.priority) }}优先级</span><time>{{ dateText(task.updateTime) }}</time></div>
          <strong>{{ task.taskName }}</strong><small>{{ task.projectName }} · {{ task.assigneeName || '未设置设计师' }}</small>
          <footer><b>待审核</b><span>{{ task.taskCode }}</span><ChevronRight/></footer>
        </button>
      </aside>

      <main v-if="detail" class="review-main">
        <article class="panel review-title"><div><span>{{ detail.task.projectName }} · {{ detail.task.taskCode }}</span><h2>{{ detail.task.taskName }}</h2><p>设计师 {{ detail.task.assigneeName }} 于 {{ dateText(detail.task.updateTime) }} 提交</p></div><button v-if="detail.submittedFile" class="btn" @click="downloadSubmitted"><Download/>下载原文件</button></article>

        <article class="panel compare review-file-panel">
          <header><span>当前审核版本 <b>V{{ detail.submittedFile?.versionNo || '—' }}</b></span><aside>{{ detail.submittedFile?.mimeType }}</aside></header>
          <div v-if="previewUrl" class="review-image"><img :src="previewUrl" :alt="detail.submittedFile?.fileName"></div>
          <div v-else class="review-file-card"><i><FileText/></i><div><strong>{{ detail.submittedFile?.fileName || '未找到设计稿' }}</strong><p>{{ detail.submittedFile?.versionNote || '本次提交未填写版本说明' }}</p><small v-if="detail.submittedFile">{{ fileSize(detail.submittedFile.fileSize) }} · {{ detail.submittedFile.uploaderName }} · {{ dateText(detail.submittedFile.createTime) }}</small></div></div>
        </article>

        <article class="panel decision"><header><div><h2>审核意见</h2><p>退回修改时必须填写具体意见；审核结果不可覆盖</p></div></header><div><textarea v-model="opinion" maxlength="1000" placeholder="请说明本次审核意见，建议指出具体部位与修改方向…"/><p class="quick"><span>快捷意见</span><button @click="opinion='请补充面料与工艺标注。'">补充标注</button><button @click="opinion='请调整版型比例后重新提交。'">调整比例</button><button @click="opinion='色彩方案与企划方向一致。'">色彩符合</button></p><footer><span/><button class="btn danger" :disabled="deciding" @click="decide('rejected')"><RotateCcw/>退回修改</button><button class="btn approve" :disabled="deciding" @click="decide('approved')"><Check/>审核通过</button></footer></div></article>

        <article class="panel review-history"><header><div><h2>历史审核记录</h2><p>共 {{ detail.history.length }} 次审核</p></div></header><div v-if="!detail.history.length" class="review-empty">暂无历史审核记录</div><div v-else><article v-for="record in detail.history" :key="record.id"><span :class="record.result">{{ record.result==='approved' ? '通过' : '退回' }}</span><div><strong>V{{ record.versionNo }} · {{ record.reviewerName }}</strong><p>{{ record.opinion || '无审核意见' }}</p></div><time>{{ dateText(record.createTime) }}</time></article></div></article>
      </main>
      <main v-else class="panel review-no-selection"><FileText/><h2>暂无待审核任务</h2><p>任务提交有效设计稿后会出现在这里。</p></main>
    </section>
  </div>
</template>
