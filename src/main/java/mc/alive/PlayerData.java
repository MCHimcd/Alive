package mc.alive;

import mc.alive.effect.Effect;
import mc.alive.effect.MultilevelEffect;
import mc.alive.role.Role;
import mc.alive.role.Skill;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.survivor.Survivor;
import mc.alive.tick.TickRunnable;
import mc.alive.tick.TickRunner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;
import static mc.alive.tick.PlayerTickrunnable.chosen_duct;
import static mc.alive.tick.PlayerTickrunnable.chosen_item_display;
import static mc.alive.util.Message.rMsg;

public final class PlayerData implements TickRunnable {
    private final List<Effect> effects = new ArrayList<>();
    private final Role role;
    private final Player player;
    private final List<Integer> skill_cd = new ArrayList<>(Arrays.asList(0, 0, 0, 0));
    //维修cd
    public int fix_tick = -1;
    //攻击cd
    public double attack_cd = -1;
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
    //体力值
    private int stamina = 0;
    //回体力cd
    private int stamina_tick = 0;

    public PlayerData(Player player, Role role) {
        this.player = player;
        this.role = role;
        damageOrHeal(-role.getMaxHealth());
        if (role instanceof Survivor s) {
            Game.team_survivor.addPlayer(player);
            shieldDamage(-s.getMaxShield());
        } else {
            Game.team_hunter.addPlayer(player);
        }
        for (int i = 0; i < role.getSkillCount(); i++) {
            skill_cd.add(0);
        }
        @SuppressWarnings("DataFlowIssue") var name = rMsg(((String) Alive.roles_config.get(String.valueOf(role.getRoleID()))).split(" ")[1]);
        player.displayName(name);
        player.playerListName(name);
        addStamina(100);
        startTick();
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
                //回血时间=剩余血量%*200
                health_tick = (int) Math.max(100, (Math.max(0, health / role.getMaxHealth()) * 400));
                player.damage(0.01);
            }
            if (role instanceof Survivor) {
                if (health <= 0) {
                    die();
                    return;
                }
            } else health = Math.max(-5, health);
        } else health = Math.min(health - amount, role.getMaxHealth());
        if (health < 0) player.setHealth(0.1);
        else player.setHealth(Math.max(0.1, 20 * health / role.getMaxHealth()));
        //减速
        AttributeInstance speed = requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED));
        if (health <= role.getMaxHealth() * 0.5) {
            speed.setBaseValue(role.getSpeed() * 0.3);
        } else if (health <= role.getMaxHealth() * 0.2) {
            speed.setBaseValue(-1);
        } else speed.setBaseValue(role.getSpeed());
    }

    private void die() {
        player.getWorld().sendMessage(player.displayName().append(rMsg("%s".formatted(List.of(
                "去世了",
                "离开了人间."
        ).get(new Random().nextInt(0, 2))))));
        game.spawnBody(player);
        Game.resetPlayer(player);
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.getAsyncScheduler().runNow(plugin, _ -> {
            game.survivors.remove(player);
            if (game.survivors.isEmpty()) {
                TickRunner.gameEnd = true;
            }
        });
    }

    /**
     * @param amount 正为扣护盾，负为加护盾
     */
    private void shieldDamage(double amount) {
        var max = ((Survivor) role).getMaxShield();
        shield = Math.min(max, Math.max(0, shield - amount));
        player.setAbsorptionAmount(20 * shield / max);
    }

    /**
     * @param amount 数量，可为负
     * @return 是否大于0
     */
    public boolean addStamina(int amount) {
        if (amount < 0) {
            stamina_tick = 40;
        }
        stamina = Math.max(0, Math.min(stamina + amount, 100));
        player.setLevel(stamina);
        player.setExp((float) stamina / 100);
        return stamina != 0;
    }

    /**
     * @return 玩家对应的PlayerData，若游戏未开始则返回null
     */
    public static PlayerData of(Player player) {
        return game.playerData.get(player);
    }

    public static void setSkillCD(Player player, int index, int amount) {
        assert game != null;
        game.playerData.get(player).skill_cd.set(index, amount);
    }

    public int getStamina() {
        return stamina;
    }

    public Role getRole() {
        return role;
    }

    public void addEffect(Effect effect) {
        var e = effects.stream().filter(e1 -> e1.getClass().equals(effect.getClass())).findFirst();
        if (e.isPresent()) {
            if (!(e.get() instanceof MultilevelEffect)) {
                e.get().addTime(effect.getTime());
            } else {
                effects.add(effect);
            }
        } else {
            effects.add(effect);
        }
    }

    public boolean hasEffect(Class<? extends Effect> effect) {
        return effects.stream().anyMatch(e -> e.getClass().equals(effect));
    }

    public void removeEffect(Class<? extends Effect> effect) {
        effects.stream().filter(effect::isInstance).findFirst().ifPresent(effect1 -> effect1.addTime(-1000000000));
    }

    @Override
    public void tick() {
        //Effect
        effects.stream()
                .filter(effect -> effect instanceof MultilevelEffect)
                .map(e -> (MultilevelEffect) e)
                .collect(Collectors.groupingBy(
                        Object::getClass,
                        Collectors.maxBy(Comparator.comparingInt(MultilevelEffect::getLevel))
                ))
                .forEach((_, effect) -> effect.ifPresent(effect1 -> {
                    if (effect1.shouldRemove()) effects.remove(effect1);
                }));
        effects.removeIf(effect -> !(effect instanceof MultilevelEffect) && effect.shouldRemove());

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

        //体力回复
        if (stamina_tick > 0) stamina_tick--;
        if (stamina_tick == 0) {
            addStamina(2);
        }

        //维修
        var target = chosen_item_display.get(player);
        if (fix_tick >= 0) {
            if (--fix_tick == 0 && target != null) {
                player.getLocation().getNearbyPlayers(20).forEach(player1 ->
                        player1.playSound(player1, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f));
                if (role instanceof Survivor survivor) {
                    game.fix(target, survivor.getFixSpeed());
                }
            }
        }
    }

    public void changeSkillValue() {
        //蓄力技能取消
        role.skill_locations.forEach((loc, task) -> {
            if (loc.equals(Role.ZERO_LOC) && task != null && !task.isCancelled()) task.cancel();
        });
        role.skill_locations.remove(Role.ZERO_LOC);

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

    private void clearTitle() {
        player.showTitle(
                Title.title(
                        Component.empty(),
                        Component.empty()
                )
        );
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

    public void tryIntoDuct() {
        if (chosen_duct != null) player.teleport(chosen_duct.clone().setDirection(player.getLocation().getDirection()));
    }

    public void tryLevelUp() {
        if (!(role instanceof Hunter h)) return;
        h.levelUp();
    }

    public boolean canMove() {
        return health > role.getMaxHealth() * 0.2;
    }
}