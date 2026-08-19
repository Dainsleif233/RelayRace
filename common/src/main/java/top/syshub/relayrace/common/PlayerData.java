package top.syshub.relayrace.common;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import top.syshub.relayrace.common.api.Platform;
import top.syshub.relayrace.common.api.PlayerExtras;

import java.util.ArrayList;
import java.util.Collection;

public class PlayerData {

    private final Location location;
    private final Vector velocity;
    private final ItemStack[] inventoryContents;
    private final ItemStack[] enderChestContents;
    private final double health;
    private final double maxHealth;
    private final double absorptionAmount;
    private final int foodLevel;
    private final float saturation;
    private final int totalExperience;
    private final int remainingAir;
    private final int fireTicks;
    private final float fallDistance;
    private final Collection<PotionEffect> potionEffects;
    private final float walkSpeed;
    private final Location bedSpawnLocation;
    private final int portalCooldown;
    private final boolean glowing;
    private final int heldItemSlot;
    private final ItemStack cursorItem;
    private final int arrowsInBody;
    private final int freezeTicks;

    public PlayerData(Location location, Vector velocity,
                      ItemStack[] inventoryContents, ItemStack[] enderChestContents,
                      double health, double maxHealth, double absorptionAmount,
                      int foodLevel, float saturation, int totalExperience,
                      int remainingAir, int fireTicks, float fallDistance,
                      Collection<PotionEffect> potionEffects,
                      float walkSpeed, Location bedSpawnLocation,
                      int portalCooldown, boolean glowing, int heldItemSlot,
                      ItemStack cursorItem, int arrowsInBody, int freezeTicks) {
        this.location = location;
        this.velocity = velocity;
        this.inventoryContents = inventoryContents;
        this.enderChestContents = enderChestContents;
        this.health = health;
        this.maxHealth = maxHealth;
        this.absorptionAmount = absorptionAmount;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.totalExperience = totalExperience;
        this.remainingAir = remainingAir;
        this.fireTicks = fireTicks;
        this.fallDistance = fallDistance;
        this.potionEffects = potionEffects;
        this.walkSpeed = walkSpeed;
        this.bedSpawnLocation = bedSpawnLocation;
        this.portalCooldown = portalCooldown;
        this.glowing = glowing;
        this.heldItemSlot = heldItemSlot;
        this.cursorItem = cursorItem;
        this.arrowsInBody = arrowsInBody;
        this.freezeTicks = freezeTicks;
    }

    public static PlayerData capture(Player player, Platform platform) {
        PlayerExtras extras = new PlayerExtras();
        platform.capturePlayerExtras(player, extras);

        PlayerData data = new PlayerData(
            player.getLocation().clone(),
            player.getVelocity().clone(),
            player.getInventory().getContents().clone(),
            player.getEnderChest().getContents().clone(),
            player.getHealth(),
            platform.captureMaxHealth(player),
            player.getAbsorptionAmount(),
            player.getFoodLevel(),
            player.getSaturation(),
            platform.captureTotalExperience(player),
            player.getRemainingAir(),
            player.getFireTicks(),
            player.getFallDistance(),
                new ArrayList<>(player.getActivePotionEffects()),
            player.getWalkSpeed(),
            player.getBedSpawnLocation() != null
                ? player.getBedSpawnLocation().clone() : null,
            player.getPortalCooldown(),
            player.isGlowing(),
            player.getInventory().getHeldItemSlot(),
            player.getItemOnCursor().clone(),
            extras.getArrowsInBody(),
            extras.getFreezeTicks()
        );
        reset(player, platform);
        return data;
    }

    public static void reset(Player player, Platform platform) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        player.setLevel(0);
        player.setExp(0);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setVelocity(new Vector(0, 0, 0));
        player.setAbsorptionAmount(0);
        player.setRemainingAir(player.getMaximumAir());
        player.setPortalCooldown(0);
        player.setGlowing(false);
        player.setItemOnCursor(null);
        platform.resetPlayerExtras(player);
    }

    public void apply(Player player, Platform platform) {
        player.getInventory().setContents(inventoryContents);
        player.getEnderChest().setContents(enderChestContents);
        platform.applyMaxHealth(player, maxHealth);
        player.setHealth(Math.min(health, platform.captureMaxHealth(player)));
        player.setAbsorptionAmount(absorptionAmount);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(0);
        player.setExp(0);
        player.giveExp(totalExperience);
        player.setRemainingAir(remainingAir);
        player.setFireTicks(fireTicks);
        player.setFallDistance(fallDistance);
        player.setVelocity(velocity);
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        for (PotionEffect effect : potionEffects) {
            player.addPotionEffect(effect);
        }
        player.setWalkSpeed(walkSpeed);
        if (bedSpawnLocation != null) {
            player.setBedSpawnLocation(bedSpawnLocation, true);
        }
        player.setPortalCooldown(portalCooldown);
        player.setGlowing(glowing);
        player.getInventory().setHeldItemSlot(heldItemSlot);
        player.setItemOnCursor(cursorItem);
        player.teleport(location);

        PlayerExtras extras = new PlayerExtras();
        extras.setArrowsInBody(arrowsInBody);
        extras.setFreezeTicks(freezeTicks);
        platform.applyPlayerExtras(player, extras);
    }
}