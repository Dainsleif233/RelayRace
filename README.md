# RelayRace

接力生存挑战——玩家轮流进入生存，继承状态，直到通关游戏。

[English](README.en.md)

## 命令

所有命令需要 `relayrace.command` 权限（默认 OP）。主命令 `/relayrace`，别名 `/rr`。

| 命令                       | 说明                  |
|--------------------------|---------------------|
| `/rr join <玩家>`          | 将玩家加入游玩队列           |
| `/rr leave <玩家>`         | 将玩家移出游玩队列           |
| `/rr sort`               | 随机打乱游玩队列            |
| `/rr start`              | 开始游戏                |
| `/rr next`               | 强制切换到下一名玩家          |
| `/rr stop`               | 强制结束游戏              |
| `/rr playtime [秒数]`      | 查看或设置每轮时长（默认 300 秒） |
| `/rr loop [true/false]`  | 查看或设置循环模式（默认开启）     |
| `/rr debug [true/false]` | 查看或设置调试模式           |

## 配置

`plugins/RelayRace/config.yml`：

```yaml
time: 300   # 每轮时长（秒）
loop: true  # 循环模式
debug: false # 调试日志
```

## 许可证

本项目使用 [GPL v3](https://www.gnu.org/licenses/gpl-3.0.html) 协议开源。

## 致谢

- 本项目使用 [重庆大学开源软件镜像站](https://mirrors.cqu.edu.cn/#/) 加速构建
- 本项目使用 [shaokeyibb/paper-plugins-skill](https://github.com/shaokeyibb/paper-plugins-skill) 辅助开发
- 本项目使用 [DeepSeek v4 系列模型](https://platform.deepseek.com/usage) 辅助开发
