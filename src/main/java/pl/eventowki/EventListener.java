package pl.eventowki;

import io.papermc.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class EventListener implements Listener {

    private final EventowkiPlugin plugin;
    private final FieldManager fields;
    private final SandCastleManager castles;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public EventListener(EventowkiPlugin plugin, FieldManager fields, SandCastleManager castles) {
        this.plugin = plugin;
        this.fields = fields;
        this.castles = castles;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        EventItem item = plugin.getItem(hand);
        if (item == null) return;

        e.setCancelled(true); // nie pozwalaj np. uzyc barwnika na zwierzeciu/bloku

        String ck = p.getUniqueId() + ":" + item.id;
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(ck, 0L);
        if (readyAt > now) {
            long left = (long) Math.ceil((readyAt - now) / 1000.0);
            p.sendActionBar(plugin.msg("cooldown", "{time}", String.valueOf(left)));
            return;
        }

        if ("DEATH_FIELD".equals(item.type)) {
            fields.place(p, p.getLocation(), item);
        } else if ("SAND_CASTLE".equals(item.type)) {
            if (!castles.build(p, item)) {
                p.sendActionBar(plugin.msg("no-space"));
                return;
            }
        } else {
            return;
        }

        cooldowns.put(ck, now + item.cooldownSeconds * 1000L);
        if (item.consume) {
            hand.setAmount(hand.getAmount() - 1);
        }
    }

    /** Piasek z zamku nie moze spadac ani sie osypywac. */
    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityChangeBlockEvent e) {
        if (e.getEntity() instanceof FallingBlock && castles.isTracked(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    /** Mury zamku sa niezniszczalne (opcjonalnie poza wlascicielem); nic nie dropi. */
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!castles.isTracked(e.getBlock())) return;
        if (!castles.canBreak(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(plugin.msg("castle-protected"));
            return;
        }
        e.setDropItems(false);
        castles.untrack(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(castles::isExplosionProof);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(castles::isExplosionProof);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (castles.isTracked(b)) { e.setCancelled(true); return; }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (castles.isTracked(b)) { e.setCancelled(true); return; }
        }
    }

    /** Zakaz wejscia do zamku perla / chorusem. */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        PlayerTeleportEvent.TeleportCause c = e.getCause();
        if (c != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && c != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) return;
        if (e.getTo() != null && castles.blocksTeleport(e.getFrom(), e.getTo())) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(plugin.msg("castle-protected"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.isGliding() && fields.isBlocked(p)) {
            e.setCancelled(true);
            p.sendActionBar(plugin.msg("elytra-blocked"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBoost(PlayerElytraBoostEvent e) {
        if (fields.isBlocked(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
}
