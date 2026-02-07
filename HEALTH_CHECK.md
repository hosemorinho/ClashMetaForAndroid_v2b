# Build Health Check & Auto-Fix Script

自动监控、诊断和修复 GitHub Actions 构建错误的脚本。

## 功能

✅ **自动监控** - 持续监控最新的 GitHub Actions 构建
✅ **错误诊断** - 分析构建日志并识别常见错误
✅ **自动修复** - 自动修复已知的编译问题
✅ **自动推送** - 提交修复并推送到仓库
✅ **循环重试** - 重复上述流程，直到构建成功

## 前置条件

1. **GitHub CLI 已安装并认证**
   ```bash
   gh auth login
   ```

2. **Git 已配置并连接到仓库**
   ```bash
   git remote -v  # 验证 v2board 远程存在
   ```

3. **脚本可执行**
   ```bash
   chmod +x health-check.sh
   ```

## 使用方法

### 方式 1: 直接运行

```bash
cd /home/ClashMetaForAndroid
./health-check.sh
```

### 方式 2: 后台运行（推荐）

```bash
nohup ./health-check.sh > health-check.log 2>&1 &
tail -f health-check.log  # 实时查看日志
```

### 方式 3: 使用 screen 或 tmux

```bash
screen -S health-check
./health-check.sh

# 或 tmux
tmux new-session -d -s health-check './health-check.sh'
tmux attach -t health-check
```

## 脚本功能说明

### 监控流程

1. **获取最新构建** - 从 GitHub 获取最新的 Actions 运行
2. **检查状态** - 判断构建是否成功、失败或进行中
3. **失败分析** - 提取构建日志并识别错误
4. **自动修复** - 应用已知的修复方案
5. **提交推送** - 将修复提交到仓库
6. **等待新构建** - 等待 GitHub Actions 重新构建
7. **重复** - 回到步骤 1，最多重试 5 次

### 支持的自动修复

| 错误类型 | 修复方法 |
|---------|--------|
| 字符串插值语法错误 | 移除插值表达式中的额外空格 |
| Public inline 函数访问非public属性 | 改为 internal inline 函数 |
| 其他编译错误 | 标记为需要手动审查 |

## 配置

编辑脚本中的以下变量来自定义行为：

```bash
REPO="hosemorinho/ClashMetaForAndroid_v2b"  # GitHub 仓库
BRANCH="main"                                # 监控的分支
MAX_RETRIES=5                               # 最大重试次数
CHECK_INTERVAL=60                           # 检查间隔（秒）
```

## 输出示例

```
[INFO] ==========================================
[INFO] ClashMetaForAndroid V2B Health Check
[INFO] ==========================================
[INFO] Repository: hosemorinho/ClashMetaForAndroid_v2b
[INFO] Branch: main
[INFO] Max retries: 5

[INFO] Attempt 1/5
[INFO] Fetching latest workflow run...
[INFO] Run ID: 21782022271 | Status: completed | Conclusion: failure
[!] Build failed, analyzing...
[!] Detected: Public inline function accessing non-public property
[✓] Fixed inline function visibility
[INFO] Changes detected, committing...
[INFO] Pushing to v2board remote...
[✓] Changes pushed successfully
[INFO] Waiting for GitHub Actions to start new build...
[✓] New build detected!

[INFO] Attempt 2/5
...
[✓] ✓ BUILD SUCCESSFUL!
[✓] Repository: https://github.com/hosemorinho/ClashMetaForAndroid_v2b
[✓] Run: https://github.com/hosemorinho/ClashMetaForAndroid_v2b/actions/runs/...
```

## 日志文件

脚本在后台运行时，所有输出都写入 `health-check.log`：

```bash
# 查看最后 50 行
tail -50 health-check.log

# 实时跟踪
tail -f health-check.log

# 搜索错误
grep "ERROR\|error\|FAILED" health-check.log
```

## 停止脚本

### 如果在前台运行
```bash
Ctrl+C
```

### 如果在后台运行
```bash
# 查找进程
ps aux | grep health-check.sh

# 杀死进程
kill <PID>

# 或使用 screen/tmux
screen -X -S health-check quit
tmux kill-session -t health-check
```

## 故障排查

### "gh: command not found"
```bash
# 安装 GitHub CLI
brew install gh  # macOS
apt-get install gh  # Ubuntu/Debian
```

### "Permission denied"
```bash
chmod +x health-check.sh
```

### "Not authenticated with GitHub"
```bash
gh auth login
# 按照提示进行认证
```

### 脚本无法检测到新构建
- 检查 GitHub Actions 工作流是否配置为 `push` 触发
- 验证分支是否为 `main`
- 检查 GitHub API 速率限制

## 限制

⚠️ **已知限制：**

1. **自动修复范围有限** - 仅支持常见的编译问题
2. **复杂错误需要手动** - 逻辑错误、算法问题等需要人工审查
3. **GitHub API 速率限制** - 频繁请求可能受限
4. **无法修复 Android SDK 问题** - 需要在 GitHub Actions 中配置 SDK

## 示例：完整的自动修复流程

```bash
# 1. 启动健康检查脚本
cd /home/ClashMetaForAndroid
nohup ./health-check.sh > health-check.log 2>&1 &

# 2. 实时监控日志
tail -f health-check.log

# 3. 脚本会自动：
#    - 检测构建失败
#    - 修复编译错误
#    - 提交和推送修复
#    - 等待新构建
#    - 重复，直到成功

# 4. 构建成功后脚本会输出：
#    [✓] ✓ BUILD SUCCESSFUL!

# 5. 停止脚本（如果需要）
pkill -f health-check.sh
```

## 环境变量

可以通过环境变量自定义脚本行为：

```bash
# 自定义 GitHub 仓库
REPO="your-org/your-repo" ./health-check.sh

# 自定义分支
BRANCH="develop" ./health-check.sh

# 自定义重试次数
MAX_RETRIES=10 ./health-check.sh
```

## 贡献

如果发现新的可自动修复的错误模式，可以：

1. 在脚本中添加新的错误检测
2. 实现对应的自动修复逻辑
3. 提交 PR 或 Issue

## 许可证

同项目主许可证 (GPLv3)

---

**需要帮助？**
- 查看脚本中的注释
- 检查 `health-check.log` 了解执行细节
- 访问 GitHub Actions 页面查看完整构建日志
