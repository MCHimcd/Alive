package mc.alive;

import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.*;

public class StoredData implements Serializable {
    public static Map<Player, StoredData> playerStoredData = new HashMap<>();
    public static List<StoredData> data = new LinkedList<>();
    private final Date date = new Date();
    private final String name;
    private final Map<Option, OptionValue<?>> options = new HashMap<>() {{
        put(Option.FEATURE, new OptionValue<>(0));
    }};

    public StoredData(String name) {
        this.name = name;
    }

    public StoredData updateDate() {
        date.setTime(System.currentTimeMillis());
        return this;
    }

    public boolean isValid() {
        //7 days
        return System.currentTimeMillis() - date.getTime() < 7 * 24 * 60 * 60 * 1000;
    }

    public String getName() {
        return name;
    }

    public <T extends Serializable> void setOption(Option option, T value) {
        options.put(option, new OptionValue<>(value));
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getOption(Option option) {
        OptionValue<T> optionValue = (OptionValue<T>) options.get(option);
        if (optionValue == null) return null;
        return optionValue.value;
    }

    public enum Option {
        FEATURE
    }

    public record OptionValue<T extends Serializable>(T value) implements Serializable {
    }
}
