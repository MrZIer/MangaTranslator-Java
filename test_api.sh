#!/bin/bash

# API测试脚本
# 使用curl测试所有API端点

BASE_URL="http://localhost:8080"
TEST_IMAGE="test_manga.png"  # 请替换为实际的测试图片路径

echo "================================================"
echo "   漫画翻译工具API测试脚本"
echo "================================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试1: 上传文件
echo -e "${YELLOW}[测试1] 上传文件并开始翻译${NC}"
echo "----------------------------------------"

if [ ! -f "$TEST_IMAGE" ]; then
    echo -e "${RED}错误: 找不到测试图片 $TEST_IMAGE${NC}"
    echo "请创建或指定一个有效的测试图片文件"
    exit 1
fi

UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/upload" \
  -F "file=@$TEST_IMAGE" \
  -F "engine=OPENAI" \
  -F "sourceLanguage=ja" \
  -F "targetLanguage=zh" \
  -F "outputFormat=SINGLE_IMAGE")

echo "$UPLOAD_RESPONSE" | jq '.'

# 提取sessionId
SESSION_ID=$(echo "$UPLOAD_RESPONSE" | jq -r '.data.sessionId')

if [ "$SESSION_ID" = "null" ] || [ -z "$SESSION_ID" ]; then
    echo -e "${RED}❌ 上传失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 上传成功${NC}"
echo "Session ID: $SESSION_ID"
echo ""

# 测试2: 查询进度
echo -e "${YELLOW}[测试2] 查询任务进度${NC}"
echo "----------------------------------------"
sleep 2

PROGRESS_RESPONSE=$(curl -s "$BASE_URL/api/tasks/$SESSION_ID/progress")
echo "$PROGRESS_RESPONSE" | jq '.'

STATUS=$(echo "$PROGRESS_RESPONSE" | jq -r '.data.status')
PROGRESS=$(echo "$PROGRESS_RESPONSE" | jq -r '.data.progress')

echo -e "${GREEN}✓ 当前状态: $STATUS${NC}"
echo -e "${GREEN}✓ 当前进度: $PROGRESS%${NC}"
echo ""

# 测试3: 轮询直到完成
echo -e "${YELLOW}[测试3] 等待任务完成 (最多等待5分钟)${NC}"
echo "----------------------------------------"

MAX_WAIT=300  # 最多等待300秒
WAIT_TIME=0

while [ "$STATUS" != "COMPLETED" ] && [ "$STATUS" != "FAILED" ] && [ $WAIT_TIME -lt $MAX_WAIT ]; do
    sleep 5
    WAIT_TIME=$((WAIT_TIME + 5))
    
    PROGRESS_RESPONSE=$(curl -s "$BASE_URL/api/tasks/$SESSION_ID/progress")
    STATUS=$(echo "$PROGRESS_RESPONSE" | jq -r '.data.status')
    PROGRESS=$(echo "$PROGRESS_RESPONSE" | jq -r '.data.progress')
    STAGE=$(echo "$PROGRESS_RESPONSE" | jq -r '.data.currentStage')
    
    echo "[$WAIT_TIME秒] 状态: $STATUS | 进度: $PROGRESS% | $STAGE"
done

if [ "$STATUS" = "COMPLETED" ]; then
    echo -e "${GREEN}✓ 任务完成！${NC}"
elif [ "$STATUS" = "FAILED" ]; then
    echo -e "${RED}❌ 任务失败${NC}"
    ERROR_MSG=$(echo "$PROGRESS_RESPONSE" | jq -r '.data.errorMessage')
    echo "错误信息: $ERROR_MSG"
    exit 1
else
    echo -e "${YELLOW}⚠ 任务超时 (等待超过${MAX_WAIT}秒)${NC}"
fi
echo ""

# 测试4: 获取任务详情
echo -e "${YELLOW}[测试4] 获取任务详情${NC}"
echo "----------------------------------------"

DETAILS_RESPONSE=$(curl -s "$BASE_URL/api/tasks/$SESSION_ID")
echo "$DETAILS_RESPONSE" | jq '.'
echo -e "${GREEN}✓ 详情获取成功${NC}"
echo ""

# 测试5: 获取历史记录
echo -e "${YELLOW}[测试5] 获取历史记录${NC}"
echo "----------------------------------------"

HISTORY_RESPONSE=$(curl -s "$BASE_URL/api/history?page=0&size=5")
echo "$HISTORY_RESPONSE" | jq '.data.content[] | {id, fileName, engine, createdAt}'

TOTAL=$(echo "$HISTORY_RESPONSE" | jq -r '.data.totalElements')
echo -e "${GREEN}✓ 历史记录总数: $TOTAL${NC}"
echo ""

# 测试6: 下载结果（如果任务完成）
if [ "$STATUS" = "COMPLETED" ]; then
    echo -e "${YELLOW}[测试6] 下载翻译结果${NC}"
    echo "----------------------------------------"
    
    # 从历史记录中获取ID
    HISTORY_ID=$(echo "$HISTORY_RESPONSE" | jq -r '.data.content[0].id')
    
    if [ "$HISTORY_ID" != "null" ] && [ -n "$HISTORY_ID" ]; then
        OUTPUT_FILE="translated_output.png"
        
        curl -s -o "$OUTPUT_FILE" "$BASE_URL/api/history/$HISTORY_ID/download"
        
        if [ -f "$OUTPUT_FILE" ]; then
            FILE_SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
            echo -e "${GREEN}✓ 下载成功: $OUTPUT_FILE (大小: $FILE_SIZE)${NC}"
        else
            echo -e "${RED}❌ 下载失败${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ 无法获取历史记录ID${NC}"
    fi
    echo ""
fi

# 测试7: 延长保留期
if [ "$STATUS" = "COMPLETED" ] && [ "$HISTORY_ID" != "null" ]; then
    echo -e "${YELLOW}[测试7] 延长历史记录保留期${NC}"
    echo "----------------------------------------"
    
    EXTEND_RESPONSE=$(curl -s -X POST "$BASE_URL/api/history/$HISTORY_ID/extend?days=60")
    echo "$EXTEND_RESPONSE" | jq '.'
    echo -e "${GREEN}✓ 保留期延长成功${NC}"
    echo ""
fi

# 总结
echo "================================================"
echo -e "${GREEN}   测试完成！${NC}"
echo "================================================"
echo ""
echo "测试摘要:"
echo "  • Session ID: $SESSION_ID"
echo "  • 最终状态: $STATUS"
echo "  • 历史记录数: $TOTAL"
echo ""
echo "更多信息请查看:"
echo "  • README.md - 完整文档"
echo "  • QUICKSTART.md - 快速开始"
echo "  • ARCHITECTURE.md - 架构说明"
echo ""
