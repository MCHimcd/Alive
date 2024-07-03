package mc.alive.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public final class Message {
    public static final MiniMessage msg = MiniMessage.miniMessage();

    public static LinkedList<Component> convertMsg(List<String> sl) {
        return sl.stream().map(msg::deserialize).collect(Collectors.toCollection(LinkedList::new));
    }

    public static Component rMsg(String s) {
        return msg.deserialize("<reset>" + s);
    }

    public static Component rMsg(String s, NamedTextColor color) {
        return Component.text(s, color).decoration(TextDecoration.ITALIC, false);
    }

    public static Title title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return Title.title(rMsg(title), rMsg(subtitle), Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut)));
    }
}