#!/bin/bash
# FunHub 状态检查脚本 (纯 Docker 版本)

echo "📊 FunHub 状态检查"
echo "=================="
echo ""

# 容器状态
echo "🐳 容器状态:"
docker ps -a --filter name=funhub --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

# 健康检查
echo "🏥 健康检查:"
if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "✅ Web 服务：正常"
    echo "   URL: http://localhost:8080"
else
    echo "❌ Web 服务：异常"
fi
echo ""

# 资源使用
echo "💾 资源使用:"
docker stats funhub --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" 2>/dev/null || echo "   无法获取统计信息"
echo ""

# 存储使用
echo "📁 存储使用:"
if [ -d "storage" ]; then
    du -sh storage/* 2>/dev/null || echo "   存储目录为空"
fi
echo ""

# 日志最后 10 行
echo "📝 最近日志:"
docker logs funhub --tail 10 2>/dev/null || echo "   无法获取日志"
echo ""
