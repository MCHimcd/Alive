package mc.alive.game;

import mc.alive.game.effect.Effect;
import mc.alive.role.Role;
import mc.alive.role.Skill;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.survivor.Survivor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.util.Objects.requireNonNull;
import static mc.alive.Alive.game;
import static mc.alive.Alive.plugin;
import static mc.alive.game.TickRunner.chosen_duct;
import static mc.alive.game.TickRunner.chosen_item_display;
import static mc.alive.util.Message.rMsg;

public class PlayerData {
    private final List<Effect> effects = new ArrayList<>();
    private final Role role;
    private final Player player;
    private final List<Integer> skill_cd = new ArrayList<>();
    //选择的技能
    private int current_skill_id = 0;
    //选择技能重置cd
    private int current_skill_reset_cd = 0;
    private double health;
    //hunter回血间隔
    private int health_tick = 0;
    //护盾
    private double shield;
    //受伤后回护盾cd
    private int shield_cd = 0;
    //回护盾cd
    private int shield_tick = 0;
    //维修cd
    public int fix_tick = -1;
    //攻击cd
    public double attack_cd = -1;

    public Role getRole() {
        return role;
    }

    public static PlayerData getPlayerData(Player player) {
        assert game != null;
        return game.playerData.get(player);
    }

    public static void setSkillCD(Player player, int index, int amount) {
        assert game != null;
        game.playerData.get(player).skill_cd.set(index, amount);
    }

    public void addEffect(Effect effect) {
        effects.add(effect);
    }

    public boolean hasEffect(Class<? extends Effect> effect) {
        return effects.stream().anyMatch(e -> e.getClass().equals(effect));
    }

    public void tick() {
        //Effect
        effects.removeIf(Effect::tick);
        //普攻冷却
        attack_cd = Math.max(0, attack_cd - 0.05);
        if (attack_cd > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 1, 100, true, false));
        }
        //技能冷却
        for (int i = 0; i < skill_cd.size(); i++) {
            if (skill_cd.get(i) > 0) skill_cd.set(i, skill_cd.get(i) - 1);
        }
        //技能选择
        current_skill_reset_cd = Math.max(0, current_skill_reset_cd - 1);
        if (current_skill_reset_cd == 0) {
            current_skill_id = 0;
        }
        //护盾恢复
        if (role instanceof Survivor s) {
            if (shield_cd > 0) shield_cd--;
            if (shield_tick > 0) shield_tick--;
            if (shield_cd == 0 && shield_tick == 0 && shield < s.getMaxShield()) {
                shieldDamage(-5);
                shield_tick = 20;
            }
        }
        //血量恢复
        if (health_tick > 0) health_tick--;
        if (role instanceof Hunter && health_tick == 0) {
            damageOrHeal(-0.25);
        }
        //维修
        var target = chosen_item_display.get(player);
        if (fix_tick >= 0) {
            if (--fix_tick == 0 && target != null) {
                player.getLocation().getNearbyPlayers(20).forEach(player1 ->
                        player1.playSound(player1, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f));
                game.fix(target, role.getIntelligence());
            }
        }
    }

    public void changeSkillValue() {
        ++current_skill_id;
        Arrays.stream(role.getClass().getMethods())
                .forEach(m -> {
                    var a = m.getAnnotation(Skill.class);
                    if (a != null && a.id() == current_skill_id) {
                        if (a.minLevel() > role.getLevel()) {
                            ++current_skill_id;
                            return;
                        }
                        //找到
                        current_skill_reset_cd = 20;
                        if (skill_cd.get(current_skill_id) == 0) {
                            player.showTitle(
                                    Title.title(
                                            Component.text("§a·".repeat(a.id())),
                                            Component.text(a.name(), TextColor.color(0, 255, 255)),
                                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                                    )
                            );
                        } else {
                            player.showTitle(
                                    Title.title(
                                            Component.text("§a·".repeat(a.id())),
                                            Component.text("冷却中", NamedTextColor.DARK_RED),
                                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                                    )
                            );
                        }
                        player.playSound(player, Sound.UI_BUTTON_CLICK, .3f, 10f);
                    }
                });
        if (current_skill_id > role.getSkillCount()) {
            current_skill_id = 0;
            clearTitle();
        }
    }

    public void useSkill() {
        Arrays.stream(role.getClass().getMethods())
                .filter(m -> {
                    var a = m.getAnnotation(Skill.class);
                    if (a != null) {
                        return a.id() == current_skill_id && skill_cd.get(a.id()) == 0;
                    }
                    return false;
                })
                .forEach(m -> {
                    try {
                        current_skill_id = 0;
                        skill_cd.set(0, 1);
                        m.invoke(role);
                        clearTitle();
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void clearTitle() {
        player.showTitle(
                Title.title(
                        Component.empty(),
                        Component.empty()
                )
        );
    }

    public PlayerData(Player player, Role role) {
        this.player = player;
        this.role = role;
        damageOrHeal(-role.getMaxHealth());
        if (role instanceof Survivor s) {
            Game.t_survivor.addPlayer(player);
            shieldDamage(-s.getMaxShield());
        } else {
            Game.t_hunter.addPlayer(player);
        }
        for (int i = 0; i < role.getSkillCount(); i++) {
            skill_cd.add(0);
        }
        var name = Component.text(role.toString());
        player.displayName(name);
        player.playerListName(name);
    }

    /**
     * @param amount 正为扣血，负为加血
     */
    public void damageOrHeal(double amount) {
        if (amount >= 0) {
            player.damage(0.01);
            BlockData blood = Material.REDSTONE_BLOCK.createBlockData();
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().clone().add(0, 1, 0), 100, 0.3, 0.3, 0.3, blood);
            if (role instanceof Survivor) {
                if (shield > 0) {
                    //减护盾
                    var amount_s = amount;
                    amount = Math.max(0, amount - shield);
                    shieldDamage(amount_s);
                    shield_cd = 200;
                }
            }
            //减血量
            if (health >= 0) {
                health -= amount;
                health_tick = 40;
                player.damage(0.01);
            }
            if (role instanceof Survivor) {
                if (health <= 0) {
                    die();
                    return;
                }
            } else health = Math.max(-5, health);
            Bukkit.broadcast(rMsg(String.valueOf(health)));
        } else health = Math.min(health - amount, role.getMaxHealth());
        player.setHealth(Math.max(0.1, 20 * health / role.getMaxHealth()));
        //减速
        AttributeInstance speed = requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED));
        if (health <= role.getMaxHealth() * 0.5) {
            speed.setBaseValue(role.getSpeed() * 0.3);
        } else if (health <= role.getMaxHealth() * 0.2) {
            speed.setBaseValue(role.getSpeed() * 0.1);
        } else if (health <= 0) {
            speed.setBaseValue(0);
        } else speed.setBaseValue(role.getSpeed());
    }

    /**
     * @param amount 正为扣护盾，负为加护盾
     */
    private void shieldDamage(double amount) {
        var max = ((Survivor) role).getMaxShield();
        shield = Math.min(max, Math.max(0, shield - amount));
        player.setAbsorptionAmount(20 * shield / max);
    }

    public void tryIntoDuct() {
        if (chosen_duct != null) player.teleport(chosen_duct.clone().setDirection(player.getLocation().getDirection()));
    }

    public void tryLevelUp() {
        if (!(role instanceof Hunter h)) return;
        h.levelUp();
    }

    private void die() {
        Bukkit.broadcast(player.displayName().append(rMsg("%s".formatted(List.of(
                "去世了",
                "离开了人间."
        ).get(new Random().nextInt(0, 2))))));
        game.spawnBody(player);
        Game.resetPlayer(player);
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            game.survivors.remove(player);
            if (game.survivors.isEmpty()) {
                TickRunner.gameEnd = true;
            }
        });
    }

}