# Nautilus Clinic（海螺诊所）

这是一个基于 **RuoYi** 改造的诊所管理项目，提供患者档案、接诊处方、药品库存、候诊叫号和本地模拟结算。当前主要用于本地开发与演示。

## 功能与边界

- **患者档案**：维护患者资料与就诊记录，可变信息存入 PostgreSQL JSONB。
- **接诊与处方**：记录主诉、诊断和处方内容。
- **库存与发药**：管理库存、批次和效期；库存不足时拒绝扣减。
- **候诊与模拟结算**：Redis 队列配合 WebSocket 叫号；模拟结算完成后联动发药。项目不接入真实支付或医保。

模拟结算和发药共用事务处理：以就诊单待处理状态作条件更新，保证并发确认只有一个成功；处方数量必须是正整数，发药失败会回滚状态和库存。

## 技术组成

前端使用 **Vue 2 + Element UI**；后端使用 **Java 17 + Spring Boot 3 + MyBatis-Plus**。业务数据存于 PostgreSQL，Redis 用于候诊队列和若依基础缓存。项目不包含 AI 问诊或 RAG 检索。

## 测试与运行验收

已有 7 项自动化测试：[4 项 PostgreSQL 16 集成测试](ruoyi-biz/src/test/java/com/ruoyi/clinic/service/impl/NautilusConsultationSettlementPostgresTest.java)覆盖并发确认、重复确认、失败回滚和按就诊单结算；[3 项输入单元测试](ruoyi-biz/src/test/java/com/ruoyi/clinic/util/PrescriptionUtilsTest.java)验证处方数量与药品编码。诊所业务表的公开初始化脚本为 [clinic-schema-pgsql.sql](sql/clinic-schema-pgsql.sql)，初始化顺序和本地启动步骤见[启动指南](docs/STARTUP.md)。

2026-09-23 的隔离启动探测使用临时 PostgreSQL 16、Redis 7 和临时配置：全新数据库按指南顺序建表成功；Spring Boot 根路径、Vue 开发服务器页面及 JavaScript bundle 均返回 HTTP 200。该结果验证了隔离环境的服务启动和页面资源加载，不代表默认本机配置、浏览器交互或完整业务流程已验收。

## 界面

| 接诊 | 药品库存 |
| --- | --- |
| ![接诊页面](docs/images/04-consultation.png) | ![药品库存页面](docs/images/05-inventory.png) |

更多截图见 [docs/images](docs/images/)。

## 许可

[MIT License](LICENSE)
