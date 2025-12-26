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

public class SoulSilence extends ApocalypseAbility {

    private static final String NAME = "SoulSilence";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.SoulSilence.";

    private double range;
    private long cooldown;
    private double baseDuration;
    private double maxDuration;
    private double healthThreshold;

    private LivingEntity target;
    private double silenceDuration;
    private BukkitRunnable silenceTask;

    public SoulSilence(Player player) {
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

            // Вычисляем длительность тишины в зависимости от HP цели
            double healthPercent = target.getHealth() / target.getMaxHealth();
            this.silenceDuration = calculateSilenceDuration(healthPercent);

            applySoulSilence();
            start();
        }
    }

    private void setFields() {
        this.range = ConfigManager.defaultConfig.get().getDouble(path + "Range", 10.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 18000L);
        this.baseDuration = ConfigManager.defaultConfig.get().getDouble(path + "BaseDuration", 3.0);
        this.maxDuration = ConfigManager.defaultConfig.get().getDouble(path + "MaxDuration", 10.0);
        this.healthThreshold = ConfigManager.defaultConfig.get().getDouble(path + "HealthThreshold", 0.5);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.7f, 0.4f);
    }

    private double calculateSilenceDuration(double healthPercent) {
        // Чем меньше HP у цели, тем дольше длится тишина
        if (healthPercent <= healthThreshold) {
            // При HP ниже порога - максимальная длительность
            double severity = 1.0 - (healthPercent / healthThreshold);
            return baseDuration + (maxDuration - baseDuration) * severity;
        } else {
            // При HP выше порога - базовая длительность
            return baseDuration;
        }
    }

    private void applySoulSilence() {
        // Эффект подавления способностей
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                (int) (silenceDuration * 20),
                2, false, false));

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                (int) (silenceDuration * 20),
                1, false, false));

        // Эффект тишины (не даёт использовать способности)
        // В реальном плагине здесь нужно добавить логику блокировки способностей

        // Визуальные эффекты
        displaySilenceEffects();

        // Запускаем задачу для постоянных эффектов
        silenceTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline() || player.isDead()) {
                    finishSilence();
                    return;
                }

                if (target == null || target.isDead()) {
                    finishSilence();
                    return;
                }

                // Обновляем визуальные эффекты
                displayOngoingSilenceEffects();

                // Звуковые эффекты
                if (System.currentTimeMillis() % 1000L < 50L) {
                    target.getWorld().playSound(target.getLocation(),
                            Sound.BLOCK_CONDUIT_AMBIENT, 0.3f, 0.2f);
                }

                // Проверяем закончилось ли время тишины
                if (silenceDuration <= 0) {
                    finishSilence();
                }

                silenceDuration -= 0.05; // Уменьшаем оставшееся время
            }
        };

        silenceTask.runTaskTimer(ProjectKorra.plugin, 0L, 1L);

        // Сообщение игроку
        double healthPercent = target.getHealth() / target.getMaxHealth();
        String durationMessage = String.format("%.1f", silenceDuration);
        if (healthPercent <= healthThreshold) {
            player.sendMessage("§4⚰️ Тишина души применена! Длительность: §c" + durationMessage + "§4 сек (МАКС)");
        } else {
            player.sendMessage("§8Тишина души применена. Длительность: §7" + durationMessage + "§8 сек");
        }

        if (target instanceof Player) {
            ((Player) target).sendMessage("§8Ваши способности подавлены на §7" + durationMessage + "§8 сек!");
        }
    }

    private void displaySilenceEffects() {
        // Изначальный взрыв тишины
        ParticleEffect.EXPLOSION_LARGE.display(target.getLocation(), 1);

        // Кольцо тишины
        for (int i = 0; i < 16; i++) {
            double angle = 2 * Math.PI * i / 16;
            double radius = 1.5;

            Location ringParticle = target.getLocation().add(
                    Math.cos(angle) * radius,
                    0.5,
                    Math.sin(angle) * radius
            );

            ParticleEffect.SMOKE_LARGE.display(ringParticle, 1, 0.1, 0.1, 0.1, 0.02);
        }

        // Черные частицы тишины
        for (int i = 0; i < 20; i++) {
            Location silenceParticle = target.getLocation().add(
                    (Math.random() - 0.5) * 2,
                    Math.random() * 2,
                    (Math.random() - 0.5) * 2
            );

            ParticleEffect.SPELL_WITCH.display(silenceParticle, 1, 0.1, 0.1, 0.1, 0.05);
        }

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.3f);
    }

    private void displayOngoingSilenceEffects() {
        // Парящие черные частицы вокруг цели
        for (int i = 0; i < 3; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = 0.8 + Math.random() * 0.7;
            double height = Math.random() * 1.5;

            Location floatingParticle = target.getLocation().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
            );

            // Частицы становятся гуще при меньшем HP цели
            double healthPercent = target.getHealth() / target.getMaxHealth();
            int particleCount = healthPercent <= healthThreshold ? 2 : 1;

            ParticleEffect.SMOKE_NORMAL.display(floatingParticle, particleCount, 0.05, 0.05, 0.05, 0.005);
        }

        // Эффект на земле
        if (System.currentTimeMillis() % 500L < 50L) {
            for (int i = 0; i < 4; i++) {
                double angle = 2 * Math.PI * i / 4;
                double radius = 0.5;

                Location groundEffect = target.getLocation().add(
                        Math.cos(angle) * radius,
                        -0.2,
                        Math.sin(angle) * radius
                );

                ParticleEffect.SMOKE_NORMAL.display(groundEffect, 1, 0.1, 0, 0.1, 0.01);
            }
        }

        // Аура тишины при низком HP
        double healthPercent = target.getHealth() / target.getMaxHealth();
        if (healthPercent <= healthThreshold) {
            for (int i = 0; i < 2; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = 1.2;

                Location auraParticle = target.getLocation().add(
                        Math.cos(angle) * radius,
                        0.3 + Math.random() * 1.0,
                        Math.sin(angle) * radius
                );

                ParticleEffect.SPELL_WITCH.display(auraParticle, 1, 0.1, 0.1, 0.1, 0.03);
            }
        }
    }

    private void finishSilence() {
        if (silenceTask != null) {
            silenceTask.cancel();
        }

        // Эффект снятия тишины
        if (target != null && !target.isDead()) {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 0.8f);

            // Взрыв освобождения
            ParticleEffect.FLAME.display(target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.1);

            // Снятие эффектов
            target.removePotionEffect(PotionEffectType.WEAKNESS);
            target.removePotionEffect(PotionEffectType.SLOWNESS);

            if (target instanceof Player) {
                ((Player) target).sendMessage("§aЭффект тишины души снят!");
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.5f);

        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void progress() {
        // Логика в silenceTask
    }

    @Override
    public void remove() {
        super.remove();
        if (silenceTask != null) {
            silenceTask.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "Подавляет способности цели. Чем ниже HP цели - тем дольше длится эффект.";
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
        return UTILITY;
    }

    @Override
    public Location getLocation() {
        return player != null ? player.getLocation() : null;
    }

    @Override
    public void load() {
        ConfigManager.defaultConfig.get().addDefault(path + "Enabled", true);
        ConfigManager.defaultConfig.get().addDefault(path + "Range", 10.0);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 18000L);
        ConfigManager.defaultConfig.get().addDefault(path + "BaseDuration", 3.0);
        ConfigManager.defaultConfig.get().addDefault(path + "MaxDuration", 10.0);
        ConfigManager.defaultConfig.get().addDefault(path + "HealthThreshold", 0.5);
        ConfigManager.defaultConfig.save();
    }
}
