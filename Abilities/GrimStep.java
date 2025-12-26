package me.miminttto.Apocalypse.Abilities;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.configuration.ConfigManager;
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

public class GrimStep extends ApocalypseAbility {

    private static final String NAME = "GrimStep";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.GrimStep.";

    private double range;
    private long cooldown;
    private double slowDuration;
    private double slowStrength;
    private int particleCount;

    private Location teleportDestination;

    public GrimStep(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        setFields();

        // Определяем точку телепорта
        this.teleportDestination = getTeleportLocation();

        if (teleportDestination != null) {
            performGrimStep();
            start();
        }
    }

    private void setFields() {
        this.range = ConfigManager.defaultConfig.get().getDouble(path + "Range", 15.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 8000L);
        this.slowDuration = ConfigManager.defaultConfig.get().getDouble(path + "SlowDuration", 3.0);
        this.slowStrength = ConfigManager.defaultConfig.get().getDouble(path + "SlowStrength", 0.5);
        this.particleCount = ConfigManager.defaultConfig.get().getInt(path + "ParticleCount", 30);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.3f);
    }

    private Location getTeleportLocation() {
        Location targetLocation = GeneralMethods.getTargetedLocation(player, range);

        // Проверяем, можно ли телепортироваться в эту точку
        if (isSafeLocation(targetLocation)) {
            return targetLocation;
        }

        // Ищем безопасное место поблизости
        for (int y = -2; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location checkLocation = targetLocation.clone().add(x, y, z);
                    if (isSafeLocation(checkLocation)) {
                        return checkLocation;
                    }
                }
            }
        }

        player.sendMessage("§cНе удалось найти безопасное место для телепорта!");
        return null;
    }

    private boolean isSafeLocation(Location location) {
        // Проверяем, что блок под ногами твердый
        if (!GeneralMethods.isSolid(location.clone().subtract(0, 1, 0).getBlock())) {
            return false;
        }

        // Проверяем, что место для появления свободно (2 блока высоты)
        if (GeneralMethods.isSolid(location.getBlock()) ||
                GeneralMethods.isSolid(location.clone().add(0, 1, 0).getBlock())) {
            return false;
        }

        return true;
    }

    private void performGrimStep() {
        Location startLocation = player.getLocation().clone();

        // Эффект исчезновения
        displayDepartureEffects(startLocation);

        // Пауза перед телепортом
        ProjectKorra.plugin.getServer().getScheduler().runTaskLater(ProjectKorra.plugin, () -> {
            // Телепортация
            player.teleport(teleportDestination);

            // Эффект появления
            displayArrivalEffects(teleportDestination);

            // Замедление ближайших врагов
            slowNearbyEnemies(teleportDestination);

            // Финальные эффекты
            finishGrimStep();

        }, 10L);
    }

    private void displayDepartureEffects(Location location) {
        // Черная дыра исчезновения
        for (int i = 0; i < particleCount; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = Math.random() * 1.0;
            double height = Math.random() * 2.0;

            Location particleLoc = location.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
            );

            // Частицы втягиваются в центр
            Vector toCenter = location.toVector().subtract(particleLoc.toVector()).normalize().multiply(0.1);
            ParticleEffect.SMOKE_LARGE.display(particleLoc, 1,
                    (float) toCenter.getX(), (float) toCenter.getY(), (float) toCenter.getZ(), 0.02);
        }

        // Звук исчезновения
        player.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.2f);

        // Эффект на игроке перед телепортом
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10, false, false));
    }

    private void displayArrivalEffects(Location location) {
        // Взрыв тени при появлении
        ParticleEffect.EXPLOSION_LARGE.display(location, 1);

        // Кольцо теней
        for (int i = 0; i < 16; i++) {
            double angle = 2 * Math.PI * i / 16;
            double radius = 1.5;

            Location ringParticle = location.clone().add(
                    Math.cos(angle) * radius,
                    0.2,
                    Math.sin(angle) * radius
            );

            ParticleEffect.SMOKE_LARGE.display(ringParticle, 2, 0.1, 0.1, 0.1, 0.05);

            // Второе кольцо выше
            Location upperRing = location.clone().add(
                    Math.cos(angle) * radius * 0.7,
                    1.0,
                    Math.sin(angle) * radius * 0.7
            );
            ParticleEffect.SPELL_WITCH.display(upperRing, 1, 0.1, 0.1, 0.1, 0.03);
        }

        // Столб теней
        for (double y = 0; y < 2.5; y += 0.3) {
            for (int i = 0; i < 4; i++) {
                double angle = 2 * Math.PI * i / 4;
                double radius = 0.5;

                Location columnParticle = location.clone().add(
                        Math.cos(angle) * radius,
                        y,
                        Math.sin(angle) * radius
                );

                ParticleEffect.SMOKE_NORMAL.display(columnParticle, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Звук появления
        player.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
        player.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.8f);

        // Эффект на игроке после появления
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, false, false));
    }

    private void slowNearbyEnemies(Location center) {
        double slowRadius = 4.0;

        for (Entity entity : center.getWorld().getNearbyEntities(center, slowRadius, slowRadius, slowRadius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;

                // Применяем замедление
                int slownessLevel = (int) (slowStrength * 2);
                living.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        (int) (slowDuration * 20),
                        slownessLevel, false, false
                ));

                // Слабость
                living.addPotionEffect(new PotionEffect(
                        PotionEffectType.WEAKNESS,
                        (int) (slowDuration * 20),
                        0, false, false
                ));

                // Визуальный эффект замедления
                displaySlowEffect(living.getLocation());

                // Звуковой эффект
                living.getWorld().playSound(living.getLocation(),
                        Sound.ENTITY_WITHER_HURT, 0.3f, 0.5f);
            }
        }

        // Сообщение игроку
        player.sendMessage("§8Враги вокруг замедлены на §7" + String.format("%.1f", slowDuration) + "§8 сек");
    }

    private void displaySlowEffect(Location location) {
        // Синие частицы замедления
        for (int i = 0; i < 3; i++) {
            Location particleLoc = location.clone().add(
                    (Math.random() - 0.5) * 0.5,
                    Math.random() * 1.5,
                    (Math.random() - 0.5) * 0.5
            );

            ParticleEffect.SPELL_MOB.display(particleLoc, 1, 0.1, 0.1, 0.1,
                    new Particle.DustOptions(Color.fromRGB(0, 0, 150), 1.0f));
        }

        // Кольцо на земле
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            double radius = 0.7;

            Location groundRing = location.clone().add(
                    Math.cos(angle) * radius,
                    -0.1,
                    Math.sin(angle) * radius
            );

            ParticleEffect.SMOKE_NORMAL.display(groundRing, 1, 0.05, 0, 0.05, 0.005);
        }
    }

    private void finishGrimStep() {
        // Эффект восстановления
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 20) {
                    cancel();
                    return;
                }

                // Исчезающие частицы восстановления
                if (ticks % 5 == 0) {
                    for (int i = 0; i < 2; i++) {
                        Location recoveryParticle = player.getLocation().add(
                                (Math.random() - 0.5) * 1.0,
                                Math.random() * 1.0,
                                (Math.random() - 0.5) * 1.0
                        );
                        ParticleEffect.HEART.display(recoveryParticle, 1, 0.1, 0.1, 0.1);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(ProjectKorra.plugin, 0L, 1L);

        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void progress() {
        remove();
    }

    @Override
    public String getDescription() {
        return "Телепортируется сквозь тень смерти, замедляя врагов вокруг точки появления.";
    }

    @Override
    public String getInstructions() {
        return "Нажмите ЛКМ по месту телепорта";
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
        return MOVEMENT;
    }

    @Override
    public Location getLocation() {
        return teleportDestination != null ? teleportDestination : player.getLocation();
    }

    @Override
    public void load() {
        ConfigManager.defaultConfig.get().addDefault(path + "Enabled", true);
        ConfigManager.defaultConfig.get().addDefault(path + "Range", 15.0);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 8000L);
        ConfigManager.defaultConfig.get().addDefault(path + "SlowDuration", 3.0);
        ConfigManager.defaultConfig.get().addDefault(path + "SlowStrength", 0.5);
        ConfigManager.defaultConfig.get().addDefault(path + "ParticleCount", 30);
        ConfigManager.defaultConfig.save();
    }
}