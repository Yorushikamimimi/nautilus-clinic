# Nautilus Clinic 本地启动指南

本文从 PostgreSQL、Redis、Java 与 Node.js 已在本机可用开始。数据库地址、端口和账号由本机环境决定；命令通过 `PGHOST`、`PGPORT`、`PGUSER` 读取连接信息，密码由 `psql` 提示输入，不要把密码写进命令或提交到仓库。

## 首次初始化数据库

先设置当前终端的连接目标（按本机 PostgreSQL 实际地址调整）：

```bash
export PGHOST=localhost
export PGPORT=5432
export PGUSER=postgres
```

仅在目标数据库尚不存在时创建数据库。下例创建空数据库 `nautilus_clinic`：

```bash
psql -W -d postgres
```

在 `psql` 提示符中执行：

```sql
CREATE DATABASE nautilus_clinic;
\q
```

以下导入顺序适用于这个全新数据库。`ruoyi-pgsql.sql` 会重建 RuoYi 表，请勿在包含现有数据的数据库上运行：

```bash
psql -W -d nautilus_clinic -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA IF NOT EXISTS ruoyi;'
psql -W -d nautilus_clinic -v ON_ERROR_STOP=1 -f sql/ruoyi-pgsql.sql
psql -W -d nautilus_clinic -v ON_ERROR_STOP=1 -f sql/magic-api-pgsql.sql
psql -W -d nautilus_clinic -v ON_ERROR_STOP=1 -f sql/clinic-schema-pgsql.sql
```

`clinic-schema-pgsql.sql` 只创建患者、就诊和库存表、约束、索引与说明注释，不含患者或库存演示记录。脚本可重复执行。`magic-api-pgsql.sql` 和基础 RuoYi 脚本属于项目原有初始化 SQL。

## 启动前配置

确认 `ruoyi-admin/src/main/resources/application-devpg.yml` 中 PostgreSQL 与 Redis 的地址、端口、数据库名、用户名和本地凭据与你的环境一致。该配置文件由开发者按本机环境维护；不要把个人密码写入文档或提交。Redis 需已启动并可从应用连接。

首次安装前端依赖和 Maven 模块依赖：

```bash
cd ruoyi-ui && npm install
cd .. && mvn install -DskipTests
```

若 macOS 报 `vue-cli-service: Permission denied`，可执行：

```bash
chmod +x ruoyi-ui/node_modules/.bin/*
```

## 日常启动

在项目根目录分别开两个终端：

```bash
cd ruoyi-admin
mvn spring-boot:run -Dspring.profiles.active=devpg
```

```bash
cd ruoyi-ui
npm run dev -- --port 8080
```

后端默认端口由 `ruoyi-admin/src/main/resources/application.yml` 配置；前端开发服务器使用命令指定的 8080 端口。端口或本地配置有调整时，以对应配置文件及启动日志为准。

## 配置文件

- 后端通用配置：`ruoyi-admin/src/main/resources/application.yml`
- PostgreSQL 开发配置：`ruoyi-admin/src/main/resources/application-devpg.yml`
- 前端开发配置：`ruoyi-ui/.env.development`
- Webpack 开发服务器：`ruoyi-ui/vue.config.js`

## 常见问题

| 现象 | 检查方向 |
|------|----------|
| PostgreSQL 连接失败 | 检查 `PGHOST`、`PGPORT`、数据库名、账号、密码及本机网络；并确认 `application-devpg.yml` 使用相同连接参数 |
| Redis 连接失败 | 确认 Redis 服务正在运行，并核对开发配置中的地址、端口和本地凭据 |
| `vue-cli-service: Permission denied` | 运行上面的 `.bin` 权限修复命令 |
| Maven 找不到项目模块 | 在项目根目录重新运行 `mvn install -DskipTests` |
| 端口被占用 | 查看本机占用进程后停止该进程，或调整服务端口配置 |

本指南记录启动步骤，不代表前后端已在某台机器上完成启动或页面验收。
