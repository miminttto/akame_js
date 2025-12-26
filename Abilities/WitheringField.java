package me.miminttto.Apocalypse.Abilities;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.configuration.ConfigManager;
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
import org.bukkit.scheduler.BukkitRunnable;

public class WitheringField extends ApocalypseAbility {

    private static final String NAME = "WitheringField";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.WitheringField.";

    private double radius;
    private long cooldown;
    private long duration;
    private double damagePerSecond;
    private double speedReduction;
    private double damageReduction;

    private Location center;
    private long startTime;
    private BukkitRunnable fieldTask;

    public WitheringField(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        setFields();

        this.center = player.getLocation();
        this.startTime = System.currentTimeMillis();

        startFieldEffect();
        start();
    }

    private void setFields() {
        this.radius = ConfigManager.defaultConfig.get().getDouble(path + "Radius", 8.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 15000L);
        this.duration = ConfigManager.defaultConfig.get().getLong(path + "Duration", 8000L);
        this.damagePerSecond = ConfigManager.defaultConfig.get().getDouble(path + "DamagePerSecond", 0.5);
        this.speedReduction = ConfigManager.defaultConfig.get().getDouble(path + "SpeedReduction", 0.3);
        this.damageReduction = ConfigManager.defaultConfig.get().getDouble(path + "DamageReduction", 0.4);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.3f);
    }

    private void startFieldEffect() {
        fieldTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline() || player.isDead()) {
                    finishField();
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime > duration) {
                    finishField();
                    return;
                }

                // Обновляем центр поля
                center = player.getLocation();

                // Применяем эффекты к врагам в радиусе
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;

                        // Эффекты голода
                        living.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 40, (int) (speedReduction * 2), false, false));
                        living.addPotionEffect(new PotionEffect(
                                PotionEffectType.WEAKNESS, 40, (int) (damageReduction * 2), false, false));
                        living.addPotionEffect(new PotionEffect(
                                PotionEffectType.HUNGER, 100, 2, false, false));

                        // Периодический урон
                        if (currentTime % 1000L < 50L) {
                            living.damage(damagePerSecond);
                        }
                    }
                }

                // Визуальные эффекты
                displayFieldParticles();

                // Звуковые эффекты
                if (currentTime % 2000L < 50L) {
                    player.getWorld().playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.5f);
                }
            }
        };

        fieldTask.runTaskTimer(ProjectKorra.plugin, 0L, 20L);
    }

    private void displayFieldParticles() {
        // Кольцо частиц по радиусу
        for (int i = 0; i < 16; i++) {
            double angle = 2 * Math.PI * i / 16;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = center.clone().add(x, 0.1, z);
            ParticleEffect.SMOKE_LARGE.display(particleLoc, 1, 0.1, 0, 0.1, 0.02);

            // Плавающие частицы внутри поля
            if (i % 4 == 0) {
                for (int j = 0; j < 3; j++) {
                    Location floatingParticle = center.clone().add(
                            (Math.random() - 0.5) * radius * 1.5,
                            Math.random() * 2.0,
                            (Math.random() - 0.5) * radius * 1.5
                    );
                    ParticleEffect.VILLAGER_ANGRY.display(floatingParticle, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }

        // Центральные частицы
        ParticleEffect.DRAGON_BREATH.display(center.clone().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.05);
    }

    private void finishField() {
        if (fieldTask != null) {
            fieldTask.cancel();
        }

        player.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 0.5f, 0.8f);
        ParticleEffect.EXPLOSION_LARGE.display(center, 1);

        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void progress() {
        // Логика в fieldTask
    }

    @Override
    public void remove() {
        super.remove();
        if (fieldTask != null) {
            fieldTask.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "Создает зону голода, замедляющую и ослабляющую врагов внутри.";
    }

    @Override
    public String getInstructions() {
        return "Нажмите ЛКМ";
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
        return UTILITY;
    }

    @Override
    public Location getLocation() {
        return center;
    }

    @Override
    public void load() {
        ConfigManager.defaultConfig.get().addDefault(path + "Enabled", true);
        ConfigManager.defaultConfig.get().addDefault(path + "Radius", 8.0);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 15000L);
        ConfigManager.defaultConfig.get().addDefault(path + "Duration", 8000L);
        ConfigManager.defaultConfig.get().addDefault(path + "DamagePerSecond", 0.5);
        ConfigManager.defaultConfig.get().addDefault(path + "SpeedReduction", 0.3);
        ConfigManager.defaultConfig.get().addDefault(path + "DamageReduction", 0.4);
        ConfigManager.defaultConfig.save();
    }
}