# RelayRace

Survival relay challenge — players take turns entering survival, inheriting state, until they clear the game.

[中文](README.md)

## Commands

All commands require the `relayrace.command` permission (default OP). Root command `/relayrace`, alias `/rr`.

| Command                                  | Description                                                  |
|------------------------------------------|--------------------------------------------------------------|
| `/rr join <player>`                      | Add player(s) to the queue                                   |
| `/rr leave <player>`                     | Remove player(s) from the queue                              |
| `/rr sort`                               | Randomly shuffle the queue                                   |
| `/rr start`                              | Start the game (15-second frozen countdown by default)       |
| `/rr next`                               | Force switch to the next player (10-second frozen countdown) |
| `/rr stop`                               | Force stop the game                                          |
| `/rr config playtime [seconds]`          | View or set turn duration (default 300 seconds)              |
| `/rr config loop [true/false]`           | View or set loop mode (default on)                           |
| `/rr config freeze [true/false]`         | View or set freeze countdown (default on)                    |
| `/rr config debug [true/false]`          | View or set debug mode                                       |
| `/rr config locales [<locale>]`          | View or set locale (zh / en)                                 |
| `/rr config externallobby [true/false]`  | View or set external lobby mode (default off)                |
| `/rr config externallobby-server <name>` | View or set the external lobby server name                   |

## Configuration

`plugins/RelayRace/config.yml`:

```yaml
locale: zh               # locale (zh / en)
time: 300                # turn duration in seconds
loop: true               # loop mode
freeze: true             # freeze countdown on start/switch
debug: false             # debug logging
external-lobby: false    # external lobby mode
external-lobby-server: "" # external lobby server (Velocity sub-server name)
```

## External Lobby

When external lobby mode is enabled (`external-lobby: true`), when it's an external lobby player's turn, they are automatically recalled to the game server.

## Milestone Scoreboard

When the game starts (`/rr start`), a **Milestones** scoreboard appears on every player's sidebar, recording the key relay checkpoints. The board does not disappear when the game ends (naturally or via `/rr stop`) — it persists until the next game resets it.

Board contents:

- **Current player**: the active player currently playing
- **Current progress**: the current stage status, sequentially overwritten, skippable (entering bastion remnant and entering nether fortress are interchangeable)
- **Milestone list**: one line per achieved checkpoint, showing its label, the achieving player, and the total play time at that moment

| Milestone | Trigger | Display line | Status change |
|-----------|---------|--------------|---------------|
| Start game | Game starts (`/rr start`) | ✓ | Game started |
| Enter nether | Active player reaches the Nether | ✓ | Entered nether |
| Reach bastion remnant | Active player gets the "光辉岁月" (`nether/find_bastion`) advancement | ✓ | Entered bastion remnant |
| Reach nether fortress | Active player gets the "阴森的要塞" (`nether/find_fortress`) advancement | ✓ | Entered nether fortress |
| Heading to stronghold | Active player successfully throws an ender eye | ✗ | Heading to stronghold |
| Reach stronghold | Active player gets the "隔墙有眼" (`story/follow_ender_eye`) advancement | ✓ | Entered stronghold |
| Enter end | Active player reaches the End | ✓ | Entered end |
| Defeat ender dragon | Ender dragon killed (any source) | ✓ | — |
| Clear game | Game cleared (`winGame`) | ✓ | — |

> Note: Defeating the ender dragon does not change the "Current progress" status (status stops at "Entered end"); it only adds a display line. Clearing the game updates the status to "Game cleared".

## License

This project is open-source under the [GPL v3](https://www.gnu.org/licenses/gpl-3.0.html) license.

## Credits

- Build accelerated by [Chongqing University Open Source Mirror](https://mirrors.cqu.edu.cn/#/)
- Project aided by [shaokeyibb/paper-plugins-skill](https://github.com/shaokeyibb/paper-plugins-skill)
- Project aided by [DeepSeek v4 series models](https://platform.deepseek.com/usage)
