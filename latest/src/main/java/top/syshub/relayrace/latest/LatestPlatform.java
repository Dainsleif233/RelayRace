package top.syshub.relayrace.latest;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

import top.syshub.relayrace.common.GameManager;
import top.syshub.relayrace.common.RelayRacePlugin;
import top.syshub.relayrace.common.api.CommandRegistrar;
import top.syshub.relayrace.common.api.Platform;
import top.syshub.relayrace.common.api.PlayerExtras;
import top.syshub.relayrace.common.api.PlayerUi;
import top.syshub.relayrace.common.api.Scheduler;
import top.syshub.relayrace.common.api.TickControl;
import top.syshub.relayrace.common.api.WorldFactory;

public final class LatestPlatform implements Platform {

    private final Scheduler scheduler = new LatestScheduler();
    private final TickControl tickControl = new LatestTickControl();
    private final WorldFactory worldFactory = new LatestWorldFactory();
    private final PlayerUi ui = new LatestPlayerUi();
    private final CommandRegistrar commands = new LatestCommandHandler();

    public LatestPlatform() {}

    @Override
    public String id() {
        return "latest";
    }

    @Override
    public CommandRegistrar commands() {
        return commands;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public TickControl tickControl() {
        return tickControl;
    }

    @Override
    public WorldFactory worldFactory() {
        return worldFactory;
    }

    @Override
    public PlayerUi ui() {
        return ui;
    }

    @Override
    public void setHasSeenWinScreen(Player player, boolean value) {
        player.setHasSeenWinScreen(value);
    }

    @Override
    public void capturePlayerExtras(Player player, PlayerExtras extras) {
        extras.setArrowsInBody(player.getArrowsInBody());
        extras.setFreezeTicks(player.getFreezeTicks());
    }

    @Override
    public void applyPlayerExtras(Player player, PlayerExtras extras) {
        player.setArrowsInBody(extras.getArrowsInBody());
        player.setFreezeTicks(extras.getFreezeTicks());
    }

    @Override
    public void resetPlayerExtras(Player player) {
        player.setArrowsInBody(0);
        player.setFreezeTicks(0);
    }

    @Override
    public int captureTotalExperience(Player player) {
        return player.calculateTotalExperiencePoints();
    }

    @Override
    public void setRespawnLocation(Player player, Location location) {
        player.setRespawnLocation(location, true);
    }

    @Override
    public double captureMaxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getBaseValue() : 20.0;
    }

    @Override
    public void applyMaxHealth(Player player, double maxHealth) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(maxHealth);
        }
    }

    @Override
    public void registerVersionEvents(RelayRacePlugin plugin, GameManager gameManager) {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEntitiesLoad(EntitiesLoadEvent event) {
                gameManager.onEntitiesLoad(event.getEntities());
            }
        }, plugin);
    }
}
