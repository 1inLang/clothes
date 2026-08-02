<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Check, ChevronLeft, ChevronRight, KeyRound, LockKeyhole, Pencil,
  Plus, Search, ShieldCheck, Trash2, X,
} from 'lucide-vue-next'
import PageHead from '../components/PageHead.vue'
import {
  permissionApi, roleApi, session,
  type SysPermission, type SysRole,
} from '../api'

type Dialog = 'role' | 'permission' | 'assign' | null

const isAdmin = session.hasRole('admin') && !session.demo
const activeTab = ref<'roles' | 'permissions'>('roles')
const roles = ref<SysRole[]>([])
const permissions = ref<SysPermission[]>([])
const allPermissions = ref<SysPermission[]>([])
const rolePermissions = ref<Record<string, SysPermission[]>>({})
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const dialog = ref<Dialog>(null)
const editing = ref(false)

const roleCurrent = ref(1)
const roleTotal = ref(0)
const rolePageSize = 8
const roleKeyword = ref('')

const permissionCurrent = ref(1)
const permissionTotal = ref(0)
const permissionPageSize = 12
const permissionKeyword = ref('')

const roleForm = reactive({
  id: '',
  roleName: '',
  roleCode: '',
  description: '',
  sortOrder: 0,
  status: 1,
})

const permissionForm = reactive({
  id: '',
  permissionName: '',
  permissionCode: '',
  permissionType: 2,
  parentId: '0',
  path: '',
  sortOrder: 0,
  status: 1,
  description: '',
})

const assigningRole = ref<SysRole | null>(null)
const assignedIds = ref<string[]>([])

const rolePages = computed(() => Math.max(1, Math.ceil(roleTotal.value / rolePageSize)))
const permissionPages = computed(() =>
  Math.max(1, Math.ceil(permissionTotal.value / permissionPageSize)))

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}

function roleColor(index: number) {
  return ['coral', 'blue', 'moss', 'sand'][index % 4]
}

function permissionsFor(roleId: string) {
  return rolePermissions.value[roleId] || []
}

function parentName(parentId: string) {
  if (!parentId || parentId === '0') return '顶级权限'
  return allPermissions.value.find((item) => item.id === parentId)?.permissionName || `#${parentId}`
}

async function loadRoles() {
  if (!isAdmin) return
  const keyword = roleKeyword.value.trim()
  const { data } = await roleApi.list({
    current: roleCurrent.value,
    pageSize: rolePageSize,
    roleName: keyword || undefined,
    sortField: 'sortOrder',
    sortOrder: 'ascend',
  })
  roles.value = data.data.records
  roleTotal.value = data.data.total

  const entries = await Promise.all(roles.value.map(async (role) => {
    const response = await roleApi.listPermissions(role.id)
    return [role.id, response.data.data] as const
  }))
  rolePermissions.value = Object.fromEntries(entries)
}

async function loadPermissions() {
  if (!isAdmin) return
  const keyword = permissionKeyword.value.trim()
  const { data } = await permissionApi.list({
    current: permissionCurrent.value,
    pageSize: permissionPageSize,
    permissionName: keyword || undefined,
    sortField: 'sortOrder',
    sortOrder: 'ascend',
  })
  permissions.value = data.data.records
  permissionTotal.value = data.data.total
}

async function loadPermissionOptions() {
  const { data } = await permissionApi.list({
    current: 1,
    pageSize: 500,
    sortField: 'sortOrder',
    sortOrder: 'ascend',
  })
  allPermissions.value = data.data.records
}

async function loadAll() {
  if (!isAdmin) return
  loading.value = true
  message.value = ''
  try {
    await Promise.all([loadRoles(), loadPermissions(), loadPermissionOptions()])
  } catch (error) {
    message.value = errorMessage(error, '角色权限数据加载失败')
  } finally {
    loading.value = false
  }
}

function openAddRole() {
  editing.value = false
  Object.assign(roleForm, {
    id: '', roleName: '', roleCode: '', description: '', sortOrder: 0, status: 1,
  })
  dialog.value = 'role'
}

function openEditRole(role: SysRole) {
  editing.value = true
  Object.assign(roleForm, {
    id: role.id,
    roleName: role.roleName,
    roleCode: role.roleCode,
    description: role.description || '',
    sortOrder: role.sortOrder,
    status: role.status,
  })
  dialog.value = 'role'
}

function openAddPermission() {
  editing.value = false
  Object.assign(permissionForm, {
    id: '', permissionName: '', permissionCode: '', permissionType: 2,
    parentId: '0', path: '', sortOrder: 0, status: 1, description: '',
  })
  dialog.value = 'permission'
}

function openEditPermission(permission: SysPermission) {
  editing.value = true
  Object.assign(permissionForm, {
    id: permission.id,
    permissionName: permission.permissionName,
    permissionCode: permission.permissionCode,
    permissionType: permission.permissionType,
    parentId: permission.parentId || '0',
    path: permission.path || '',
    sortOrder: permission.sortOrder,
    status: permission.status,
    description: permission.description || '',
  })
  dialog.value = 'permission'
}

function openAssign(role: SysRole) {
  assigningRole.value = role
  assignedIds.value = permissionsFor(role.id).map((item) => item.id)
  dialog.value = 'assign'
}

async function saveRole() {
  saving.value = true
  message.value = ''
  try {
    const payload = {
      roleName: roleForm.roleName.trim(),
      roleCode: roleForm.roleCode.trim(),
      description: roleForm.description.trim(),
      sortOrder: roleForm.sortOrder,
      status: roleForm.status,
    }
    if (editing.value) {
      await roleApi.update({ id: roleForm.id, ...payload })
      message.value = '角色更新成功'
    } else {
      await roleApi.add(payload)
      message.value = '角色创建成功'
    }
    dialog.value = null
    await loadRoles()
  } catch (error) {
    message.value = errorMessage(error, '角色保存失败')
  } finally {
    saving.value = false
  }
}

async function savePermission() {
  saving.value = true
  message.value = ''
  try {
    const payload = {
      permissionName: permissionForm.permissionName.trim(),
      permissionCode: permissionForm.permissionCode.trim(),
      permissionType: permissionForm.permissionType,
      parentId: permissionForm.parentId || '0',
      path: permissionForm.path.trim(),
      sortOrder: permissionForm.sortOrder,
      status: permissionForm.status,
      description: permissionForm.description.trim(),
    }
    if (editing.value) {
      await permissionApi.update({ id: permissionForm.id, ...payload })
      message.value = '权限更新成功'
    } else {
      await permissionApi.add(payload)
      message.value = '权限创建成功'
    }
    dialog.value = null
    await loadAll()
  } catch (error) {
    message.value = errorMessage(error, '权限保存失败')
  } finally {
    saving.value = false
  }
}

async function saveAssignment() {
  if (!assigningRole.value) return
  saving.value = true
  message.value = ''
  try {
    await roleApi.assignPermissions(assigningRole.value.id, assignedIds.value)
    message.value = `“${assigningRole.value.roleName}”的权限已更新`
    dialog.value = null
    await loadRoles()
  } catch (error) {
    message.value = errorMessage(error, '角色授权失败')
  } finally {
    saving.value = false
  }
}

async function removeRole(role: SysRole) {
  if (!window.confirm(`确定删除角色“${role.roleName}”吗？`)) return
  try {
    await roleApi.remove(role.id)
    message.value = '角色删除成功'
    await loadRoles()
  } catch (error) {
    message.value = errorMessage(error, '角色删除失败')
  }
}

async function removePermission(permission: SysPermission) {
  if (!window.confirm(`确定删除权限“${permission.permissionName}”吗？`)) return
  try {
    await permissionApi.remove(permission.id)
    message.value = '权限删除成功'
    await loadAll()
  } catch (error) {
    message.value = errorMessage(error, '权限删除失败')
  }
}

async function changeRolePage(step: number) {
  const next = roleCurrent.value + step
  if (next < 1 || next > rolePages.value) return
  roleCurrent.value = next
  await loadRoles()
}

async function changePermissionPage(step: number) {
  const next = permissionCurrent.value + step
  if (next < 1 || next > permissionPages.value) return
  permissionCurrent.value = next
  await loadPermissions()
}

async function switchTab(tab: 'roles' | 'permissions') {
  activeTab.value = tab
  message.value = ''
}

onMounted(loadAll)
</script>

<template>
  <div>
    <PageHead
      eyebrow="RBAC CONTROL"
      title="角色权限"
      description="维护系统角色、功能权限以及角色与权限之间的授权关系。"
    >
      <button
        v-if="isAdmin && activeTab === 'roles'"
        class="btn primary"
        @click="openAddRole"
      ><Plus />创建角色</button>
      <button
        v-if="isAdmin && activeTab === 'permissions'"
        class="btn primary"
        @click="openAddPermission"
      ><Plus />创建权限</button>
    </PageHead>

    <div class="permission-note">
      <LockKeyhole />
      <p>
        <strong>权限配置由后端 Sa-Token 实时校验</strong>
        <span>前端负责展示和配置，最终访问权限以服务端角色及权限数据为准。</span>
      </p>
    </div>

    <section v-if="!isAdmin" class="panel access-empty">
      <LockKeyhole />
      <h2>仅系统管理员可以访问</h2>
      <p>当前账号没有角色与权限配置功能，请联系系统管理员。</p>
    </section>

    <template v-else>
      <div class="rbac-tabs">
        <button :class="{ active: activeTab === 'roles' }" @click="switchTab('roles')">
          <ShieldCheck />角色管理 <b>{{ roleTotal }}</b>
        </button>
        <button :class="{ active: activeTab === 'permissions' }" @click="switchTab('permissions')">
          <KeyRound />权限管理 <b>{{ permissionTotal }}</b>
        </button>
      </div>

      <p v-if="message" class="notice">{{ message }}</p>

      <template v-if="activeTab === 'roles'">
        <div class="filters rbac-filters">
          <div><Search /><input v-model="roleKeyword" placeholder="搜索角色名称" @keyup.enter="roleCurrent=1;loadRoles()"></div>
          <button class="ghost" @click="roleCurrent=1;loadRoles()"><Search />查询</button>
          <span />
          <small>共 {{ roleTotal }} 个角色</small>
        </div>

        <div v-if="loading" class="panel table-state">正在加载角色数据…</div>
        <div v-else-if="!roles.length" class="panel table-state">暂无符合条件的角色</div>
        <section v-else class="role-grid">
          <article v-for="(role, index) in roles" :key="role.id" class="panel role-card">
            <header>
              <span :class="roleColor(index)"><ShieldCheck /></span>
              <div>
                <h2>{{ role.roleName }}</h2>
                <small>{{ role.roleCode }} · 排序 {{ role.sortOrder }}</small>
              </div>
              <em :class="{ off: role.status === 0 }">{{ role.status === 1 ? '已启用' : '已停用' }}</em>
            </header>
            <p>{{ role.description || '暂无角色说明' }}</p>
            <div>
              <span>已授权 {{ permissionsFor(role.id).length }} 项权限</span>
              <ul v-if="permissionsFor(role.id).length">
                <li v-for="permission in permissionsFor(role.id).slice(0, 6)" :key="permission.id">
                  <Check />{{ permission.permissionCode }}
                </li>
              </ul>
              <small v-else class="empty-permission">尚未配置权限</small>
            </div>
            <footer>
              <button @click="openEditRole(role)"><Pencil />编辑角色</button>
              <button @click="openAssign(role)"><KeyRound />分配权限</button>
              <button class="danger-text" @click="removeRole(role)"><Trash2 />删除</button>
            </footer>
          </article>
        </section>

        <footer class="pagination standalone">
          <span>第 {{ roleCurrent }} / {{ rolePages }} 页</span>
          <button :disabled="roleCurrent <= 1" @click="changeRolePage(-1)"><ChevronLeft /></button>
          <button :disabled="roleCurrent >= rolePages" @click="changeRolePage(1)"><ChevronRight /></button>
        </footer>
      </template>

      <template v-else>
        <div class="filters rbac-filters">
          <div><Search /><input v-model="permissionKeyword" placeholder="搜索权限名称" @keyup.enter="permissionCurrent=1;loadPermissions()"></div>
          <button class="ghost" @click="permissionCurrent=1;loadPermissions()"><Search />查询</button>
          <span />
          <small>共 {{ permissionTotal }} 项权限</small>
        </div>

        <section class="panel table-scroll">
          <table>
            <thead>
              <tr><th>权限名称</th><th>权限编码</th><th>类型</th><th>父级</th><th>路径</th><th>状态</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="7" class="table-state">正在加载权限数据…</td></tr>
              <tr v-else-if="!permissions.length"><td colspan="7" class="table-state">暂无符合条件的权限</td></tr>
              <tr v-for="permission in permissions" :key="permission.id">
                <td><strong>{{ permission.permissionName }}</strong><small>{{ permission.description || '暂无说明' }}</small></td>
                <td><code class="permission-code">{{ permission.permissionCode }}</code></td>
                <td>{{ permission.permissionType === 1 ? '菜单' : '按钮 / 接口' }}</td>
                <td>{{ parentName(permission.parentId) }}</td>
                <td>{{ permission.path || '—' }}</td>
                <td><span class="status-chip" :class="{ off: permission.status === 0 }">{{ permission.status === 1 ? '启用' : '停用' }}</span></td>
                <td>
                  <button class="text-btn" @click="openEditPermission(permission)">编辑</button>
                  <button class="text-btn danger-text" @click="removePermission(permission)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <footer class="pagination">
            <span>第 {{ permissionCurrent }} / {{ permissionPages }} 页</span>
            <button :disabled="permissionCurrent <= 1" @click="changePermissionPage(-1)"><ChevronLeft /></button>
            <button :disabled="permissionCurrent >= permissionPages" @click="changePermissionPage(1)"><ChevronRight /></button>
          </footer>
        </section>
      </template>
    </template>

    <div v-if="dialog === 'role'" class="modal-wrap" @click.self="dialog=null">
      <div class="modal">
        <header>
          <div><p>{{ editing ? 'EDIT ROLE' : 'ADD ROLE' }}</p><h2>{{ editing ? '编辑角色' : '创建角色' }}</h2></div>
          <button @click="dialog=null"><X /></button>
        </header>
        <form @submit.prevent="saveRole">
          <label><span>角色名称</span><input v-model.trim="roleForm.roleName" required maxlength="50" placeholder="例如：设计师"></label>
          <label><span>角色编码</span><input v-model.trim="roleForm.roleCode" required pattern="[a-z][a-z0-9_]{1,49}" placeholder="例如：designer"></label>
          <div class="form-row">
            <label><span>显示顺序</span><input v-model.number="roleForm.sortOrder" type="number" min="0"></label>
            <label><span>角色状态</span><select v-model.number="roleForm.status"><option :value="1">启用</option><option :value="0">停用</option></select></label>
          </div>
          <label><span>角色说明</span><textarea v-model="roleForm.description" maxlength="255" placeholder="说明该角色的职责范围"></textarea></label>
          <footer><button type="button" class="btn" @click="dialog=null">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '正在保存…' : '确认保存' }}</button></footer>
        </form>
      </div>
    </div>

    <div v-if="dialog === 'permission'" class="modal-wrap" @click.self="dialog=null">
      <div class="modal wide">
        <header>
          <div><p>{{ editing ? 'EDIT PERMISSION' : 'ADD PERMISSION' }}</p><h2>{{ editing ? '编辑权限' : '创建权限' }}</h2></div>
          <button @click="dialog=null"><X /></button>
        </header>
        <form @submit.prevent="savePermission">
          <div class="form-row">
            <label><span>权限名称</span><input v-model.trim="permissionForm.permissionName" required maxlength="100" placeholder="例如：查看设计"></label>
            <label><span>权限编码</span><input v-model.trim="permissionForm.permissionCode" required pattern="[a-z][a-z0-9:_-]{1,99}" placeholder="例如：design:view"></label>
          </div>
          <div class="form-row">
            <label><span>权限类型</span><select v-model.number="permissionForm.permissionType"><option :value="1">菜单</option><option :value="2">按钮 / 接口</option></select></label>
            <label><span>父级权限</span><select v-model="permissionForm.parentId"><option value="0">顶级权限</option><option v-for="item in allPermissions.filter(p => p.id !== permissionForm.id)" :key="item.id" :value="item.id">{{ item.permissionName }}</option></select></label>
          </div>
          <div class="form-row">
            <label><span>路由或接口路径</span><input v-model.trim="permissionForm.path" placeholder="/project/**"></label>
            <label><span>显示顺序</span><input v-model.number="permissionForm.sortOrder" type="number" min="0"></label>
          </div>
          <label><span>权限状态</span><select v-model.number="permissionForm.status"><option :value="1">启用</option><option :value="0">停用</option></select></label>
          <label><span>权限说明</span><textarea v-model="permissionForm.description" maxlength="255" placeholder="说明该权限允许执行的操作"></textarea></label>
          <footer><button type="button" class="btn" @click="dialog=null">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '正在保存…' : '确认保存' }}</button></footer>
        </form>
      </div>
    </div>

    <div v-if="dialog === 'assign' && assigningRole" class="modal-wrap" @click.self="dialog=null">
      <div class="modal wide">
        <header>
          <div><p>ASSIGN PERMISSIONS</p><h2>为“{{ assigningRole.roleName }}”分配权限</h2></div>
          <button @click="dialog=null"><X /></button>
        </header>
        <form @submit.prevent="saveAssignment">
          <div v-if="!allPermissions.length" class="assign-empty">暂无可分配权限，请先创建权限。</div>
          <div v-else class="permission-picker">
            <label v-for="permission in allPermissions" :key="permission.id" :class="{ disabled: permission.status === 0 }">
              <input v-model="assignedIds" type="checkbox" :value="permission.id" :disabled="permission.status === 0">
              <span><strong>{{ permission.permissionName }}</strong><small>{{ permission.permissionCode }}</small></span>
            </label>
          </div>
          <p>已选择 {{ assignedIds.length }} 项权限。保存后将覆盖该角色原有授权。</p>
          <footer><button type="button" class="btn" @click="dialog=null">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '正在授权…' : '保存授权' }}</button></footer>
        </form>
      </div>
    </div>
  </div>
</template>
