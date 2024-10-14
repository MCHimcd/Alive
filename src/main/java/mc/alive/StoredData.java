package mc.alive;

import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StoredData implements Serializable {
    public static Map<Player, StoredData> playerStoredData = new HashMap<>();
    public static List<StoredData> data = new LinkedList<>();
    private final String name;
    private final Map<String, Integer> options = new HashMap<>() {{
        put("feature", 0);
    }};

    public StoredData(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setOption(String option, int value) {
        options.put(option, value);
    }

    public int getOption(String option) {
        return options.get(option);
    }
}
