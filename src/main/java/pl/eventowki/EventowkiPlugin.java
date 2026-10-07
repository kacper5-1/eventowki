package pl.eventowki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EventowkiPlugin extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private NamespacedKey itemKey;
    private final Map<String, EventItem> items = new LinkedHashMap<>();
    private FieldManager fields;
    private SandCastleManager castles;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        itemKey = new NamespacedKey(this, "eventowka");
        loadItems();

        fields = new FieldManager(this);
        fields.start();
        castles = new SandCastleManager(this);
        getServer().getPluginManager().registerEvents(new EventListener(this, fields, castles), this);

        EventowkaCommand cmd = new EventowkaCommand(this);
        getCommand("eventowka").setExecutor(cmd);
        getCommand("eventowka").setTabCompleter(cmd);
    }

    @Override
    public void onDisable() {
        if (fields != null) fields.clear();
        if (castles != null) castles.clear();
    }

    public void reload() {
        reloadConfig();
        loadItems();
    }

    private void loadItems() {
        items.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("items");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s != null) items.put(id.toLowerCase(), new EventItem(id.toLowerCase(), s));
        }
    }

    public EventItem getItem(String id) {
        return items.get(id.toLowerCase());
    }

    /** Zwraca definicje eventowki jesli ItemStack jest eventowka, inaczej null. */
    public EventItem getItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        String id = stack.getItemMeta().getPersistentDataContainer()
                .get(itemKey, PersistentDataType.STRING);
        return id == null ? null : items.get(id);
    }

    public ItemStack createItem(EventItem item, int amount) {
        return item.create(itemKey, amount);
    }

    public Collection<String> itemIds() {
        return items.keySet();
    }

    public Component msg(String key, String... replacements) {
        String raw = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return MM.deserialize(raw);
    }
}
