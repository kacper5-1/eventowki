package pl.eventowki;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class EventowkaCommand implements CommandExecutor, TabCompleter {

    private final EventowkiPlugin plugin;

    public EventowkaCommand(EventowkiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission("eventowki.admin")) {
            s.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (a.length == 0) {
            s.sendMessage(net.kyori.adventure.text.Component.text("/eventowka <give|list|reload>"));
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                s.sendMessage(plugin.msg("reloaded"));
            }
            case "list" -> s.sendMessage(net.kyori.adventure.text.Component.text(
                    "Eventowki: " + String.join(", ", plugin.itemIds())));
            case "give" -> {
                if (a.length < 3) {
                    s.sendMessage(net.kyori.adventure.text.Component.text("/eventowka give <gracz> <id> [ilosc]"));
                    return true;
                }
                Player target = Bukkit.getPlayer(a[1]);
                EventItem item = plugin.getItem(a[2]);
                if (target == null || item == null) {
                    s.sendMessage(net.kyori.adventure.text.Component.text("Nie ma takiego gracza lub eventowki."));
                    return true;
                }
                int amount = 1;
                if (a.length >= 4) {
                    try { amount = Integer.parseInt(a[3]); } catch (NumberFormatException ignored) { }
                }
                ItemStack stack = plugin.createItem(item, amount);
                target.getInventory().addItem(stack).values()
                        .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
                s.sendMessage(net.kyori.adventure.text.Component.text(
                        "Dano " + amount + "x " + item.id + " graczowi " + target.getName()));
            }
            default -> s.sendMessage(net.kyori.adventure.text.Component.text("/eventowka <give|list|reload>"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String o : List.of("give", "list", "reload")) if (o.startsWith(a[0].toLowerCase())) out.add(o);
        } else if (a.length == 2 && a[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> { if (p.getName().toLowerCase().startsWith(a[1].toLowerCase())) out.add(p.getName()); });
        } else if (a.length == 3 && a[0].equalsIgnoreCase("give")) {
            for (String id : plugin.itemIds()) if (id.startsWith(a[2].toLowerCase())) out.add(id);
        }
        return out;
    }
}
