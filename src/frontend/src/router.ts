import { createRouter, createWebHistory } from 'vue-router'
import { session } from './api'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('./views/LoginView.vue'), meta: { public: true, title: '登录' } },
    {
      path: '/',
      component: () => import('./views/AppLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: () => import('./views/DashboardView.vue'), meta: { title: '工作台' } },
        { path: 'projects', component: () => import('./views/ProjectsView.vue'), meta: { title: '设计项目' } },
        { path: 'tasks', component: () => import('./views/TasksView.vue'), meta: { title: '设计任务' } },
        { path: 'reviews', component: () => import('./views/ReviewsView.vue'), meta: { title: '审核中心' } },
        { path: 'users', component: () => import('./views/UsersView.vue'), meta: { title: '用户管理' } },
        { path: 'roles', component: () => import('./views/RolesView.vue'), meta: { title: '角色权限' } },
        { path: 'notifications', component: () => import('./views/NotificationsView.vue'), meta: { title: '消息通知' } },
        { path: 'audit', component: () => import('./views/SystemView.vue'), meta: { title: '审计日志', mode: 'audit' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach((to) => {
  document.title = `${String(to.meta.title || '工作台')} · 织序`
  if (!to.meta.public && !session.user) return '/login'
  if (to.path === '/login' && session.user) return '/dashboard'
})

export default router
