package me.miminttto.Apocalypse.Abilities;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.miminttto.Apocalypse.ApocalypseAbility;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HungerChain extends ApocalypseAbility {

    private static final String NAME = "HungerChain";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.HungerChain.";

    private double range;
    private long cooldown;
    private double maxDistance;
    private double baseDamage;
    private double distanceMultiplier;
    private long chainDuration;

    private LivingEntity target;
    private Location originalTargetLocation;
    private long startTime;
    private BukkitRunnable chainTask;

    public HungerChain(Player player) {
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
            this.originalTargetLocation = target.getLocation().clone();
            this.startTime = System.currentTimeMillis();

            startChainEffect();
            start();
        }
    }

    private void setFields() {
        this.range = ConfigManager.defaultConfig.get().getDouble(path + "Range", 10.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 12000L);
        this.maxDistance = ConfigManager.defaultConfig.get().getDouble(path + "MaxDistance", 15.0);
        this.baseDamage = ConfigManager.defaultConfig.get().getDouble(path + "BaseDamage", 1.0);
        this.distanceMultiplier = ConfigManager.defaultConfig.get().getDouble(path + "DistanceMultiplier", 0.5);
        this.chainDuration = ConfigManager.defaultConfig.get().getLong(path + "ChainDuration", 10000L);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 0.8f, 0.3f);
    }

    private void startChainEffect() {
        chainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline() || player.isDead()) {
                    finishChain();
                    return;
                }

                if (target == null || target.isDead()) {
                    finishChain();
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime > chainDuration) {
                    finishChain();
                    return;
                }

                // Вычисляем расстояние, на которое цель отошла от исходной позиции
                double distanceMoved = target.getLocation().distance(originalTargetLocation);

                // Применяем эффекты цепей
                applyChainEffects(distanceMoved);

                // Отображаем визуальные эффекты цепей
                displayChains(distanceMoved);

                // Наносим урон в зависимости от расстояния
                if (currentTime % 1000L < 50L) {
                    applyDistanceDamage(distanceMoved);
                }

                // Звуковые эффекты натяжения цепи
                if (distanceMoved > 5 && currentTime % 500L < 50L) {
                    float pitch = (float) (0.5 + (distanceMoved / maxDistance) * 0.5);
                    player.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.3f, pitch);
                }
            }
        };

        chainTask.runTaskTimer(ProjectKorra.plugin, 0L, 20L);

        // Изначальный звук привязки
        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.5f);
    }

    private void applyChainEffects(double distance) {
        // Слабость в зависимости от расстояния
        int weaknessLevel = (int) (distance / 3);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, Math.min(3, weaknessLevel), false, false));

        // Голод
        target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1, false, false));

        // Замедление при попытке отойти
        if (distance > 3) {
            int slownessLevel = (int) ((distance - 3) / 2);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, Math.min(2, slownessLevel), false, false));
        }
    }

    private void applyDistanceDamage(double distance) {
        // Урон увеличивается с расстоянием
        double damage = baseDamage + (distance * distanceMultiplier);
        DamageHandler.damageEntity(target, damage, this);

        // Визуальный эффект при сильном уроне
        if (damage > baseDamage * 2) {
            ParticleEffect.CRIT.display(target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 0.3f);
        }
    }

    private void displayChains(double distance) {
        // Отображаем цепи между исходной позицией и текущей
        Vector direction = target.getLocation().toVector().subtract(originalTargetLocation.toVector());
        double chainLength = direction.length();
        direction.normalize();

        int chainLinks = Math.max(3, (int) (chainLength * 0.8));

        for (int i = 0; i <= chainLinks; i++) {
            double ratio = i / (double) chainLinks;
            Location chainLink = originalTargetLocation.clone().add(direction.clone().multiply(ratio * chainLength));

            // Цепи становятся краснее при натяжении
            float red = (float) Math.min(1.0, distance / maxDistance);
            ParticleEffect.REDSTONE.display(chainLink, 1, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.fromRGB(145, 0, 0), 1.0f));

            // Эффект искр при сильном натяжении
            if (distance > maxDistance * 0.7 && Math.random() < 0.3) {
                ParticleEffect.FLAME.display(chainLink, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Якорь цепи на исходной позиции
        for (int i = 0; i < 4; i++) {
            double angle = 2 * Math.PI * i / 4;
            double radius = 0.3;
            Location anchorPoint = originalTargetLocation.clone().add(
                    Math.cos(angle) * radius,
                    0.1,
                    Math.sin(angle) * radius
            );
            ParticleEffect.SMOKE_NORMAL.display(anchorPoint, 1, 0.05, 0, 0.05, 0.005);
        }

        // Цепи на цели
        for (int i = 0; i < 3; i++) {
            double angle = 2 * Math.PI * i / 3;
            double radius = 0.5;
            Location chainOnTarget = target.getLocation().add(
                    Math.cos(angle) * radius,
                    0.5 + (i * 0.3),
                    Math.sin(angle) * radius
            );
            ParticleEffect.SMOKE_NORMAL.display(chainOnTarget, 1, 0.05, 0.05, 0.05, 0.005);
        }
    }

    private void finishChain() {
        if (chainTask != null) {
            chainTask.cancel();
        }

        // Финальный эффект разрыва цепи
        if (target != null && !target.isDead()) {
            double finalDistance = target.getLocation().distance(originalTargetLocation);
            if (finalDistance > maxDistance) {
                // Разрыв цепи при превышении расстояния
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.8f);
                ParticleEffect.EXPLOSION_NORMAL.display(target.getLocation(), 3, 0.3, 0.5, 0.3, 0.1);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3, false, false));
                player.sendMessage("§cЦепь разорвалась от натяжения!");
            } else {
                // Обычное окончание
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 0.5f, 1.0f);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 0.5f, 0.7f);

        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void progress() {
        // Логика в chainTask
    }

    @Override
    public void remove() {
        super.remove();
        if (chainTask != null) {
            chainTask.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "Связывает цель цепью голода. Чем дальше цель отходит - тем сильнее урон и эффекты.";
    }

    @Override
    public String getInstructions() {
        return "Нажмите ЛКМ по цели";
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
        ConfigManager.defaultConfig.get().addDefault(path + "Range", 10.0);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 12000L);
        ConfigManager.defaultConfig.get().addDefault(path + "MaxDistance", 15.0);
        ConfigManager.defaultConfig.get().addDefault(path + "BaseDamage", 1.0);
        ConfigManager.defaultConfig.get().addDefault(path + "DistanceMultiplier", 0.5);
        ConfigManager.defaultConfig.get().addDefault(path + "ChainDuration", 10000L);
        ConfigManager.defaultConfig.save();
    }
}