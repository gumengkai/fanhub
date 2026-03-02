#!/bin/bash

# FanHub 自动提交脚本
# 每天23点运行，自动提交代码到GitHub

set -e

cd /home/gmk/funhub

echo "=== FanHub Auto Commit $(date) ==="

# 检查是否有变更
git status --porcelain > /tmp/git-status.txt

if [ ! -s /tmp/git-status.txt ]; then
    echo "No changes to commit"
    exit 0
fi

# 生成提交信息
echo "Analyzing changes..."

# 获取变更文件列表
ADDED=$(git status --porcelain | grep "^??" | wc -l)
MODIFIED=$(git status --porcelain | grep "^ M" | wc -l)
DELETED=$(git status --porcelain | grep "^ D" | wc -l)

# 获取具体变更的文件
NEW_FILES=$(git status --porcelain | grep "^??" | awk '{print $2}' | head -10)
CHANGED_FILES=$(git status --porcelain | grep "^ M" | awk '{print $2}' | head -10)

# 生成提交信息
COMMIT_MSG="chore: Auto commit $(date +%Y-%m-%d)

Daily auto-backup of fanhub project

Changes summary:
- Added: $ADDED file(s)
- Modified: $MODIFIED file(s)
- Deleted: $DELETED file(s)
"

# 如果有新文件，添加到提交信息
if [ -n "$NEW_FILES" ]; then
    COMMIT_MSG="$COMMIT_MSG
New files:
$NEW_FILES
"
fi

# 添加所有变更
git add -A

# 提交
git commit -m "$COMMIT_MSG"

# 推送到GitHub
git push origin master

echo "=== Commit completed successfully ==="
