package mc.alive;

import mc.alive.effect.Effect;
import mc.alive.effect.Giddy;
import mc.alive.effect.MultilevelEffect;
import mc.alive.effect.Speed;
import mc.alive.mechanism.SignalRepeater;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private final List<Integer> skill_cd = new ArrayList<>(Arrays.asList(0, 0, 0, 0));
    private final Map<Integer, Method> skill_methods = new HashMap<>();
    public int generator_tick = -1;    //维修或破坏cd
    private Player player;
    private int current_skill_id = 0;    //选择的技能
    private int current_skill_reset_cd = 0;    //选择技能重置cd
    private double health;


    public PlayerData(Player player, Role role) {
        this.player = player;
        this.role = role;
        if (role instanceof Survivor) {
            Game.team_survivor.addPlayer(player);
        } else if (role instanceof Hunter h) {
            Game.team_hunter.addPlayer(player);
            damageOrHeal(-h.getMaxHealth());
        }

        Arrays.stream(role.getClass().getMethods()).forEach(m -> {
            var a = m.getAnnotation(Skill.class);
            if (a != null) {
                skill_methods.put(a.id(), m);
            }
        });
        for (int i = 0; i < role.getSkillCount(); i++) {
            skill_cd.add(0);
        }

        @SuppressWarnings("DataFlowIssue") var name = rMsg(((String) Alive.roles_config.get(String.valueOf(role.getRoleID()))).split(" ")[1]);
        player.displayName(name);
        player.playerListName(name);

        startTick();
    }

    /**
     * @param amount 正为扣血，负为加血
     */
    public void damageOrHeal(double amount) {
        if (amount > 0) {
            player.damage(0.01);
            BlockData blood = Material.REDSTONE_BLOCK.createBlockData();
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().clone().add(0, 1, 0), 100, 0.3, 0.3, 0.3, blood);

        }
        if (role instanceof Survivor s) {
            if (amount > 0) {
                s.damage();
            } else {
                s.heal();
            }
            //加速
            addEffect(new Speed(player, 20, 0));
        } else if (role instanceof Hunter h) {
            if (amount >= 0) {
                //减血量
                if (health >= 0) {
                    health -= amount;
                    //回血时间=剩余血量%*200
                    h.resetHealthTick(health);
                    player.damage(0.01);
                }
                health = Math.max(-5, health);
            } else health = Math.min(health - amount, h.getMaxHealth());
            if (health < 0) player.setHealth(0.1);
            else player.setHealth(Math.max(0.1, 20 * health / h.getMaxHealth()));
            //减速
            AttributeInstance speed = requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED));
            if (health <= h.getMaxHealth() * 0.5) {
                speed.setBaseValue(role.getSpeed() * 0.3);
            } else if (health <= h.getMaxHealth() * 0.2) {
                speed.setBaseValue(-1);
            } else speed.setBaseValue(h.getSpeed());
        }
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

    /**
     * 设置Role的技能的CD
     * @param player 玩家
     * @param index 技能id，从1开始
     * @param amount 单位为tick
     */
    public static void setSkillCD(Player player, int index, int amount) {
        assert game != null;
        game.playerData.get(player).skill_cd.set(index, amount);
    }

    /**
     * @return 玩家对应的PlayerData，若游戏未开始则返回null
     */
    public static PlayerData of(Player player) {
        return game.playerData.get(player);
    }

    public void die() {
        player.getWorld().sendMessage(player.displayName().append(rMsg("%s".formatted(List.of(
                "去世了",
                "离开了人间."
        ).get(new Random().nextInt(0, 2))))));
        game.spawnPlayerBody(player);
        Game.resetPlayer(player);
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.getAsyncScheduler().runNow(plugin, _ -> {
            game.survivors.remove(player);
            if (game.survivors.isEmpty()) {
                TickRunner.gameEnd = true;
            }
        });
    }

    public void setPlayer(Player player) {
        role.setPlayer(player);
        this.player = player;
    }

    public Role getRole() {
        return role;
    }

    public boolean hasEffect(Class<? extends Effect> effect) {
        return effects.stream().anyMatch(e -> e.getClass().equals(effect));
    }

    public void removeEffect(Class<? extends Effect> effect) {
        effects.stream().filter(effect::isInstance).findFirst().ifPresent(effect1 -> effect1.addTime(-1000000000));
    }

    @Override
    public void tick() {
        if (game.isPaused) return;
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

        //维修或破坏
        var target = chosen_item_display.get(player);
        if (generator_tick >= 0) {
            if (--generator_tick == 0 && target != null) {
                player.getLocation().getNearbyPlayers(20).forEach(player1 ->
                        player1.playSound(player1, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f));
                SignalRepeater signalRepeater = game.signal_repeaters.get(target);
                if (role instanceof Survivor survivor) {
                    if (!signalRepeater.isFixed())
                        signalRepeater.fix(survivor.getFixSpeed());
                } else if (role instanceof Hunter hunter && hunter.breakTick()) {
                    signalRepeater.destroy(hunter.getOtherFeature() == 0 ? 0.8 : 0.9);
                }
            }
            if (role instanceof Hunter) {
                addEffect(new Giddy(player, 1));
            }
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
    }

    /**
     * 将选择的技能改为下一个
     */
    public void changeSkillValue() {
        //蓄力技能取消
        role.removeSkillLocation(Role.ZERO_LOC);

        ++current_skill_id;
        skill_methods.forEach((id, m) -> {
            if (id == current_skill_id) {
                var a = m.getAnnotation(Skill.class);
                if (role instanceof Hunter hunter && a.minLevel() > hunter.getLevel()) {
                    ++current_skill_id;
                    return;
                }
                //找到
                current_skill_reset_cd = 20;
                if (skill_cd.get(current_skill_id) == 0) {
                    player.showTitle(
                            Title.title(
                                    Component.text("§a·".repeat(id)),
                                    Component.text(a.name(), TextColor.color(0, 255, 255)),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                            )
                    );
                } else {
                    player.showTitle(
                            Title.title(
                                    Component.text("§a·".repeat(id)),
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

    /**
     * 尝试使用选中的技能
     */
    public void useSkill() {
        skill_methods.forEach((id, m) -> {
            if (id == current_skill_id && skill_cd.get(id) == 0) {
                try {
                    current_skill_id = 0;
                    skill_cd.set(0, 1);
                    m.invoke(role);
                    clearTitle();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * 尝试进入管道
     */
    public void tryIntoDuct() {
        if (chosen_duct != null) player.teleport(chosen_duct.clone().setDirection(player.getLocation().getDirection()));
    }

    /**
     * 若是Hunter则升级
     */
    public void tryLevelUp() {
        if (!(role instanceof Hunter h)) return;
        h.levelUp();
    }

    public boolean canMove() {
        if (role instanceof Hunter h) {
            return health > h.getMaxHealth() * 0.2;
        }
        return true;
    }

}