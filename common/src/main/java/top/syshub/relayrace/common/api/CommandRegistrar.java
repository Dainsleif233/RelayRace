package top.syshub.relayrace.common.api;

import top.syshub.relayrace.common.GameManager;
import top.syshub.relayrace.common.RelayRacePlugin;

public interface CommandRegistrar {

    void register(RelayRacePlugin plugin, GameManager gameManager);
}
