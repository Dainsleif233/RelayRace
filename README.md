# RelayRace

接力生存挑战——玩家轮流进入生存，继承状态，直到通关游戏。

[English](README.en.md)

## 命令

所有命令需要 `relayrace.command` 权限（默认 OP）。主命令 `/relayrace`，别名 `/rr`。

| 命令                               | 说明                       |
|----------------------------------|--------------------------|
| `/rr join <玩家>`                  | 将玩家加入游玩队列                |
| `/rr leave <玩家>`                 | 将玩家移出游玩队列                |
| `/rr sort`                       | 随机打乱游玩队列                 |
| `/rr start`                      | 开始游戏（默认冻结 15 秒倒计时）       |
| `/rr next`                       | 强制切换到下一名玩家（默认冻结 10 秒倒计时） |
| `/rr stop`                       | 强制结束游戏                   |
| `/rr config playtime [秒数]`       | 查看或设置每轮时长（默认 300 秒）      |
| `/rr config loop [true/false]`   | 查看或设置循环模式（默认开启）          |
| `/rr config freeze [true/false]` | 查看或设置冻结冷却（默认开启）          |
| `/rr config debug [true/false]`  | 查看或设置调试模式                |
| `/rr config locales [<语言>]`      | 查看或设置语言（zh / en）         |
| `/rr config externallobby [true/false]` | 查看或设置外部大厅模式（默认关闭） |
| `/rr config externallobby-server <名称>` | 查看或设置外部大厅服务器名 |

## 游戏机制

- **开始倒计时**：`/rr start` 后，当前玩家被传送至主世界出生点，服务器将 **冻结 15 秒**，所有玩家屏幕显示金色数字倒计时（15→1），结束后显示 **GO!** 并开始计时。
- **切换倒计时**：切换玩家时（超时、强制 `/rr next`、主动退出 End 传送门获胜），新玩家就绪后服务器将 **冻结 10 秒**，同样显示数字倒计时，结束后显示 **GO!**。
- **冻结冷却**：可通过 `/rr config freeze false` 关闭冻结冷却。关闭后开始游戏和切换玩家时将跳过倒计时，立即开始计时或行动（恢复为早期版本行为）。

## 配置

`plugins/RelayRace/config.yml`：

```yaml
locale: zh               # 语言（zh / en）
time: 300                # 每轮时长（秒）
loop: true               # 循环模式
freeze: true             # 冻结冷却（开始/切换时冻结倒计时）
debug: false             # 调试日志
external-lobby: false    # 外部大厅模式
external-lobby-server: "" # 外部大厅服务器名（Velocity 子服务器名）
```

## 外部大厅

启用外部大厅时（`external-lobby: true`），等待玩家会被发送到 Velocity 代理下的另一个子服务器（如主城服务器），而不是停留在游戏服务器的本地大厅世界。需要：

- 在 Velocity 的 `velocity.toml` 中启用 BungeeCord 插件消息转发
- 正确配置 `external-lobby-server`（大厅服务器名）
- 服务器名称由第一名加入的玩家自动检测，无需手动配置
- 当前回合时间到达 100%、60%、20%、10s 时，下一个等待的玩家会收到聊天提醒
- 轮到外部大厅的玩家时，系统会自动将其召回游戏服务器。若 30 秒内无法召回，则跳过该玩家

## 许可证

本项目使用 [GPL v3](https://www.gnu.org/licenses/gpl-3.0.html) 协议开源。

## 致谢

- 本项目使用 [重庆大学开源软件镜像站](https://mirrors.cqu.edu.cn/#/) 加速构建
- 本项目使用 [shaokeyibb/paper-plugins-skill](https://github.com/shaokeyibb/paper-plugins-skill) 辅助开发
- 本项目使用 [DeepSeek v4 系列模型](https://platform.deepseek.com/usage) 辅助开发
