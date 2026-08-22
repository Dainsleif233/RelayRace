# RelayRace

接力生存挑战——玩家轮流进入生存，继承状态，直到通关游戏。

[English](README.en.md)

## 命令

所有命令需要 `relayrace.command` 权限（默认 OP）。主命令 `/relayrace`，别名 `/rr`。

| 命令                                     | 说明                                         |
|------------------------------------------|----------------------------------------------|
| `/rr join <玩家>`                        | 将玩家加入游玩队列                           |
| `/rr leave <玩家>`                       | 将玩家移出游玩队列                           |
| `/rr sort`                               | 随机打乱游玩队列                             |
| `/rr start`                              | 开始游戏（默认冻结 15 秒倒计时）             |
| `/rr next`                               | 强制切换到下一名玩家（默认冻结 10 秒倒计时） |
| `/rr stop`                               | 强制结束游戏                                 |
| `/rr config playtime [秒数]`             | 查看或设置每轮时长（默认 300 秒）            |
| `/rr config loop [true/false]`           | 查看或设置循环模式（默认开启）               |
| `/rr config freeze [true/false]`         | 查看或设置冻结冷却（默认开启）               |
| `/rr config debug [true/false]`          | 查看或设置调试模式                           |
| `/rr config locales [<语言>]`            | 查看或设置语言（zh / en）                    |
| `/rr config externallobby [true/false]`  | 查看或设置外部大厅模式（默认关闭）           |
| `/rr config externallobby-server <名称>` | 查看或设置外部大厅服务器名                   |

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

启用外部大厅时（`external-lobby: true`），轮到外部大厅的玩家时，系统会自动将其召回游戏服务器。

## 里程碑计分板

游戏开始后（命令 `/rr start`），所有玩家侧边栏会出现一块**里程碑**计分板，记录接力过程中的关键节点。游戏结束（自然结束或 `/rr stop`）后计分板不会消失，直到下一局开始才重置。

计分板内容：

- **当前玩家**：当前正在操作的活跃玩家
- **当前进度**：当前阶段状态，依次覆盖、可跳过（已进入堡垒遗迹与已进入下界要塞可互换）
- **里程碑列表**：每达成一个节点逐行出现，显示标签、达成玩家与当时总游玩时长

| 里程碑 | 判定条件 | 显示行 | 状态变化 |
|--------|----------|--------|----------|
| 开始游戏 | 游戏开始（`/rr start`） | ✓ | 已开始游戏 |
| 进入下界 | 活跃玩家到达下界维度 | ✓ | 已进入下界 |
| 抵达堡垒遗迹 | 活跃玩家获得进度「光辉岁月」(`nether/find_bastion`) | ✓ | 已进入堡垒遗迹 |
| 抵达下界要塞 | 活跃玩家获得进度「阴森的要塞」(`nether/find_fortress`) | ✓ | 已进入下界要塞 |
| 前往要塞中 | 活跃玩家成功丢出末影之眼 | ✗ | 前往要塞中 |
| 抵达要塞 | 活跃玩家获得进度「隔墙有眼」(`story/follow_ender_eye`) | ✓ | 已进入要塞 |
| 进入末地 | 活跃玩家到达末地维度 | ✓ | 已进入末地 |
| 击败末影龙 | 末影龙被击杀（任意来源） | ✓ | — |
| 通关游戏 | 通关游戏（`winGame`） | ✓ | — |

> 注：击败末影龙不更新「当前进度」状态（进度状态止于「已进入末地」），仅新增显示行；通关游戏会将状态更新为「已通关游戏」。

## 许可证

本项目使用 [GPL v3](https://www.gnu.org/licenses/gpl-3.0.html) 协议开源。

## 致谢

- 本项目使用 [重庆大学开源软件镜像站](https://mirrors.cqu.edu.cn/#/) 加速构建
- 本项目使用 [shaokeyibb/paper-plugins-skill](https://github.com/shaokeyibb/paper-plugins-skill) 辅助开发
- 本项目使用 [DeepSeek v4 系列模型](https://platform.deepseek.com/usage) 辅助开发
