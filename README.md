# Nautilus Clinic（海螺诊所）

这是一个基于 **RuoYi** 改造的诊所管理项目。登录、角色和菜单权限沿用若依；项目新增患者档案、接诊处方、药品库存等业务模块，并提供候诊叫号和收费页面。当前主要用于本地开发与演示。

## 能做什么

- **患者档案**：登记、查询患者资料，查看就诊记录；标签和过敏史等可变信息存入 PostgreSQL JSONB。
- **接诊与处方**：记录主诉、诊断和处方，处方药品以 JSONB 保存。
- **药品库存**：维护药品、数量、批次和效期；发药或结算代码会检查并扣减库存。
- **候诊与收费**：候诊模块用 Redis 排队、WebSocket 推送叫号；收费模块提供待缴费处方查询、账单编号和结算接口。

这些模块在代码中有对应实现；**发药和结算各自处理就诊单状态，完整业务流程仍需联调验收**。

## 技术组成

前端是 **Vue 2 + Element UI**，后端是 **Java 17 + Spring Boot 3 + MyBatis-Plus**。业务数据存于 PostgreSQL；Redis 用于候诊队列及结算相关的编号和锁。目前不包含 AI 问诊或 RAG 检索。

## 界面

| 接诊 | 药品库存 |
| --- | --- |
| ![接诊页面](docs/images/04-consultation.png) | ![药品库存页面](docs/images/05-inventory.png) |

更多截图见 [docs/images](docs/images/)。

## 本地运行现状

后端需要 PostgreSQL 和 Redis，前端需要 Node.js。仓库有一份[本机启动记录](docs/STARTUP.md)，其中的容器名和端口是原开发环境配置，需要按自己的环境调整。

**当前 Git 仓库尚未收录诊所业务建表文件 `sql/clinic-pgsql.sql`。** 因此只克隆 GitHub 仓库，暂不能照启动记录完成全新数据库初始化。干净环境首次启动及完整浏览器流程也尚未验证。

## 许可

[MIT License](LICENSE)
