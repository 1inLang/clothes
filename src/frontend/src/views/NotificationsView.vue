<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bell, BriefcaseBusiness, CheckCheck, ChevronLeft, ChevronRight, ClipboardCheck, Clock3, Shirt } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import PageHead from '../components/PageHead.vue'
import { notificationApi, session, type NotificationRecord, type NotificationType } from '../api'

const router = useRouter()
const rows = ref<NotificationRecord[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 12
const loading = ref(false)
const message = ref('')
const type = ref<'' | NotificationType>('')
const unreadOnly = ref(false)
const unreadCount = ref(0)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const typeOptions: Array<{ value: '' | NotificationType; label: string }> = [
  { value: '', label: '全部消息' }, { value: 'task', label: '任务' },
  { value: 'review', label: '设计审核' }, { value: 'acceptance', label: '项目验收' },
  { value: 'project', label: '项目成员' }, { value: 'deadline', label: '临期提醒' },
]

async function load() {
  if (session.demo) { loadDemo(); return }
  loading.value = true
  message.value = ''
  try {
    const [listRes, countRes] = await Promise.all([
      notificationApi.list({ current: current.value, pageSize,
        type: type.value || undefined, unreadOnly: unreadOnly.value || undefined }),
      notificationApi.unreadCount(),
    ])
    rows.value = listRes.data.data.records
    total.value = listRes.data.data.total
    unreadCount.value = countRes.data.data
    emitCount()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '通知加载失败'
  } finally { loading.value = false }
}

function loadDemo() {
  rows.value = [
    { id: '1', type: 'review', title: '设计稿被退回修改', content: '机能风背心工艺单已被退回，请根据审核意见修改。', route: '/tasks', read: false, createTime: new Date().toISOString() },
    { id: '2', type: 'review', title: '设计稿等待审核', content: '通勤衬衫面料与色卡确认 V2 正在等待你审核。', route: '/reviews', read: false, createTime: new Date(Date.now()-2160000).toISOString() },
    { id: '3', type: 'deadline', title: '任务即将到期', content: '双面羊毛短外套款式图将在 2 天内到期。', route: '/tasks', read: true, createTime: new Date(Date.now()-86400000).toISOString() },
  ]
  total.value = rows.value.length
  unreadCount.value = rows.value.filter(item => !item.read).length
  emitCount()
}

async function openNotification(item: NotificationRecord) {
  if (!item.read && !session.demo) {
    try {
      await notificationApi.read(item.id)
      item.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      emitCount()
    } catch (error) {
      message.value = error instanceof Error ? error.message : '通知状态更新失败'
      return
    }
  } else if (!item.read) {
    item.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    emitCount()
  }
  if (item.route) router.push(item.route)
}

async function readAll() {
  if (!unreadCount.value) return
  try {
    if (!session.demo) await notificationApi.readAll()
    rows.value.forEach(item => { item.read = true })
    unreadCount.value = 0
    emitCount()
    message.value = '全部通知已标记为已读'
    if (unreadOnly.value) await query()
  } catch (error) { message.value = error instanceof Error ? error.message : '全部已读操作失败' }
}

async function query() { current.value = 1; await load() }
async function goPage(step: number) {
  const next = current.value + step
  if (next < 1 || next > pageCount.value) return
  current.value = next
  await load()
}
function icon(typeValue: NotificationType) {
  return typeValue === 'task' ? Shirt : typeValue === 'review' ? ClipboardCheck
    : typeValue === 'acceptance' ? CheckCheck : typeValue === 'project' ? BriefcaseBusiness : Clock3
}
function timeText(value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  const minutes = Math.floor((Date.now() - date.getTime()) / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (minutes < 1440) return `${Math.floor(minutes / 60)} 小时前`
  return value.replace('T', ' ').slice(0, 16)
}
function emitCount() { window.dispatchEvent(new CustomEvent('notification-count-changed', { detail: unreadCount.value })) }

onMounted(load)
</script>

<template>
  <div>
    <PageHead eyebrow="MESSAGE CENTER" title="消息通知" description="集中查看任务分派、设计审核、项目验收、成员变化和临期提醒。">
      <button class="btn" :disabled="!unreadCount" @click="readAll"><CheckCheck/>全部标为已读</button>
    </PageHead>
    <div class="notification-toolbar">
      <div class="notification-tabs"><button v-for="item in typeOptions" :key="item.value" :class="{active:type===item.value}" @click="type=item.value;query()">{{ item.label }}</button></div>
      <label><input v-model="unreadOnly" type="checkbox" @change="query">只看未读</label>
      <span>{{ unreadCount }} 条未读</span>
    </div>
    <p v-if="message" class="notice">{{ message }}</p>
    <section class="notification-list panel">
      <p v-if="loading" class="notification-empty">正在加载通知…</p>
      <p v-else-if="!rows.length" class="notification-empty"><Bell/>暂无符合条件的通知</p>
      <article v-for="item in rows" v-else :key="item.id" :class="[{unread:!item.read},item.type]" @click="openNotification(item)">
        <span><component :is="icon(item.type)"/></span><div><header><strong>{{ item.title }}</strong><i v-if="!item.read">未读</i></header><p>{{ item.content }}</p><small>{{ timeText(item.createTime) }}</small></div><button>{{ item.route ? '查看详情' : '标为已读' }}</button>
      </article>
    </section>
    <footer v-if="total" class="pagination standalone"><span>共 {{ total }} 条 · 第 {{ current }} / {{ pageCount }} 页</span><button :disabled="current<=1" @click="goPage(-1)"><ChevronLeft/></button><button :disabled="current>=pageCount" @click="goPage(1)"><ChevronRight/></button></footer>
  </div>
</template>
