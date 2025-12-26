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

public class FinalClaim extends ApocalypseAbility {

    private static final String NAME = "FinalClaim";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.FinalClaim.";

    private double range;
    private long cooldown;
    private double executeThreshold;
    private double fearDuration;
    private double executeDamage;
    private double nonExecuteDamage;

    private LivingEntity target;

    public FinalClaim(Player player) {
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
            executeFinalClaim();
            start();
        }
    }

    private void setFields() {
        this.range = ConfigManager.defaultConfig.get().getDouble(path + "Range", 15.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 20000L);
        this.executeThreshold = ConfigManager.defaultConfig.get().getDouble(path + "ExecuteThreshold", 0.3);
        this.fearDuration = ConfigManager.defaultConfig.get().getDouble(path + "FearDuration", 5.0);
        this.executeDamage = ConfigManager.defaultConfig.get().getDouble(path + "ExecuteDamage", 20.0);
        this.nonExecuteDamage = ConfigManager.defaultConfig.get().getDouble(path + "NonExecuteDamage", 8.0);
    }

    private void executeFinalClaim() {
        double targetHealthPercent = target.getHealth() / target.getMaxHealth();
        boolean canExecute = targetHealthPercent <= executeThreshold;

        // Звук предупреждения
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.3f);

        // Анимация прицеливания
        Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector());
        double distance = player.getLocation().distance(target.getLocation());
        int particles = (int) (distance * 0.3);

        for (int i = 0; i <= particles; i++) {
            double ratio = i / (double) particles;
            Location particleLoc = player.getLocation().clone().add(direction.clone().multiply(ratio));
            ParticleEffect.SMOKE_LARGE.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Задержка перед ударом
        ProjectKorra.plugin.getServer().getScheduler().runTaskLater(ProjectKorra.plugin, () -> {
            if (canExecute) {
                executeTarget();
            } else {
                damageTarget();
            }

            bPlayer.addCooldown(this);
            remove();
        }, 20L);
    }

    private void executeTarget() {
        // Мгновенное убийство
        target.setHealth(0);

        // Эффекты исполнения
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);

        // Частицы смерти
        ParticleEffect.EXPLOSION_HUGE.display(target.getLocation(), 1);
        ParticleEffect.FLAME.display(target.getLocation().add(0, 1, 0), 20, 0.5, 1.0, 0.5, 0.1);

        // Визуальный эффект для игрока
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0, false, false));
    }

    private void damageTarget() {
        // Сильный урон
        DamageHandler.damageEntity(target, nonExecuteDamage, this);

        // Эффект страха
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (fearDuration * 20), 3, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int) (fearDuration * 20), 2, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (fearDuration * 10), 0, false, false));

        // Эффекты
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.7f);
        ParticleEffect.SMOKE_LARGE.display(target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);

        // Визуальный эффект страха
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            double radius = 1.5;
            Location particleLoc = target.getLocation().add(
                    Math.cos(angle) * radius,
                    0.5,
                    Math.sin(angle) * radius
            );
            ParticleEffect.SPELL_WITCH.display(particleLoc, 2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    public void progress() {
        remove();
    }

    @Override
    public String getDescription() {
        return "Мгновенно убивает цель с низким здоровьем или наносит огромный урон и страх.";
    }

    @Override
    public String getInstructions() {
        return "Нажмите ЛКМ по цели с низким HP";
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
        ConfigManager.defaultConfig.get().addDefault(path + "Range", 15.0);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 20000L);
        ConfigManager.defaultConfig.get().addDefault(path + "ExecuteThreshold", 0.3);
        ConfigManager.defaultConfig.get().addDefault(path + "FearDuration", 5.0);
        ConfigManager.defaultConfig.get().addDefault(path + "ExecuteDamage", 20.0);
        ConfigManager.defaultConfig.get().addDefault(path + "NonExecuteDamage", 8.0);
        ConfigManager.defaultConfig.save();
    }
}