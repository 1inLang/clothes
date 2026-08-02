export const projects = [
  { id: 1, code: 'FW26-W-014', name: '城市微光 · 秋冬通勤系列', category: '女装', season: '2026 秋冬', manager: '林知夏', status: '设计中', progress: 68, due: '08-18', priority: '高', done: 8, total: 12, tone: 'clay' },
  { id: 2, code: 'SS27-M-003', name: '海岸线 · 男装度假胶囊', category: '男装', season: '2027 春夏', manager: '林知夏', status: '已立项', progress: 24, due: '09-05', priority: '中', done: 2, total: 9, tone: 'slate' },
  { id: 3, code: 'FW26-K-008', name: '小小探险家 · 儿童户外系列', category: '童装', season: '2026 秋冬', manager: '唐予安', status: '验收中', progress: 92, due: '08-02', priority: '高', done: 11, total: 12, tone: 'moss' },
  { id: 4, code: 'SS27-A-002', name: '柔性秩序 · 配饰拓展企划', category: '配饰', season: '2027 春夏', manager: '沈可', status: '草稿', progress: 8, due: '10-12', priority: '低', done: 0, total: 6, tone: 'sand' },
]

export const tasks = [
  { id: 101, code: 'TSK-260729-018', name: '双面羊毛短外套款式图', project: '城市微光 · 秋冬通勤系列', assignee: '许棠', reviewer: '顾言', status: '进行中', priority: '高', due: '07-31', progress: 72, version: 'V3' },
  { id: 102, code: 'TSK-260727-012', name: '通勤衬衫面料与色卡确认', project: '城市微光 · 秋冬通勤系列', assignee: '周一禾', reviewer: '顾言', status: '待审核', priority: '高', due: '今天', progress: 90, version: 'V2' },
  { id: 103, code: 'TSK-260725-009', name: '机能风背心工艺单', project: '城市微光 · 秋冬通勤系列', assignee: '许棠', reviewer: '宋嘉', status: '退回修改', priority: '中', due: '逾期 1 天', progress: 64, version: 'V4' },
  { id: 104, code: 'TSK-260730-023', name: '沙滩针织套装灵感版', project: '海岸线 · 男装度假胶囊', assignee: '陈屿', reviewer: '叶嘉', status: '待领取', priority: '中', due: '08-08', progress: 10, version: '—' },
  { id: 105, code: 'TSK-260721-004', name: '儿童冲锋衣版型定稿', project: '小小探险家 · 儿童户外系列', assignee: '罗薇', reviewer: '顾言', status: '已完成', priority: '高', due: '07-28', progress: 100, version: 'V5' },
]

export const members = [
  { id: 1, name: '林知夏', account: 'lin.zhixia', role: '项目经理', roleValue: 'project_manager', department: '女装产品中心', status: '正常', lastLogin: '今天 09:42' },
  { id: 2, name: '许棠', account: 'xu.tang', role: '设计师', roleValue: 'designer', department: '女装设计一组', status: '正常', lastLogin: '今天 10:18' },
  { id: 3, name: '顾言', account: 'gu.yan', role: '审核人', roleValue: 'reviewer', department: '设计管理部', status: '正常', lastLogin: '昨天 18:35' },
  { id: 4, name: '周一禾', account: 'zhou.yihe', role: '设计师', roleValue: 'designer', department: '女装设计一组', status: '正常', lastLogin: '今天 08:56' },
  { id: 5, name: '唐予安', account: 'tang.yuan', role: '项目经理', roleValue: 'project_manager', department: '童装产品中心', status: '停用', lastLogin: '07-22 16:10' },
]

export const reviews = [
  { id: 1, task: '通勤衬衫面料与色卡确认', project: '城市微光', designer: '周一禾', version: 'V2', wait: '36 分钟', priority: '高' },
  { id: 2, task: '儿童户外裤反光细节', project: '小小探险家', designer: '方圆', version: 'V3', wait: '18 小时', priority: '中' },
  { id: 3, task: '针织开衫色彩方案', project: '城市微光', designer: '许棠', version: 'V1', wait: '2 天', priority: '高' },
]
