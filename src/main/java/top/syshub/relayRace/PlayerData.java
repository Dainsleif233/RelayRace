package top.syshub.relayRace;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

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
    private final int arrowsInBody;
    private final int portalCooldown;
    private final int freezeTicks;
    private final boolean glowing;

    public PlayerData(Location location, Vector velocity,
                      ItemStack[] inventoryContents, ItemStack[] enderChestContents,
                      double health, double maxHealth, double absorptionAmount,
                      int foodLevel, float saturation, int totalExperience,
                      int remainingAir, int fireTicks, float fallDistance,
                      Collection<PotionEffect> potionEffects,
                      float walkSpeed, Location bedSpawnLocation,
                      int arrowsInBody, int portalCooldown, int freezeTicks,
                      boolean glowing) {
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
        this.arrowsInBody = arrowsInBody;
        this.portalCooldown = portalCooldown;
        this.freezeTicks = freezeTicks;
        this.glowing = glowing;
    }

    public static PlayerData capture(Player player) {
        PlayerData data = new PlayerData(
            player.getLocation().clone(),
            player.getVelocity().clone(),
            player.getInventory().getContents().clone(),
            player.getEnderChest().getContents().clone(),
            player.getHealth(),
            player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getBaseValue() : 20.0,
            player.getAbsorptionAmount(),
            player.getFoodLevel(),
            player.getSaturation(),
            player.calculateTotalExperiencePoints(),
            player.getRemainingAir(),
            player.getFireTicks(),
            player.getFallDistance(),
            new ArrayList<>(player.getActivePotionEffects()),
            player.getWalkSpeed(),
            player.getBedSpawnLocation() != null
                ? player.getBedSpawnLocation().clone() : null,
            player.getArrowsInBody(),
            player.getPortalCooldown(),
            player.getFreezeTicks(),
            player.isGlowing()
        );
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setLevel(0);
        player.setExp(0);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setVelocity(new Vector(0, 0, 0));
        player.setAbsorptionAmount(0);
        player.setRemainingAir(player.getMaximumAir());
        player.setArrowsInBody(0);
        player.setPortalCooldown(0);
        player.setFreezeTicks(0);
        player.setGlowing(false);
        return data;
    }

    public void apply(Player player) {
        player.getInventory().setContents(inventoryContents);
        player.getEnderChest().setContents(enderChestContents);
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
        }
        player.setHealth(Math.min(health, player.getMaxHealth()));
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
        potionEffects.forEach(e -> player.addPotionEffect(e));
        player.setWalkSpeed(walkSpeed);
        if (bedSpawnLocation != null) {
            player.setBedSpawnLocation(bedSpawnLocation, true);
        }
        player.setArrowsInBody(arrowsInBody);
        player.setPortalCooldown(portalCooldown);
        player.setFreezeTicks(freezeTicks);
        player.setGlowing(glowing);
        player.teleport(location);
    }
}
