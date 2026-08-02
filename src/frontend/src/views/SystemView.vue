<script setup lang="ts">
import { Bell, CheckCheck, Download, FileClock, Search } from 'lucide-vue-next'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageHead from '../components/PageHead.vue'
const route=useRoute(),audit=computed(()=>route.meta.mode==='audit')
const logs=[['林知夏','创建项目','城市微光 · 秋冬通勤系列','成功','10:32','192.168.1.26'],['顾言','审核退回','TSK-260725-009','成功','09:48','192.168.1.32'],['周一禾','提交设计稿','V2 / TSK-260727-012','成功','09:26','192.168.1.18'],['管理员','停用账号','tang.yuan','成功','昨天 16:10','192.168.1.8']]
</script>
<template><div><PageHead :eyebrow="audit?'AUDIT TRAIL':'MESSAGE CENTER'" :title="audit?'审计日志':'消息通知'" :description="audit?'检索关键业务操作，保证权限与状态变化可追溯。':'集中查看任务分派、审核和到期提醒。'"><button class="btn" v-if="audit"><Download/>导出日志</button><button class="btn" v-else><CheckCheck/>全部标为已读</button></PageHead>
  <template v-if="audit"><div class="filters"><div><Search/><input placeholder="搜索操作者、对象编号"></div><select><option>全部模块</option><option>项目</option><option>任务</option><option>权限</option></select><input type="date"><span/></div><section class="panel table-scroll"><table><thead><tr><th>操作者</th><th>动作</th><th>业务对象</th><th>结果</th><th>时间</th><th>来源地址</th></tr></thead><tbody><tr v-for="l in logs" :key="l[4]"><td><strong>{{l[0]}}</strong></td><td>{{l[1]}}</td><td>{{l[2]}}</td><td><span class="success-dot">成功</span></td><td>{{l[4]}}</td><td>{{l[5]}}</td></tr></tbody></table></section></template>
  <section v-else class="notification-list panel"><article v-for="(n,i) in [['审核退回','机能风背心工艺单已被顾言退回，请根据意见修改。','刚刚'],['待审核提醒','通勤衬衫面料与色卡确认 V2 正在等待你审核。','36 分钟前'],['任务即将到期','双面羊毛短外套款式图将在 2 天后到期。','今天 08:30'],['新增项目成员','你已被加入「海岸线 · 男装度假胶囊」项目。','昨天 17:20']]" :key="n[0]" :class="{unread:i<2}"><span><Bell/></span><div><strong>{{n[0]}}</strong><p>{{n[1]}}</p><small>{{n[2]}}</small></div><button>查看详情</button></article></section>
</div></template>
