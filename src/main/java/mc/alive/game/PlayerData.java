package mc.alive.game;

import mc.alive.role.Role;
import mc.alive.role.Skill;
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

public class PlayerData {
    private int current_skill_id = 0;
    private final Role role;
    private final Player player;
    private final List<Integer> skill_cd = new ArrayList<>() {{
        add(-1);
    }};

    public void tick() {
        for (int i = 0; i < skill_cd.size(); i++) {
            if (skill_cd.get(i) > 0) skill_cd.set(i, skill_cd.get(i) - 1);
        }
        if (skill_cd.getFirst() == 0) {
            current_skill_id = 0;
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
                        player.playSound(player, Sound.UI_BUTTON_CLICK,.3f,10f);
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
        this.player = player;
    }
}
