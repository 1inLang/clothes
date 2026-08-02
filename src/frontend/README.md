# 织序 Vue 前端

## 本地启动

```bash
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。开发服务器会将 `/api` 请求代理到
`http://localhost:8123`，请先启动 Spring Boot 后端。

## 构建

```bash
npm run build
```

构建结果输出到 `dist`。

## 接口说明

当前真实接入：

- `POST /api/user/login`
- `GET /api/user/get/login`
- `POST /api/user/logout`
- `POST /api/user/add`

项目、任务、审核、通知与日志页面当前使用 `src/demo.ts` 中的演示数据。
后端接口完成后，可将对应页面的数据来源替换为独立 API 服务。
