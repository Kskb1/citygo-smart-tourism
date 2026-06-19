# 部署说明

## 依赖

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8

## 后端

配置环境变量后运行：

```bash
cd backend
mvn clean package
java -jar target/citygo-backend-0.1.0.jar
```

## 前端

```bash
cd frontend
npm install
npm run build
```

构建产物在 `frontend/dist`。

## 数据库

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/init.sql
```

生产环境应把 API 日志和缓存写入 MySQL 表，并根据 `expire_at` 控制缓存有效期。
