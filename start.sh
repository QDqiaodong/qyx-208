#!/bin/bash
set -e

echo "========================================"
echo "  地质勘测野外工作站台账系统 - 启动脚本"
echo "========================================"
echo ""

cd "$(dirname "$0")"

echo "[1/3] 检查端口占用情况..."
PORTS=(8128 8138 3354 6427)
for PORT in "${PORTS[@]}"; do
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "  ❌ 端口 $PORT 被占用"
        exit 1
    else
        echo "  ✅ 端口 $PORT 可用"
    fi
done

echo ""
echo "[2/3] 停止现有容器..."
docker compose down

echo ""
echo "[3/3] 构建并启动所有服务..."
docker compose up -d --build

echo ""
echo "等待服务启动..."
sleep 10

echo ""
echo "========================================"
echo "  服务状态检查"
echo "========================================"
docker compose ps

echo ""
echo "========================================"
echo "  访问地址"
echo "========================================"
echo "  前端: http://localhost:8128"
echo "  后端API: http://localhost:8138/api"
echo "  MySQL: 127.0.0.1:3354"
echo "  Redis: 127.0.0.1:6427"
echo "========================================"