package me.miminttto.Apocalypse.Abilities;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.miminttto.Apocalypse.ApocalypseAbility;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class DevourEssence extends ApocalypseAbility {

    private static final String NAME = "DevourEssence";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.DevourEssence.";

    private double range;
    private long cooldown;
    private double damagePerSecond;
    private double healPerSecond;
    private long duration;
    private double leechStrength;

    private LivingEntity target;
    private Location beamStart;
    private Location beamEnd;
    private long startTime;
    private boolean devouring;

    public DevourEssence(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        setFields();

        Entity targetEntity = GeneralMethods.getTargetedEntity(player, range);

        if (targetEntity instanceof LivingEntity && targetEntity.getUniqueId() != player.getUniqueId()) {
            this.target = (LivingEntity) targetEntity;
            this.startTime = System.currentTimeMillis();
            this.devouring = true;
            start();
        }
    }

    private void setFields() {
        this.range = ConfigManager.defaultConfig.get().getDouble(path + "Range", 12.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 10000L);
        this.damagePerSecond = ConfigManager.defaultConfig.get().getDouble(path + "DamagePerSecond", 1.5);
        this.healPerSecond = ConfigManager.defaultConfig.get().getDouble(path + "HealPerSecond", 0.8);
        this.duration = ConfigManager.defaultConfig.get().getLong(path + "Duration", 6000L);
        this.leechStrength = ConfigManager.defaultConfig.get().getDouble(path + "LeechStrength", 2.0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.5f);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (target == null || target.isDead() || !devouring) {
            finishDevour();
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - startTime;

        if (timePassed > duration) {
            finishDevour();
            return;
        }

        if (!target.getWorld().equals(player.getWorld()) ||
                player.getLocation().distance(target.getLocation()) > range) {
            finishDevour();
            return;
        }

        if (!player.isSneaking()) {
            finishDevour();
            return;
        }

        this.beamStart = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
        this.beamEnd = target.getEyeLocation();

        if (timePassed % 1000L < 50L) {
            applyDevourEffects();
        }

        displayDevourBeam();

        if (timePassed % 500L < 50L) {
            player.getWorld().playSound(beamStart, Sound.ENTITY_WITHER_HURT, 0.4f, 0.7f);
            target.getWorld().playSound(beamEnd, Sound.ENTITY_PLAYER_HURT, 0.5f, 0.6f);
        }
    }

    private void applyDevourEffects() {
        // Урон цели и ослабление регенерации
        if (damagePerSecond > 0) {
            DamageHandler.damageEntity(target, damagePerSecond, this);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, false, false));
            target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, (int) leechStrength, false, false));

            // Подавление регенерации
            target.removePotionEffect(PotionEffectType.REGENERATION);
            target.removePotionEffect(PotionEffectType.REGENERATION);
        }

        // Лечение игрока
        if (healPerSecond > 0 && player.getHealth() < player.getMaxHealth()) {
            double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healPerSecond);
            player.setHealth(newHealth);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, false, false));

            // Восстановление голода
            if (player.getFoodLevel() < 20) {
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 2));
            }
        }

        // Эффекты
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 80, 0, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 1, false, false));
    }

    private void displayDevourBeam() {
        if (beamStart == null || beamEnd == null) return;

        Vector direction = beamEnd.toVector().subtract(beamStart.toVector());
        double distance = beamStart.distance(beamEnd);
        int particles = Math.max(5, (int) (distance * 0.4));
        double step = 1.0 / particles;

        for (int i = 0; i <= particles; i++) {
            double ratio = i * step;
            Location particleLoc = beamStart.clone().add(direction.clone().multiply(ratio));

            // Красные частицы голода
            ParticleEffect.DRAGON_BREATH.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.01);

            if (Math.random() < 0.3) {
                ParticleEffect.SMOKE_NORMAL.display(particleLoc, 1, 0.03, 0.03, 0.03, 0.005);
            }
        }

        // Частицы вокруг цели
        for (int i = 0; i < 6; i++) {
            double angle = 2 * Math.PI * i / 6;
            double radius = 0.8;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = target.getLocation().add(x, 1.0, z);
            ParticleEffect.VILLAGER_ANGRY.display(particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void finishDevour() {
        devouring = false;

        if (target != null && !target.isDead()) {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.4f, 1.0f);
            ParticleEffect.LAVA.display(target.getLocation().add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0.05);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 0.5f);
        ParticleEffect.HEART.display(player.getLocation().add(0, 1.5, 0), 4, 0.2, 0.2, 0.2);

        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Высасывает жизненную сущность цели, нанося урон и подавляя восстановление.";
    }

    @Override
    public String getInstructions() {
        return "Зажмите Shift и смотрите на цель";
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ConfigManager.defaultConfig.get().getBoolean(path + "Enabled", true);
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getAbilityType() {
        return ATTACK;
    }

    @Override
    public Location getLocation() {
        return player != null ? player.getLocation() : null;
    }

    @Override
    public void load() {
        ConfigManager.defaultConfig.get().addDefault(path + "Enabled", true);
        ConfigManager.defaultConfig.get().addDefault(path + "Range", 12.0);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 10000L);
        ConfigManager.defaultConfig.get().addDefault(path + "DamagePerSecond", 1.5);
        ConfigManager.defaultConfig.get().addDefault(path + "HealPerSecond", 0.8);
        ConfigManager.defaultConfig.get().addDefault(path + "Duration", 6000L);
        ConfigManager.defaultConfig.get().addDefault(path + "LeechStrength", 2.0);
        ConfigManager.defaultConfig.save();
    }

    @Override
    public void stop() {
    }
}