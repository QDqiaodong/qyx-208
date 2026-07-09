# 地质勘测野外工作站简易操作台勘测小队绑定台账系统

## 项目简介

本系统用于地质勘测后勤管理，实现野外操作台与勘测小队的绑定台账管理，支持按小队分组归集资产和按队员维度关联查询。

## 技术栈

- **前端**: Vue3 + Vite + Element Plus + TypeScript
- **后端**: Spring Boot 3.3 + JDK 17 + JPA
- **数据库**: MySQL 8.0 + Redis 7
- **容器化**: Docker + Docker Compose

## 访问地址

- 前端: http://localhost:8128
- 后端API: http://localhost:8138
- MySQL: 127.0.0.1:3354
- Redis: 127.0.0.1:6427

## 核心功能

1. **野外操作台基础档案录入** - 编号、承重、适配野外工作站
2. **勘测小队初始归属绑定** - 分配操作台所属外勤小队
3. **小队归属调整登记** - 更换操作台归属小队，留存变更记录
4. **按队员编号查询所属小队全部操作台** - 队员维度关联查询资产
5. **小队资产归集总览** - 按小队统计操作台数量和总承重

## 项目结构

```
qyx-208/
├── .env                        # 环境变量配置
├── docker-compose.yml          # Docker服务编排
├── backend/                    # Spring Boot后端
│   ├── src/main/java/com/example/geological/
│   │   ├── controller/         # REST API控制器
│   │   ├── service/            # 业务逻辑层
│   │   ├── repository/         # 数据访问层
│   │   ├── entity/             # JPA实体类
│   │   ├── dto/                # 数据传输对象
│   │   └── config/             # 配置类
│   └── Dockerfile
└── frontend/                   # Vue3前端
    ├── src/
    │   ├── views/              # 页面组件
    │   ├── api/                # API封装
    │   ├── App.vue
    │   └── main.ts
    ├── Dockerfile
    └── nginx.conf
```

## 启动方式

### 方式一：Docker Compose（推荐）

```bash
docker compose up -d --build
```

### 方式二：本地开发

**后端**:
```bash
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
mvn spring-boot:run
```

**前端**:
```bash
cd frontend
npm ci
npm run dev
```

## 环境变量

环境变量配置在 `.env` 文件中：

| 变量 | 值 |
|------|-----|
| FRONTEND_PORT | 8128 |
| BACKEND_PORT | 8138 |
| MYSQL_PORT | 3354 |
| REDIS_PORT | 6427 |
| MYSQL_ROOT_PASSWORD | geological2024 |
| MYSQL_DATABASE | geological |
| MYSQL_USER | geological |
| MYSQL_PASSWORD | geological2024 |
| REDIS_PASSWORD | geological2024 |