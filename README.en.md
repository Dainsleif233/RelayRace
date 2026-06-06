# RelayRace

Survival relay challenge — players take turns entering survival, inheriting state, until they clear the game.

[中文](README.md)

## Commands

All commands require the `relayrace.command` permission (default OP). Root command `/relayrace`, alias `/rr`.

| Command                          | Description                                                  |
|----------------------------------|--------------------------------------------------------------|
| `/rr join <player>`              | Add player(s) to the queue                                   |
| `/rr leave <player>`             | Remove player(s) from the queue                              |
| `/rr sort`                       | Randomly shuffle the queue                                   |
| `/rr start`                      | Start the game (15-second frozen countdown by default)       |
| `/rr next`                       | Force switch to the next player (10-second frozen countdown) |
| `/rr stop`                       | Force stop the game                                          |
| `/rr config playtime [seconds]`  | View or set turn duration (default 300 seconds)              |
| `/rr config loop [true/false]`   | View or set loop mode (default on)                           |
| `/rr config freeze [true/false]` | View or set freeze countdown (default on)                    |
| `/rr config debug [true/false]`  | View or set debug mode                                       |
| `/rr config locales [<locale>]`  | View or set locale (zh / en)                                 |

## Game Mechanics

- **Start countdown**: After `/rr start`, the active player is teleported to the overworld spawn and the server **freezes for 15 seconds**. All players see a gold countdown title (15→1), followed by **GO!** when the game timer begins.
- **Switch countdown**: When switching players (timeout, forced `/rr next`, or End portal win), the new player is set up and the server **freezes for 10 seconds** with a similar countdown, followed by **GO!**.
- **Freeze config**: Use `/rr config freeze false` to disable the countdown freeze. When disabled, game start and player switches skip the countdown and proceed immediately (restoring the earlier behavior).

## Configuration

`plugins/RelayRace/config.yml`:

```yaml
locale: zh # locale (zh / en)
time: 300       # turn duration in seconds
loop: true      # loop mode
freeze: true    # freeze countdown on start/switch
debug: false    # debug logging
```

## License

This project is open-source under the [GPL v3](https://www.gnu.org/licenses/gpl-3.0.html) license.

## Credits

- Build accelerated by [Chongqing University Open Source Mirror](https://mirrors.cqu.edu.cn/#/)
- Project aided by [shaokeyibb/paper-plugins-skill](https://github.com/shaokeyibb/paper-plugins-skill)
- Project aided by [DeepSeek v4 series models](https://platform.deepseek.com/usage)
