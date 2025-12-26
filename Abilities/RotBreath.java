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
import org.bukkit.util.Vector;

public class RotBreath extends ApocalypseAbility {

    private static final String NAME = "RotBreath";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.RotBreath.";

    private double range;
    private long cooldown;
    private double duration;
    private double healingReduction;
    private double blindnessDuration;

    private Location breathOrigin;
    private Vector breathDirection;
    private BukkitRunnable breathTask;
    private long startTime;

    public RotBreath(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        setFields();

        this.breathOrigin = player.getEyeLocation();
        this.breathDirection = player.getLocation().getDirection().normalize();
        this.startTime = System.currentTimeMillis();

        startRotBreath();
        start();
    }

    private void setFields() {
        this.range = ConfigManager.defaultConfig.get().getDouble(path + "Range", 8.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 10000L);
        this.duration = ConfigManager.defaultConfig.get().getDouble(path + "Duration", 5.0);
        this.healingReduction = ConfigManager.defaultConfig.get().getDouble(path + "HealingReduction", 0.7);
        this.blindnessDuration = ConfigManager.defaultConfig.get().getDouble(path + "BlindnessDuration", 3.0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.2f);
    }

    private void startRotBreath() {
        breathTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline() || player.isDead()) {
                    finishBreath();
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime > duration * 1000) {
                    finishBreath();
                    return;
                }

                // Обновляем направление дыхания по взгляду игрока
                breathDirection = player.getLocation().getDirection().normalize();
                breathOrigin = player.getEyeLocation();

                // Создаем конус дыхания
                createRotBreathCone();

                // Поражаем врагов в конусе
                affectEntitiesInCone();

                // Звуковые эффекты
                if (currentTime % 300L < 50L) {
                    player.getWorld().playSound(breathOrigin, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.3f);
                }
            }
        };

        breathTask.runTaskTimer(ProjectKorra.plugin, 0L, 2L);
    }

    private void createRotBreathCone() {
        double coneAngle = 30.0; // Угол конуса в градусах
        double coneLength = range;

        for (double r = 0; r < coneLength; r += 0.5) {
            double currentRadius = r * Math.tan(Math.toRadians(coneAngle / 2));

            for (int i = 0; i < 8; i++) {
                double angle = 2 * Math.PI * i / 8;
                double offsetX = Math.cos(angle) * currentRadius;
                double offsetZ = Math.sin(angle) * currentRadius;

                // Случайное смещение для эффекта тумана
                offsetX += (Math.random() - 0.5) * 0.3;
                offsetZ += (Math.random() - 0.5) * 0.3;

                Vector offset = new Vector(offsetX, (Math.random() - 0.5) * 0.5, offsetZ);

                // Поворачиваем смещение в направлении дыхания
                Vector rotatedOffset = rotateVectorAroundAxis(offset, breathDirection);

                Location particleLoc = breathOrigin.clone().add(
                        breathDirection.clone().multiply(r)
                ).add(rotatedOffset);

                // Зеленый/желтый туман гниения
                if (Math.random() < 0.7) {
                    // Основной туман
                    ParticleEffect.SPELL_MOB_AMBIENT.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                } else {
                    // Споры гниения
                    ParticleEffect.DRAGON_BREATH.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.02);
                }

                // Эффект на земле
                if (r > 1 && Math.random() < 0.1) {
                    Location groundParticle = particleLoc.clone();
                    groundParticle.setY(groundParticle.getY() - 1);
                    if (GeneralMethods.isSolid(groundParticle.getBlock())) {
                        ParticleEffect.SMOKE_NORMAL.display(groundParticle, 1, 0.1, 0, 0.1, 0.005);
                    }
                }
            }
        }

        // Концентрированный поток изо рта
        for (int i = 0; i < 3; i++) {
            Location coreStream = breathOrigin.clone().add(
                    breathDirection.clone().multiply(i * 0.3)
            ).add(
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.2
            );
            ParticleEffect.SLIME.display(coreStream, 2, 0.1, 0.1, 0.1, 0.03);
        }
    }

    private Vector rotateVectorAroundAxis(Vector vector, Vector axis) {
        // Поворот вектора вокруг заданной оси
        Vector normalizedAxis = axis.clone().normalize();
        return vector.clone().rotateAroundAxis(normalizedAxis, 0);
    }

    private void affectEntitiesInCone() {
        double coneAngle = Math.toRadians(30);
        double maxDistance = range;

        for (Entity entity : breathOrigin.getWorld().getNearbyEntities(breathOrigin, maxDistance, maxDistance, maxDistance)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;

                Vector toEntity = entity.getLocation().toVector().subtract(breathOrigin.toVector());
                double distance = toEntity.length();

                if (distance <= maxDistance) {
                    // Проверяем, находится ли сущность в конусе
                    double angle = breathDirection.angle(toEntity);

                    if (angle <= coneAngle / 2) {
                        // Применяем эффекты гниения
                        applyRotEffects(living);

                        // Визуальный эффект поражения
                        if (System.currentTimeMillis() % 500L < 50L) {
                            ParticleEffect.VILLAGER_ANGRY.display(
                                    entity.getLocation().add(0, 1, 0),
                                    2, 0.2, 0.3, 0.2, 0.05
                            );
                        }
                    }
                }
            }
        }
    }

    private void applyRotEffects(LivingEntity entity) {
        // Сильное снижение лечения
        entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, false));

        // Снижение эффективности зелий лечения
        entity.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 100, 2, false, false));

        // Слепота (с шансом)
        if (Math.random() < 0.3) {
            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS,
                    (int) (blindnessDuration * 20),
                    0, false, false
            ));
        }

        // Тошнота
        entity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, false));

        // Замедление
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, false));

        // Снижение атаки
        entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, false, false));

        // Звук поражения
        if (System.currentTimeMillis() % 1000L < 50L) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_HURT, 0.4f, 0.5f);
        }
    }

    private void finishBreath() {
        if (breathTask != null) {
            breathTask.cancel();
        }

        // Финальный выдох
        player.getWorld().playSound(breathOrigin, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.3f);

        // Облако остаточного тумана
        for (int i = 0; i < 20; i++) {
            Location residualCloud = breathOrigin.clone().add(
                    breathDirection.clone().multiply(Math.random() * 3)
            ).add(
                    (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 1,
                    (Math.random() - 0.5) * 2
            );
            ParticleEffect.SPELL_MOB.display(residualCloud, 1, 0.2, 0.2, 0.2, 0.03);
        }

        // Эффект на игроке
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false));

        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void progress() {
        // Логика в breathTask
    }

    @Override
    public void remove() {
        super.remove();
        if (breathTask != null) {
            breathTask.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "Выдыхает облако гниения, которое снижает лечение и видимость врагов.";
    }

    @Override
    public String getInstructions() {
        return "Зажмите Shift";
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
        return breathOrigin;
    }

    @Override
    public void load() {
        ConfigManager.defaultConfig.get().addDefault(path + "Enabled", true);
        ConfigManager.defaultConfig.get().addDefault(path + "Range", 8.0);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 10000L);
        ConfigManager.defaultConfig.get().addDefault(path + "Duration", 5.0);
        ConfigManager.defaultConfig.get().addDefault(path + "HealingReduction", 0.7);
        ConfigManager.defaultConfig.get().addDefault(path + "BlindnessDuration", 3.0);
        ConfigManager.defaultConfig.save();
    }
}
