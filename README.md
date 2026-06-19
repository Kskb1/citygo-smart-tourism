# 智游 CityGo

CityGo 是一个智慧旅游服务平台，提供登录注册、真实 POI/天气/路线查询、景点探索、智能行程规划、预算参考、个人行程保存和管理员后台。

CityGo 使用高德开放平台提供真实地点、天气和地面路线数据。机票、火车票、酒店及其他费用为系统规则预估的非实时预算参考，实际价格以航空公司、12306、酒店或正规预订平台为准。

## 技术栈

- Backend: Spring Boot 3, Java 17, JDBC, Flyway, MySQL, BCrypt, JWT
- Frontend: Vue 3, Vite, Element Plus, Axios, ECharts
- Deployment: Docker, Docker Compose, Nginx

## 功能列表

- 用户注册、登录、JWT 会话、权限控制
- 首页数据源状态
- 景点推荐与目的地探索
- 天气查询
- 酒店和机场 POI 查询
- 交通路线查询与 12306 官方跳转
- 智能规划、跨城市合理性判断、预算参考价
- 保存行程、我的行程、行程详情、改标题、删除
- 管理员后台：用户统计、行程统计、API 日志、预算规则

## 数据和价格原则

- 高德 Web 服务 Key 只在后端读取。
- 未配置 Key 时返回明确提示，不生成替代数据。
- 酒店和机场展示真实 POI，不展示编造房价、房态或航班。
- 高铁默认 `official_redirect`，不爬取 12306。
- 预算参考价的 `priceMode` 为 `RULE_ESTIMATED`，保存行程时会保存预算快照和规则版本。

## 本地运行

复制配置：

```powershell
copy .env.local.example .env.local
```

填写至少这些变量：

```dotenv
AMAP_API_KEY=你的高德开放平台Web服务Key
MYSQL_URL=jdbc:mysql://localhost:3306/citygo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
MYSQL_USER=root
MYSQL_PASSWORD=你的本地MySQL密码
JWT_SECRET=本地开发用长随机字符串
CITYGO_DEMO_USERS_ENABLED=true
```

初始化数据库：

```powershell
mysql -u root -p < database/schema.sql
mysql -u root -p < database/init.sql
```

启动：

```powershell
.\start-backend.ps1
.\start-frontend.ps1
```

访问：

```text
http://localhost:5173
```

默认开发账号：

- `user / 123456`
- `admin / 123456`

## Docker 运行

```bash
cp .env.production.example .env.production
# edit .env.production
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml up -d
```

生产只暴露 Nginx 80 端口；MySQL 3306 和 Spring Boot 8080 只在 Docker 网络内部使用。

## 数据库说明

- 生产使用 Flyway：`backend/src/main/resources/db/migration/`
- 核心表：`users`、`user_trip`、`api_call_log`
- `users.password_hash` 使用 BCrypt
- `user_trip.user_id` 由后端根据 JWT 获取，前端不能传入 userId 决定归属
- `plan_json` 保存完整行程和预算快照

## 环境变量

见：

- `.env.example`
- `.env.local.example`
- `.env.production.example`

真实 Key、数据库密码和 `JWT_SECRET` 不得提交到 Git。

## 项目目录

```text
backend/     Spring Boot 后端
frontend/    Vue 前端与 Nginx 配置
database/    本地参考 SQL
docs/        设计、接口和运行检查文档
scripts/     部署、更新、备份、恢复、健康检查脚本
```

## 云端部署

详见 [DEPLOYMENT.md](DEPLOYMENT.md)。

## 安全注意事项

- 生产必须设置强 `JWT_SECRET`。
- 生产必须设置 `CITYGO_ADMIN_PASSWORD`。
- 生产建议 `CITYGO_DEMO_USERS_ENABLED=false`。
- 不要暴露 MySQL 3306。
- 不要把 `.env.production`、`.env.local`、真实 API Key 或密码提交到仓库。

## 已知限制

- 机票、火车、酒店费用为规则预算参考，不是实时价格。
- 未配置高德 Key 时，POI、天气、路线等真实数据功能会返回未配置提示。
- Docker Compose 不默认配置 HTTPS，需要在云服务器或反向代理层配置证书。
