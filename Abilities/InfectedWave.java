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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class InfectedWave extends ApocalypseAbility {

    private static final String NAME = "InfectedWave";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.InfectedWave.";

    private double range;
    private long cooldown;
    private double waveWidth;
    private double waveLength;
    private double infectionDuration;
    private double defenseReduction;

    private Location waveStart;
    private Vector waveDirection;
    private BukkitRunnable waveTask;

    public InfectedWave(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        setFields();

        this.waveStart = player.getEyeLocation();
        this.waveDirection = player.getLocation().getDirection().normalize();

        launchInfectedWave();
        start();
    }

    private void setFields() {
        this.range = ConfigManager.defaultConfig.get().getDouble(path + "Range", 12.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 14000L);
        this.waveWidth = ConfigManager.defaultConfig.get().getDouble(path + "WaveWidth", 3.0);
        this.waveLength = ConfigManager.defaultConfig.get().getDouble(path + "WaveLength", 15.0);
        this.infectionDuration = ConfigManager.defaultConfig.get().getDouble(path + "InfectionDuration", 8.0);
        this.defenseReduction = ConfigManager.defaultConfig.get().getDouble(path + "DefenseReduction", 0.3);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1.0f, 0.2f);
    }

    private void launchInfectedWave() {
        waveTask = new BukkitRunnable() {
            private double distanceTraveled = 0;
            private final double waveSpeed = 1.5;

            @Override
            public void run() {
                if (player == null || !player.isOnline() || player.isDead()) {
                    finishWave();
                    return;
                }

                if (distanceTraveled > waveLength) {
                    finishWave();
                    return;
                }

                // Вычисляем текущую позицию волны
                Location currentWaveFront = waveStart.clone().add(
                        waveDirection.clone().multiply(distanceTraveled)
                );

                // Поражаем врагов в текущей позиции волны
                affectEntitiesInWave(currentWaveFront);

                // Отображаем визуальные эффекты волны
                displayWaveEffects(currentWaveFront, distanceTraveled);

                // Звуковые эффекты движения волны
                if (distanceTraveled % 2 < 0.2) {
                    player.getWorld().playSound(currentWaveFront, Sound.ENTITY_ZOMBIE_AMBIENT, 0.3f,
                            (float) (0.5 + (distanceTraveled / waveLength) * 0.5));
                }

                distanceTraveled += waveSpeed;
            }
        };

        waveTask.runTaskTimer(ProjectKorra.plugin, 0L, 2L);
    }

    private void affectEntitiesInWave(Location waveLocation) {
        // Создаем область поражения волны
        for (Entity entity : waveLocation.getWorld().getNearbyEntities(waveLocation, waveWidth, waveWidth, waveWidth)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;

                // Проверяем, находится ли сущность в пределах волны
                Vector toEntity = entity.getLocation().toVector().subtract(waveLocation.toVector());
                double lateralDistance = toEntity.clone().setY(0).length();

                if (lateralDistance <= waveWidth / 2) {
                    // Наносим эффекты заражения
                    applyInfectionEffects(living);

                    // Визуальный эффект поражения
                    ParticleEffect.SLIME.display(entity.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.05);
                }
            }
        }
    }

    private void applyInfectionEffects(LivingEntity entity) {
        // Снижение защиты
        entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                (int) (infectionDuration * 20),
                (int) (defenseReduction * 3),
                false, false));

        // Отравление
        entity.addPotionEffect(new PotionEffect(PotionEffectType.POISON,
                (int) (infectionDuration * 10),
                1, false, false));

        // Замедление
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                (int) (infectionDuration * 10),
                1, false, false));

        // Нанесение начального урона
        DamageHandler.damageEntity(entity, 2.0, this);

        // Звук заражения
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 0.5f, 0.7f);
    }

    private void displayWaveEffects(Location waveFront, double distance) {
        // Зеленая волна заражения
        for (int i = 0; i < 12; i++) {
            double angle = 2 * Math.PI * i / 12;
            double offsetX = Math.cos(angle) * (waveWidth / 2);
            double offsetZ = Math.sin(angle) * (waveWidth / 2);

            Location particleLoc = waveFront.clone().add(offsetX, 0.2, offsetZ);

            // Зеленые частицы чумы
            ParticleEffect.VILLAGER_HAPPY.display(particleLoc, 1, 0.1, 0.1, 0.1, 0.02);

            // Туман за волной
            if (distance > 2) {
                Location trailLoc = waveFront.clone().add(
                        waveDirection.clone().multiply(-1).multiply(Math.random() * 2)
                ).add(offsetX * 0.7, 0.5, offsetZ * 0.7);
                ParticleEffect.SPELL_MOB_AMBIENT.display(trailLoc, 1, 0.2, 0.3, 0.2, 0.01);
            }
        }

        // Центральный поток
        for (int i = 0; i < 3; i++) {
            Location centerStream = waveFront.clone().add(
                    (Math.random() - 0.5) * 0.5,
                    Math.random() * 0.5,
                    (Math.random() - 0.5) * 0.5
            );
            ParticleEffect.DRAGON_BREATH.display(centerStream, 2, 0.1, 0.1, 0.1, 0.03);
        }

        // Эффект на земле под волной
        if (distance % 3 < 0.5) {
            for (int i = 0; i < 8; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * waveWidth / 2;
                Location groundEffect = waveFront.clone().add(
                        Math.cos(angle) * radius,
                        -0.5,
                        Math.sin(angle) * radius
                );
                ParticleEffect.SMOKE_NORMAL.display(groundEffect, 1, 0.1, 0, 0.1, 0.01);
            }
        }
    }

    private void finishWave() {
        if (waveTask != null) {
            waveTask.cancel();
        }

        // Финальный взрыв в конце волны
        Location waveEnd = waveStart.clone().add(waveDirection.clone().multiply(waveLength));
        player.getWorld().playSound(waveEnd, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.5f);
        ParticleEffect.EXPLOSION_LARGE.display(waveEnd, 1);

        // Зеленое облако
        for (int i = 0; i < 15; i++) {
            Location cloudParticle = waveEnd.clone().add(
                    (Math.random() - 0.5) * 3,
                    Math.random() * 2,
                    (Math.random() - 0.5) * 3
            );
            ParticleEffect.SPELL_MOB.display(cloudParticle, 1, 0.2, 0.2, 0.2, 0.05);
        }

        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void progress() {
        // Логика в waveTask
    }

    @Override
    public void remove() {
        super.remove();
        if (waveTask != null) {
            waveTask.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "Выпускает волну заражения, которая наносит урон и снижает защиту всем на пути.";
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
        return ATTACK;
    }

    @Override
    public Location getLocation() {
        return waveStart;
    }

    @Override
    public void load() {
        ConfigManager.defaultConfig.get().addDefault(path + "Enabled", true);
        ConfigManager.defaultConfig.get().addDefault(path + "Range", 12.0);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 14000L);
        ConfigManager.defaultConfig.get().addDefault(path + "WaveWidth", 3.0);
        ConfigManager.defaultConfig.get().addDefault(path + "WaveLength", 15.0);
        ConfigManager.defaultConfig.get().addDefault(path + "InfectionDuration", 8.0);
        ConfigManager.defaultConfig.get().addDefault(path + "DefenseReduction", 0.3);
        ConfigManager.defaultConfig.save();
    }
}