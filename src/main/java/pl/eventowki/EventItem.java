package pl.eventowki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Definicja jednej eventowki wczytana z config.yml. */
public final class EventItem {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public final String id;
    public final String type;
    public final Material material;
    public final int customModelData;
    public final String name;
    public final List<String> lore;
    public final int cooldownSeconds;
    public final double radius;
    public final int durationSeconds;
    public final boolean affectOwner;
    public final int blockAfterLeaveSeconds;
    public final int castleRadius;
    public final int castleHeight;
    public final Material castleMaterial;
    public final boolean castleBattlements;
    public final boolean castleRoof;
    public final boolean castleFloor;
    public final boolean castleUnbreakable;
    public final boolean castleOwnerCanBreak;
    public final boolean castleExplosionProof;
    public final boolean castleBlockPearls;
    public final boolean consume;

    public EventItem(String id, ConfigurationSection s) {
        this.id = id;
        this.type = s.getString("type", "DEATH_FIELD").toUpperCase();
        Material m = Material.matchMaterial(s.getString("material", "RED_DYE"));
        this.material = m == null ? Material.RED_DYE : m;
        this.customModelData = s.getInt("custom-model-data", 0);
        this.name = s.getString("name", id);
        this.lore = s.getStringList("lore");
        this.cooldownSeconds = s.getInt("cooldown-seconds", 30);
        this.radius = s.getDouble("radius", 5.0);
        this.durationSeconds = s.getInt("duration-seconds", 5);
        this.affectOwner = s.getBoolean("affect-owner", false);
        this.blockAfterLeaveSeconds = s.getInt("block-after-leave-seconds", 5);
        this.castleRadius = s.getInt("castle-radius", 2);
        this.castleHeight = s.getInt("castle-height", 3);
        Material cm = Material.matchMaterial(s.getString("castle-material", "SAND"));
        this.castleMaterial = (cm == null || !cm.isBlock()) ? Material.SAND : cm;
        this.castleBattlements = s.getBoolean("castle-battlements", true);
        this.castleRoof = s.getBoolean("castle-roof", true);
        this.castleFloor = s.getBoolean("castle-floor", true);
        this.castleUnbreakable = s.getBoolean("castle-unbreakable", true);
        this.castleOwnerCanBreak = s.getBoolean("castle-owner-can-break", false);
        this.castleExplosionProof = s.getBoolean("castle-explosion-proof", true);
        this.castleBlockPearls = s.getBoolean("castle-block-pearls", true);
        this.consume = s.getBoolean("consume", true);
    }

    public ItemStack create(NamespacedKey key, int amount) {
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String l : lore) {
            lines.add(MM.deserialize(l).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        stack.setItemMeta(meta);
        return stack;
    }
}
