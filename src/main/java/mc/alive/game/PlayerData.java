package mc.alive.game;

import mc.alive.role.Role;
import mc.alive.role.Skill;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.survivor.Survivor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mc.alive.Alive.game;
import static mc.alive.game.TickRunner.chosen_duct;
import static mc.alive.game.TickRunner.chosen_item_display;

public class PlayerData {
    private int current_skill_id = 0;
    private final Role role;
    private final Player player;
    private final List<Integer> skill_cd = new ArrayList<>() {{
        add(-1);
    }};
    private double health;
    private double shield;
    private int shield_cd = 0;
    private int shield_tick = 0;
    public int fix_tick = -1;

    public Role getRole() {
        return role;
    }

    public static PlayerData getPlayerData(Player player) {
        assert game != null;
        return game.playerData.get(player);
    }

    public void tick() {
        //技能冷却
        for (int i = 0; i < skill_cd.size(); i++) {
            if (skill_cd.get(i) > 0) skill_cd.set(i, skill_cd.get(i) - 1);
        }
        //技能选择
        if (skill_cd.getFirst() == 0) {
            current_skill_id = 0;
        }
        //护盾恢复
        if (role instanceof Survivor s) {
            if (shield_cd > 0) shield_cd--;
            if (shield_tick > 0) shield_tick--;
            if (shield_cd == 0 && shield_tick == 0 && shield < s.getMaxShield()) {
                shield = Math.min(shield + 5, s.getMaxShield());
                shield_tick = 20;
            }
        }
        //维修
        var target = chosen_item_display.get(player);
        if (fix_tick >= 0) {
            if (--fix_tick == 0 && target != null) {
                player.getLocation().getNearbyPlayers(20).forEach(player1 -> {
                    player1.playSound(player1,Sound.ENTITY_IRON_GOLEM_REPAIR,1f,1f);
                });
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
                        skill_cd.set(0, 20);
                        player.showTitle(
                                Title.title(
                                        Component.text("§a·".repeat(a.id())),
                                        Component.text(a.name(), TextColor.color(0, 255, 255)),
                                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                                )
                        );
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
                        return a.id() == current_skill_id;
                    }
                    return false;
                })
                .forEach(m -> {
                    try {
                        m.invoke(role);
                        current_skill_id = 0;
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
        this.role = role;
        health = role.getMaxHealth();
        if (role instanceof Survivor s) shield = s.getMaxShield();
        this.player = player;
    }

    public void damage(double amount) {
        if (amount >= 0) {
            if (role instanceof Survivor) {
                if (shield > 0) {
                    //减护盾
                    var amount_s = amount;
                    amount = Math.max(0, amount - shield);
                    shield = Math.max(0, shield - amount_s);
                    shield_cd = 200;
                }
            }
            //减血量
            health -= amount;
            if (health <= 0) die();
        } else health = Math.min(health - amount, role.getMaxHealth());
    }

    public void tryIntoDuct() {
        if (chosen_duct != null) player.teleport(chosen_duct);
    }


    public void tryLevelUp() {
        if (!(role instanceof Hunter h)) return;
        h.levelUp();
    }

    private void die() {
        game.survivors.remove(player);
        if (game.survivors.isEmpty()) {
            game.end();
        }
    }
}
