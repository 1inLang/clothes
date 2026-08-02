<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, CalendarDays, CheckCircle2, Clock3, FileWarning, Plus, Sparkles } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import PageHead from '../components/PageHead.vue'
import StatusPill from '../components/StatusPill.vue'
import { projects as demoProjects, tasks as demoTasks } from '../demo'
import {
  dashboardApi, session,
  type DashboardActivity, type DashboardProjectStatus, type DashboardSummary,
  type DashboardTaskCompletion, type DashboardTodo, type ProjectStatus,
} from '../api'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const summary = ref<DashboardSummary>({ myTodoCount: 0, pendingReviewCount: 0,
  pendingAcceptanceCount: 0, dueSoonCount: 0, overdueCount: 0 })
const todos = ref<DashboardTodo[]>([])
const projectStatus = ref<DashboardProjectStatus>({ distribution: [], projects: [] })
const completion = ref<DashboardTaskCompletion>({ total: 0, completed: 0,
  inProgress: 0, overdue: 0, completionRate: 0 })
const activities = ref<DashboardActivity[]>([])

const projectStatusLabels: Record<ProjectStatus, string> = {
  draft: '草稿', approved: '已立项', designing: '设计中', acceptance: '验收中',
  completed: '已完成', cancelled: '已取消',
}
const taskStatusLabels: Record<string, string> = {
  pending_acceptance: '待领取', in_progress: '进行中', revision: '退回修改',
  pending_review: '待审核', 待审核: '待审核', 验收中: '验收中',
}
const priorityLabels = { high: '高', medium: '中', low: '低' }
const today = computed(() => new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short',
}).format(new Date()))
const greeting = computed(() => {
  const hour = new Date().getHours()
  return hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'
})
const maxProjectCount = computed(() => Math.max(1,
  ...projectStatus.value.distribution.map(item => item.count)))
const canCreate = computed(() => session.hasRole('admin') || session.hasRole('project_manager'))

async function load() {
  if (session.demo) { loadDemo(); return }
  loading.value = true
  message.value = ''
  try {
    const [summaryRes, todosRes, projectsRes, completionRes, activitiesRes] = await Promise.all([
      dashboardApi.summary(), dashboardApi.todos(), dashboardApi.projectStatus(),
      dashboardApi.taskCompletion(), dashboardApi.activities(),
    ])
    summary.value = summaryRes.data.data
    todos.value = todosRes.data.data
    projectStatus.value = projectsRes.data.data
    completion.value = completionRes.data.data
    activities.value = activitiesRes.data.data
  } catch (error) {
    message.value = error instanceof Error ? error.message : '工作台数据加载失败'
  } finally { loading.value = false }
}

function loadDemo() {
  summary.value = { myTodoCount: 8, pendingReviewCount: 3, pendingAcceptanceCount: 1,
    dueSoonCount: 5, overdueCount: 2 }
  todos.value = demoTasks.slice(0, 4).map(item => ({ id: String(item.id), businessType: 'task',
    title: item.name, subtitle: `${item.project} · ${item.code}`, status: item.status,
    priority: item.priority === '高' ? 'high' : item.priority === '低' ? 'low' : 'medium',
    overdue: item.due.includes('逾期'), route: '/tasks' }))
  projectStatus.value = {
    distribution: [{ status: 'designing', count: 2 }, { status: 'acceptance', count: 1 },
      { status: 'completed', count: 1 }],
    projects: demoProjects.slice(0, 3).map(item => ({ id: String(item.id), projectCode: item.code,
      projectName: item.name, category: item.category, status: item.status === '验收中' ? 'acceptance' : 'designing',
      progress: item.progress, totalTasks: item.total, completedTasks: item.done })),
  }
  completion.value = { total: 30, completed: 21, inProgress: 9, overdue: 2, completionRate: 70 }
  activities.value = []
}

function dueText(item: DashboardTodo) {
  if (item.overdue) return '已逾期'
  if (!item.deadline) return '无期限'
  const deadline = new Date(item.deadline)
  const days = Math.ceil((deadline.getTime() - Date.now()) / 86400000)
  if (days <= 0) return '今天'
  if (days === 1) return '明天'
  if (days <= 3) return `${days} 天后`
  return item.deadline.slice(5, 10).replace('-', '/')
}
function statusText(value: string) { return taskStatusLabels[value] || value }
function activityTime(value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  const diff = Date.now() - date.getTime()
  if (diff < 86400000 && date.getDate() === new Date().getDate()) return value.slice(11, 16)
  if (diff < 172800000) return '昨天'
  return value.slice(5, 10)
}
function projectTone(index: number) { return ['clay', 'slate', 'moss'][index % 3] }

onMounted(load)
</script>

<template>
  <div>
    <PageHead :eyebrow="`TODAY · ${today}`" :title="`${greeting}，${session.user?.userName || session.user?.userAccount}`" description="这里汇总了你需要关注的项目、任务、审核和验收事项。">
      <button class="btn" @click="router.push('/tasks')"><CalendarDays/>查看任务</button>
      <button v-if="canCreate" class="btn primary" @click="router.push('/projects')"><Plus/>新建设计项目</button>
    </PageHead>
    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="loading" class="panel dashboard-loading">正在汇总工作台数据…</p>

    <section class="stat-grid">
      <article><span class="stat-icon"><CheckCircle2/></span><div><small>我的待办</small><strong>{{ summary.myTodoCount }}</strong><p>任务、审核及项目验收</p></div></article>
      <article><span class="stat-icon blue"><Clock3/></span><div><small>待我审核</small><strong>{{ summary.pendingReviewCount }}</strong><p>另有 {{ summary.pendingAcceptanceCount }} 项项目验收</p></div></article>
      <article><span class="stat-icon amber"><CalendarDays/></span><div><small>即将到期</small><strong>{{ summary.dueSoonCount }}</strong><p>未来 3 天内到期</p></div></article>
      <article><span class="stat-icon green"><FileWarning/></span><div><small>已逾期</small><strong>{{ summary.overdueCount }}</strong><p>需要优先处理</p></div></article>
    </section>

    <section class="dash-grid">
      <article class="panel focus"><header><div><h2>今日重点</h2><p>按期限与优先级排列的个人待办</p></div><router-link to="/tasks">全部任务 <ArrowRight/></router-link></header>
        <p v-if="!loading && !todos.length" class="dashboard-empty">当前没有待处理事项</p>
        <div v-else class="focus-list"><button v-for="item in todos" :key="`${item.businessType}-${item.id}`" @click="router.push(item.route)"><time :class="{late:item.overdue}">{{ dueText(item) }}</time><span><strong>{{ item.title }}</strong><small>{{ item.subtitle }}</small></span><b class="priority" :class="priorityLabels[item.priority]">{{ priorityLabels[item.priority] }}优先级</b><StatusPill :value="statusText(item.status)"/><ArrowRight/></button></div>
      </article>

      <article class="panel progress"><header><div><h2>项目进度</h2><p>你有权访问的最近项目</p></div><router-link to="/projects" class="period">全部项目</router-link></header>
        <p v-if="!loading && !projectStatus.projects.length" class="dashboard-empty">暂无参与项目</p>
        <div v-else class="progress-list"><button v-for="(item,index) in projectStatus.projects" :key="item.id" @click="router.push('/projects')"><div><span class="project-icon" :class="projectTone(index)">{{ (item.category || '项').slice(0,1) }}</span><span><strong>{{ item.projectName }}</strong><small>{{ item.projectCode }}</small></span><b>{{ item.progress }}%</b></div><i><b :style="{width:`${item.progress}%`}"/></i><footer><span>{{ item.completedTasks }}/{{ item.totalTasks }} 项任务完成</span><StatusPill :value="projectStatusLabels[item.status]"/></footer></button></div>
      </article>

      <article class="panel activity"><header><div><h2>最近动态</h2><p>你参与项目中的真实业务记录</p></div></header>
        <p v-if="!loading && !activities.length" class="dashboard-empty">暂无项目动态</p>
        <div v-else class="timeline"><div v-for="item in activities" :key="item.id" @click="router.push(item.route)"><i :class="item.tone"/><time>{{ activityTime(item.createTime) }}</time><p><b>{{ item.actorName }}</b> {{ item.action }}<br><span>{{ item.subject }}</span></p></div></div>
      </article>

      <article class="insight dashboard-insight"><Sparkles/><p>FLOW INSIGHT</p><div class="completion-summary"><i class="completion-ring" :style="{'--rate':`${completion.completionRate * 3.6}deg`}"><b>{{ completion.completionRate }}%</b></i><div><h2>任务整体完成率</h2><span>{{ completion.completed }} / {{ completion.total }} 项已完成，{{ completion.overdue }} 项逾期</span></div></div><div class="status-bars"><div v-for="item in projectStatus.distribution" :key="item.status"><label><span>{{ projectStatusLabels[item.status] }}</span><b>{{ item.count }}</b></label><i><b :style="{width:`${item.count / maxProjectCount * 100}%`}"/></i></div></div><router-link to="/projects">查看项目组合 <ArrowRight/></router-link></article>
    </section>
  </div>
</template>
