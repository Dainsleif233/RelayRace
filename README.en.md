# RelayRace

Survival relay challenge — players take turns entering survival, inheriting state, until they clear the game.

[中文](README.md)

## Commands

All commands require the `relayrace.command` permission (default OP). Root command `/relayrace`, alias `/rr`.

| Command | Description |
|------|------|
| `/rr join <player>` | Add player(s) to the queue |
| `/rr leave <player>` | Remove player(s) from the queue |
| `/rr sort` | Randomly shuffle the queue |
| `/rr start` | Start the game (auto-sorts if not sorted) |
| `/rr next` | Force switch to the next player |
| `/rr stop` | Force stop the game |
| `/rr playtime [seconds]` | View or set turn duration (default 300 seconds) |
| `/rr loop [true/false]` | View or set loop mode (default on) |
| `/rr debug [true/false]` | View or set debug mode |

## Configuration

`plugins/RelayRace/config.yml`:

```yaml
time: 300   # turn duration in seconds
loop: true  # loop mode
debug: false # debug logging
```

## License

This project is open-source under the [GPL v3](https://www.gnu.org/licenses/gpl-3.0.html) license.

## Credits

- Build accelerated by [Chongqing University Open Source Mirror](https://mirrors.cqu.edu.cn/#/)
- Project aided by [shaokeyibb/paper-plugins-skill](https://github.com/shaokeyibb/paper-plugins-skill)
- Project aided by [DeepSeek v4 series models](https://platform.deepseek.com/usage)
