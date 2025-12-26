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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlagueTouch extends ApocalypseAbility {

    private static final String NAME = "PlagueTouch";
    private static String path = "ExtraAbilities.miminttto.Apocalypse.PlagueTouch.";

    private double range;
    private long cooldown;
    private long plagueDuration;
    private double plagueDamage;
    private double spreadRadius;
    private int maxSpread;

    private Map<UUID, Long> infectedEntities = new HashMap<>();
    private BukkitRunnable plagueTask;

    public PlagueTouch(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        setFields();

        Entity target = GeneralMethods.getTargetedEntity(player, range);

        if (target instanceof LivingEntity && target.getUniqueId() != player.getUniqueId()) {
            infectEntity((LivingEntity) target);
            startPlagueSpread();
            start();
        }
    }

    private void setFields() {
        this.range = ConfigManager.defaultConfig.get().getDouble(path + "Range", 10.0);
        this.cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown", 12000L);
        this.plagueDuration = ConfigManager.defaultConfig.get().getLong(path + "PlagueDuration", 10000L);
        this.plagueDamage = ConfigManager.defaultConfig.get().getDouble(path + "PlagueDamage", 1.0);
        this.spreadRadius = ConfigManager.defaultConfig.get().getDouble(path + "SpreadRadius", 4.0);
        this.maxSpread = ConfigManager.defaultConfig.get().getInt(path + "MaxSpread", 5);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8f, 0.3f);
    }

    private void infectEntity(LivingEntity entity) {
        infectedEntities.put(entity.getUniqueId(), System.currentTimeMillis());

        // Эффекты заражения
        entity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, false));

        // Визуальные эффекты
        ParticleEffect.SPELL_MOB_AMBIENT.display(entity.getLocation().add(0, 1, 0), 10, 0.5, 1.0, 0.5, 0.1);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 0.7f, 0.5f);
    }

    private void startPlagueSpread() {
        plagueTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    finishPlague();
                    return;
                }

                long currentTime = System.currentTimeMillis();

                // Проверяем время заражения для каждой сущности
                infectedEntities.entrySet().removeIf(entry -> {
                    Entity entity = (Entity) player.getWorld().getEntities();
                    if (entity == null || !entity.isValid()) {
                        return true;
                    }

                    if (currentTime - entry.getValue() > plagueDuration) {
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.0f);
                        return true;
                    }

                    // Наносим периодический урон
                    if (currentTime % 1000L < 50L) {
                        ((LivingEntity) entity).damage(plagueDamage);
                        displayPlagueParticles(entity.getLocation());
                    }

                    // Распространение чумы
                    if (currentTime % 2000L < 50L && infectedEntities.size() < maxSpread) {
                        spreadPlague((LivingEntity) entity);
                    }

                    return false;
                });

                // Завершаем если нет зараженных
                if (infectedEntities.isEmpty()) {
                    finishPlague();
                }
            }
        };

        plagueTask.runTaskTimer(ProjectKorra.plugin, 0L, 20L);
    }

    private void spreadPlague(LivingEntity source) {
        for (Entity nearby : source.getNearbyEntities(spreadRadius, spreadRadius, spreadRadius)) {
            if (nearby instanceof LivingEntity &&
                    nearby != player &&
                    !infectedEntities.containsKey(nearby.getUniqueId()) &&
                    infectedEntities.size() < maxSpread) {

                // Зеленая линия распространения
                Vector direction = nearby.getLocation().toVector().subtract(source.getLocation().toVector());
                double distance = source.getLocation().distance(nearby.getLocation());
                int particles = (int) (distance * 0.5);

                for (int i = 0; i <= particles; i++) {
                    double ratio = i / (double) particles;
                    Location particleLoc = source.getLocation().clone().add(direction.clone().multiply(ratio));
                    ParticleEffect.VILLAGER_HAPPY.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }

                infectEntity((LivingEntity) nearby);
                break;
            }
        }
    }

    private void displayPlagueParticles(Location location) {
        for (int i = 0; i < 3; i++) {
            Location particleLoc = location.clone().add(
                    (Math.random() - 0.5) * 1.5,
                    Math.random() * 2.0,
                    (Math.random() - 0.5) * 1.5
            );
            ParticleEffect.SLIME.display(particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void finishPlague() {
        if (plagueTask != null) {
            plagueTask.cancel();
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.0f);
        ParticleEffect.HAPPY_VILLAGER.display(player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3);

        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void progress() {
        // Логика в plagueTask
    }

    @Override
    public void remove() {
        super.remove();
        if (plagueTask != null) {
            plagueTask.cancel();
        }
        infectedEntities.clear();
    }

    @Override
    public String getDescription() {
        return "Накладывает чуму на цель, которая наносит периодический урон и распространяется на других.";
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
        ConfigManager.defaultConfig.get().addDefault(path + "PlagueDuration", 10000L);
        ConfigManager.defaultConfig.get().addDefault(path + "PlagueDamage", 1.0);
        ConfigManager.defaultConfig.get().addDefault(path + "SpreadRadius", 4.0);
        ConfigManager.defaultConfig.get().addDefault(path + "MaxSpread", 5);
        ConfigManager.defaultConfig.save();
    }
}