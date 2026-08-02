import axios from 'axios'

export interface ApiResponse<T> { code: number; data: T; message: string }
export type UserRole = string
export interface LoginUser {
  id: string
  userAccount: string
  userName: string
  userAvatar?: string
  userProfile?: string
  userRole: UserRole
  userRoles?: UserRole[]
}

export interface UserVO extends LoginUser {}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface SysRole {
  id: string
  roleName: string
  roleCode: string
  description?: string
  sortOrder: number
  status: number
  createTime?: string
  updateTime?: string
}

export interface SysPermission {
  id: string
  permissionName: string
  permissionCode: string
  permissionType: number
  parentId: string
  path?: string
  sortOrder: number
  status: number
  description?: string
  createTime?: string
  updateTime?: string
}

export type ProjectStatus = 'draft' | 'approved' | 'designing' | 'acceptance' | 'completed' | 'cancelled'
export type ProjectPriority = 'low' | 'medium' | 'high'

export interface DesignProject {
  id: string
  projectCode: string
  projectName: string
  category?: string
  season?: string
  style?: string
  targetAudience?: string
  requirement?: string
  managerId: string
  managerName?: string
  acceptorId?: string
  acceptorName?: string
  priority: ProjectPriority
  status: ProjectStatus
  planStartDate?: string
  planEndDate?: string
  progress: number
  version: number
  submittedFileId?: string
  lastRejectionReason?: string
  cancelReason?: string
  createTime?: string
  updateTime?: string
}

export interface ProjectAcceptanceRecord {
  id: string
  projectId: string
  projectVersion: number
  acceptorId: string
  acceptorName?: string
  result: 'approved' | 'rejected'
  opinion?: string
  createTime?: string
}

export type DesignFileType = 'design' | 'reference' | 'attachment'

export interface DesignFileRecord {
  id: string
  projectId: string
  taskId?: string
  fileName: string
  fileType: DesignFileType
  mimeType: string
  fileSize: number
  versionNo: number
  versionNote?: string
  uploaderId: string
  uploaderName?: string
  createTime?: string
  submitted: boolean
}

export type ReviewResult = 'approved' | 'rejected'

export interface ReviewRecord {
  id: string
  taskId: string
  fileId: string
  versionNo: number
  reviewerId: string
  reviewerName?: string
  result: ReviewResult
  opinion?: string
  requestNo: string
  createTime?: string
}

export interface ReviewDetail {
  task: DesignTask
  submittedFile?: DesignFileRecord
  history: ReviewRecord[]
}

export interface DashboardSummary {
  myTodoCount: number
  pendingReviewCount: number
  pendingAcceptanceCount: number
  dueSoonCount: number
  overdueCount: number
}

export interface DashboardTodo {
  id: string
  businessType: 'task' | 'review' | 'acceptance'
  title: string
  subtitle: string
  status: string
  priority: ProjectPriority
  deadline?: string
  overdue: boolean
  route: string
}

export interface DashboardProjectItem {
  id: string
  projectCode: string
  projectName: string
  category?: string
  status: ProjectStatus
  progress: number
  totalTasks: number
  completedTasks: number
}

export interface DashboardProjectStatus {
  distribution: Array<{ status: ProjectStatus; count: number }>
  projects: DashboardProjectItem[]
}

export interface DashboardTaskCompletion {
  total: number
  completed: number
  inProgress: number
  overdue: number
  completionRate: number
}

export interface DashboardActivity {
  id: string
  type: 'file' | 'review' | 'acceptance'
  actorName: string
  action: string
  subject: string
  createTime?: string
  tone: string
  route: string
}

export type NotificationType = 'task' | 'review' | 'acceptance' | 'project' | 'deadline'
export interface NotificationRecord {
  id: string
  type: NotificationType
  title: string
  content: string
  businessType?: string
  businessId?: string
  route?: string
  read: boolean
  readTime?: string
  createTime?: string
}

export interface ProjectFormPayload {
  projectCode?: string
  projectName: string
  category?: string
  season?: string
  style?: string
  targetAudience?: string
  requirement?: string
  managerId?: string
  acceptorId?: string
  priority: ProjectPriority
  planStartDate?: string
  planEndDate?: string
  progress?: number
}

export type ProjectMemberRole = 'manager' | 'designer' | 'reviewer' | 'viewer'

export interface ProjectMember {
  id: string
  projectId: string
  userId: string
  userAccount?: string
  userName?: string
  userAvatar?: string
  userRole?: UserRole
  userRoles?: UserRole[]
  projectRole: ProjectMemberRole
  joinTime?: string
}

export type TaskStatus = 'unassigned' | 'pending_acceptance' | 'in_progress' | 'pending_review' | 'revision' | 'completed' | 'cancelled'

export interface DesignTask {
  id: string
  projectId: string
  projectCode?: string
  projectName?: string
  taskCode: string
  taskName: string
  requirement?: string
  assigneeId?: string
  assigneeName?: string
  reviewerId?: string
  reviewerName?: string
  priority: ProjectPriority
  status: TaskStatus
  deadline?: string
  progress: number
  version: number
  lastSubmitNote?: string
  rejectionReason?: string
  cancelReason?: string
  createTime?: string
  updateTime?: string
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 12000,
  withCredentials: true,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('satoken')
  if (token) config.headers.satoken = token
  return config
})

http.interceptors.response.use((response) => {
  const body = response.data
  if (body && typeof body.code === 'number' && body.code !== 0) {
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return response
})

export const userApi = {
  register: (payload: { userAccount: string; userPassword: string; checkPassword: string }) =>
    http.post<ApiResponse<number>>('/user/register', payload),
  login: (payload: { userAccount: string; userPassword: string }) =>
    http.post<ApiResponse<LoginUser>>('/user/login', payload),
  current: () => http.get<ApiResponse<LoginUser>>('/user/get/login'),
  logout: () => http.post<ApiResponse<boolean>>('/user/logout'),
  add: (payload: {
    userAccount: string
    userName: string
    userRole?: UserRole
    roleCodes?: UserRole[]
    userProfile?: string
  }) =>
    http.post<ApiResponse<number>>('/user/add', payload),
  list: (payload: {
    current: number
    pageSize: number
    userName?: string
    userAccount?: string
    userRole?: string
  }) => http.post<ApiResponse<PageResult<UserVO>>>('/user/list/page/vo', payload),
  update: (payload: {
    id: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: UserRole
    roleCodes?: UserRole[]
  }) => http.post<ApiResponse<number>>('/user/update', payload),
  remove: (id: string) => http.post<ApiResponse<boolean>>('/user/delete', { id }),
  get: (id: string) => http.get<ApiResponse<UserVO>>('/user/get/vo', { params: { id } }),
}

export const roleApi = {
  add: (payload: {
    roleName: string
    roleCode: string
    description?: string
    sortOrder?: number
    status?: number
  }) => http.post<ApiResponse<string>>('/role/add', payload),
  update: (payload: {
    id: string
    roleName?: string
    roleCode?: string
    description?: string
    sortOrder?: number
    status?: number
  }) => http.post<ApiResponse<boolean>>('/role/update', payload),
  remove: (id: string) => http.post<ApiResponse<boolean>>('/role/delete', { id }),
  get: (id: string) => http.get<ApiResponse<SysRole>>('/role/get', { params: { id } }),
  list: (payload: {
    current: number
    pageSize: number
    roleName?: string
    roleCode?: string
    status?: number
    sortField?: string
    sortOrder?: string
  }) => http.post<ApiResponse<PageResult<SysRole>>>('/role/list/page', payload),
  assignPermissions: (roleId: string, permissionIds: string[]) =>
    http.post<ApiResponse<boolean>>('/role/assign/permissions', { roleId, permissionIds }),
  listPermissions: (roleId: string) =>
    http.get<ApiResponse<SysPermission[]>>('/role/list/permissions', { params: { roleId } }),
}

export const permissionApi = {
  add: (payload: {
    permissionName: string
    permissionCode: string
    permissionType?: number
    parentId?: string
    path?: string
    sortOrder?: number
    status?: number
    description?: string
  }) => http.post<ApiResponse<string>>('/permission/add', payload),
  update: (payload: {
    id: string
    permissionName?: string
    permissionCode?: string
    permissionType?: number
    parentId?: string
    path?: string
    sortOrder?: number
    status?: number
    description?: string
  }) => http.post<ApiResponse<boolean>>('/permission/update', payload),
  remove: (id: string) => http.post<ApiResponse<boolean>>('/permission/delete', { id }),
  get: (id: string) =>
    http.get<ApiResponse<SysPermission>>('/permission/get', { params: { id } }),
  list: (payload: {
    current: number
    pageSize: number
    permissionName?: string
    permissionCode?: string
    permissionType?: number
    parentId?: string
    status?: number
    sortField?: string
    sortOrder?: string
  }) => http.post<ApiResponse<PageResult<SysPermission>>>('/permission/list/page', payload),
}

export const projectApi = {
  add: (payload: ProjectFormPayload & { projectCode: string }) =>
    http.post<ApiResponse<string>>('/project/add', payload),
  update: (payload: ProjectFormPayload & { id: string; version: number }) =>
    http.post<ApiResponse<boolean>>('/project/update', payload),
  get: (id: string) =>
    http.get<ApiResponse<DesignProject>>('/project/get', { params: { id } }),
  progress: (id: string) =>
    http.get<ApiResponse<Pick<DesignProject, 'id' | 'status' | 'progress' | 'version'>>>(
      '/project/progress', { params: { id } }),
  list: (payload: {
    current: number
    pageSize: number
    keyword?: string
    projectCode?: string
    projectName?: string
    category?: string
    season?: string
    priority?: ProjectPriority
    status?: ProjectStatus
    managerId?: string
    sortField?: string
    sortOrder?: string
  }) => http.post<ApiResponse<PageResult<DesignProject>>>('/project/list/page', payload),
  submit: (id: string, version: number) => projectAction('/project/submit', id, version),
  startDesign: (id: string, version: number) => projectAction('/project/start-design', id, version),
  submitAcceptance: (id: string, version: number) => projectAction('/project/submit-acceptance', id, version),
  accept: (id: string, version: number, opinion: string | undefined, requestNo: string) =>
    http.post<ApiResponse<boolean>>('/project/accept', { id, version, opinion, requestNo }),
  rejectAcceptance: (id: string, version: number, opinion: string, requestNo: string) =>
    http.post<ApiResponse<boolean>>('/project/reject-acceptance', { id, version, opinion, requestNo }),
  acceptanceHistory: (id: string) =>
    http.get<ApiResponse<ProjectAcceptanceRecord[]>>('/project/acceptance/history', { params: { id } }),
  acceptanceCandidates: (keyword?: string) =>
    http.get<ApiResponse<UserVO[]>>('/project/acceptance/candidates', { params: { keyword } }),
  cancel: (id: string, version: number, reason: string) =>
    projectAction('/project/cancel', id, version, reason),
}

export const projectMemberApi = {
  list: (projectId: string) =>
    http.get<ApiResponse<ProjectMember[]>>('/project/member/list', { params: { projectId } }),
  candidates: (projectId: string, keyword?: string) =>
    http.get<ApiResponse<UserVO[]>>('/project/member/candidates', { params: { projectId, keyword } }),
  add: (projectId: string, userId: string, projectRole: ProjectMemberRole) =>
    http.post<ApiResponse<string>>('/project/member/add', { projectId, userId, projectRole }),
  update: (id: string, projectRole: ProjectMemberRole) =>
    http.post<ApiResponse<boolean>>('/project/member/update', { id, projectRole }),
  remove: (id: string) =>
    http.post<ApiResponse<boolean>>('/project/member/remove', { id }),
}

export const taskApi = {
  list: (payload: TaskQueryPayload) =>
    http.post<ApiResponse<PageResult<DesignTask>>>('/task/list/page', payload),
  mine: (payload: TaskQueryPayload) =>
    http.post<ApiResponse<PageResult<DesignTask>>>('/task/my/page', payload),
  get: (id: string) => http.get<ApiResponse<DesignTask>>('/task/get', { params: { id } }),
  add: (payload: { projectId: string; taskCode: string; taskName: string; requirement?: string; priority: ProjectPriority; deadline?: string }) =>
    http.post<ApiResponse<string>>('/task/add', payload),
  update: (payload: { id: string; version: number; taskName: string; requirement?: string; priority: ProjectPriority; deadline?: string }) =>
    http.post<ApiResponse<boolean>>('/task/update', payload),
  assign: (id: string, version: number, assigneeId: string, reviewerId: string) =>
    http.post<ApiResponse<boolean>>('/task/assign', { id, version, assigneeId, reviewerId }),
  accept: (id: string, version: number) => taskAction('/task/accept', id, version),
  updateProgress: (id: string, version: number, progress: number) =>
    http.post<ApiResponse<boolean>>('/task/update-progress', { id, version, progress }),
  submitReview: (id: string, version: number, reason?: string) =>
    taskAction('/task/submit-review', id, version, reason),
  cancel: (id: string, version: number, reason: string) =>
    taskAction('/task/cancel', id, version, reason),
}

export interface TaskQueryPayload {
  current: number
  pageSize: number
  keyword?: string
  projectId?: string
  assigneeId?: string
  reviewerId?: string
  priority?: ProjectPriority
  status?: TaskStatus
  sortField?: string
  sortOrder?: string
}

function taskAction(path: string, id: string, version: number, reason?: string) {
  return http.post<ApiResponse<boolean>>(path, { id, version, reason })
}

export const designFileApi = {
  upload: (payload: { file: File; projectId: string; taskId?: string; fileType: DesignFileType; versionNote?: string }) => {
    const form = new FormData()
    form.append('file', payload.file)
    form.append('projectId', payload.projectId)
    if (payload.taskId) form.append('taskId', payload.taskId)
    form.append('fileType', payload.fileType)
    if (payload.versionNote) form.append('versionNote', payload.versionNote)
    return http.post<ApiResponse<string>>('/file/upload', form, { timeout: 60000 })
  },
  list: (params: { projectId?: string; taskId?: string; fileType?: DesignFileType }) =>
    http.get<ApiResponse<DesignFileRecord[]>>('/file/list', { params }),
  versions: (taskId: string) =>
    http.get<ApiResponse<DesignFileRecord[]>>('/file/version/list', { params: { taskId } }),
  get: (id: string) => http.get<ApiResponse<DesignFileRecord>>('/file/get', { params: { id } }),
  download: (id: string) => http.get<Blob>('/file/download', { params: { id }, responseType: 'blob', timeout: 60000 }),
  remove: (id: string) => http.post<ApiResponse<boolean>>('/file/delete', { id }),
}

export const reviewApi = {
  pending: (payload: TaskQueryPayload) =>
    http.post<ApiResponse<PageResult<DesignTask>>>('/review/pending/page', payload),
  detail: (taskId: string) =>
    http.get<ApiResponse<ReviewDetail>>('/review/detail', { params: { taskId } }),
  history: (taskId: string) =>
    http.get<ApiResponse<ReviewRecord[]>>('/review/history', { params: { taskId } }),
  approve: (payload: ReviewActionPayload) =>
    http.post<ApiResponse<boolean>>('/review/approve', payload),
  reject: (payload: ReviewActionPayload) =>
    http.post<ApiResponse<boolean>>('/review/reject', payload),
}

export const dashboardApi = {
  summary: () => http.get<ApiResponse<DashboardSummary>>('/dashboard/summary'),
  todos: (limit = 8) => http.get<ApiResponse<DashboardTodo[]>>('/dashboard/my-todos', { params: { limit } }),
  projectStatus: () => http.get<ApiResponse<DashboardProjectStatus>>('/dashboard/project-status'),
  taskCompletion: () => http.get<ApiResponse<DashboardTaskCompletion>>('/dashboard/task-completion'),
  activities: (limit = 8) => http.get<ApiResponse<DashboardActivity[]>>('/dashboard/recent-activities', { params: { limit } }),
}

export const notificationApi = {
  list: (payload: { current: number; pageSize: number; type?: NotificationType; unreadOnly?: boolean }) =>
    http.post<ApiResponse<PageResult<NotificationRecord>>>('/notification/list/page', payload),
  unreadCount: () => http.get<ApiResponse<number>>('/notification/unread-count'),
  read: (id: string) => http.post<ApiResponse<boolean>>('/notification/read', { id }),
  readAll: () => http.post<ApiResponse<boolean>>('/notification/read-all'),
}

export interface ReviewActionPayload {
  taskId: string
  taskVersion: number
  versionNo: number
  opinion?: string
  requestNo: string
}

function projectAction(path: string, id: string, version: number, reason?: string) {
  return http.post<ApiResponse<boolean>>(path, { id, version, reason })
}

const DEMO_USER: LoginUser = {
  id: '9001',
  userAccount: 'project.manager',
  userName: '林知夏',
  userRole: 'project_manager',
  userProfile: '女装产品中心 · 项目经理',
}

export const session = {
  get user(): LoginUser | null {
    const value = localStorage.getItem('login-user')
    return value ? JSON.parse(value) : null
  },
  set user(value: LoginUser | null) {
    if (value) localStorage.setItem('login-user', JSON.stringify(value))
    else localStorage.removeItem('login-user')
  },
  get demo() { return localStorage.getItem('demo-mode') === 'true' },
  hasRole(roleCode: string) {
    const current = this.user
    return current?.userRoles?.includes(roleCode) || current?.userRole === roleCode
  },
  enterDemo() {
    localStorage.setItem('demo-mode', 'true')
    this.user = DEMO_USER
  },
  clear() {
    localStorage.removeItem('demo-mode')
    localStorage.removeItem('satoken')
    this.user = null
  },
}
