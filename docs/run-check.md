# 可运行性检查

## API Key 配置方法

后端通过环境变量读取真实数据接口配置：

```yaml
amap:
  api-key: ${AMAP_API_KEY:}

amadeus:
  client-id: ${AMADEUS_CLIENT_ID:}
  client-secret: ${AMADEUS_CLIENT_SECRET:}
  env: ${AMADEUS_ENV:test}

train:
  provider: ${TRAIN_PROVIDER:official_redirect}
  third-party-key: ${TRAIN_API_KEY:}
```

不要把真实 API Key 写进 `application.yml`，不要把真实 API Key 提交到 Git。前端不需要直接持有高德或 Amadeus Key，真实接口由后端调用。

本地开发步骤：

```powershell
cd "D:\project x\citygo-smart-tourism"
copy .env.local.example .env.local
```

编辑 `.env.local`：

```dotenv
AMAP_API_KEY=你的高德Key
AMADEUS_CLIENT_ID=你的ClientId
AMADEUS_CLIENT_SECRET=你的ClientSecret
AMADEUS_ENV=test
TRAIN_PROVIDER=official_redirect
TRAIN_API_KEY=
VITE_AMAP_JS_API_KEY=
```

使用脚本启动后端：

```powershell
.\start-backend.ps1
```

`.env.local` 已被 `.gitignore` 忽略。未配置 Key 时系统不会显示编造数据，而是返回接口未配置提示。高铁默认使用 `official_redirect`，不爬取 12306。

`VITE_AMAP_JS_API_KEY` 是可选的前端地图展示专用 Key。未配置时，前端会使用内置路线示意图，不会暴露后端高德 Web 服务 Key。

## 后端启动检查

在 Windows PowerShell 中执行：

```powershell
cd "D:\project x\citygo-smart-tourism\backend"
.\mvnw.cmd -DskipTests package
.\mvnw.cmd spring-boot:run
```

也可以在项目根目录执行：

```powershell
.\start-backend.ps1
```

后端默认地址：

```text
http://localhost:8080
```

健康检查入口：

```powershell
Invoke-RestMethod "http://localhost:8080/api/system/provider-status"
```

## 前端启动检查

```powershell
cd "D:\project x\citygo-smart-tourism\frontend"
npm install
npm run build
npm run dev
```

也可以在项目根目录执行：

```powershell
.\start-frontend.ps1
```

前端默认地址：

```text
http://localhost:5173
```

Vite 已配置 `/api` 代理到 `http://localhost:8080`。

## 数据库初始化检查

确认 MySQL 8 已启动后执行：

```powershell
cd "D:\project x\citygo-smart-tourism"
mysql -u root -p < database/schema.sql
mysql -u root -p < database/init.sql
```

初始化脚本只写入默认账号、城市 adcode、机场 IATA、兴趣分类和系统配置，不写入编造旅游价格。

## API Key 未配置时验证

不设置任何真实数据接口环境变量，启动后端，然后检查：

```powershell
Invoke-RestMethod "http://localhost:8080/api/spots/search?city=成都&keyword=景点"
Invoke-RestMethod "http://localhost:8080/api/weather?city=成都"
Invoke-RestMethod "http://localhost:8080/api/flights/search?fromCity=武汉&toCity=成都&date=2026-07-10"
Invoke-RestMethod "http://localhost:8080/api/hotels/search?city=成都&checkIn=2026-07-10&checkOut=2026-07-13"
Invoke-RestMethod "http://localhost:8080/api/trains/search?fromCity=武汉&toCity=成都&date=2026-07-10"
```

期望结果：

- 高德、Amadeus 相关接口返回 `realData=false`、`data=null`，并给出明确 `message`。
- 高铁接口返回 12306 官方查询提示与 `officialUrl`。
- 后端不应因为未配置 Key 返回 500。

## API Key 已配置时验证

PowerShell 示例：

```powershell
$env:AMAP_API_KEY="你的高德Key"
$env:AMADEUS_CLIENT_ID="你的ClientId"
$env:AMADEUS_CLIENT_SECRET="你的ClientSecret"
$env:AMADEUS_ENV="test"
$env:TRAIN_PROVIDER="official_redirect"
.\mvnw.cmd spring-boot:run
```

验证接口：

```powershell
Invoke-RestMethod "http://localhost:8080/api/spots/search?city=成都&keyword=景点"
Invoke-RestMethod "http://localhost:8080/api/weather?city=成都"
Invoke-RestMethod "http://localhost:8080/api/routes?origin=104.06,30.67&destination=104.08,30.66&mode=driving"
Invoke-RestMethod "http://localhost:8080/api/flights/search?fromCity=武汉&toCity=成都&date=2026-07-10"
Invoke-RestMethod "http://localhost:8080/api/hotels/search?city=成都&checkIn=2026-07-10&checkOut=2026-07-13"
```

真实接口成功时返回 `realData=true`、`sourceName`、`fetchedAt` 和原始响应 `rawJson`。

## 必查接口

```text
GET  /api/system/provider-status
GET  /api/spots/search
GET  /api/weather
GET  /api/routes
GET  /api/flights/search
GET  /api/hotels/search
GET  /api/trains/search
POST /api/plans/generate
POST /api/assistant/chat
GET  /api/admin/dashboard
GET  /api/admin/api-logs
```

## 常见错误

### java 或 JAVA_HOME 不可识别

安装 JDK 17+，然后配置：

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

永久配置可在 Windows“系统环境变量”中设置 `JAVA_HOME`，并把 `%JAVA_HOME%\bin` 加入 `Path`。

### mvn 不可识别

优先使用项目内脚本：

```powershell
cd "D:\project x\citygo-smart-tourism\backend"
.\mvnw.cmd -v
```

首次运行会尝试下载 Maven 到 `backend/.mvn/wrapper/`。如果公司网络或代理阻止下载，请手动安装 Maven 3.9+ 并加入 `Path`。

### npm 或 node 不可识别

安装 Node.js 18+ LTS，然后重新打开 PowerShell：

```powershell
node -v
npm -v
```

如果仍不可识别，把 Node.js 安装目录加入 Windows `Path`。

### npm install 失败

检查网络和 npm registry：

```powershell
npm config get registry
npm config set registry https://registry.npmjs.org/
npm install
```

### 前端接口 404 或连接失败

先确认后端运行在 `http://localhost:8080`，再启动前端。开发环境由 `frontend/vite.config.js` 把 `/api` 代理到后端。
