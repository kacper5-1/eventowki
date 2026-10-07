package pl.eventowki;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Buduje tymczasowe, w pelni zamkniete zamki z piasku (podloga, mury, dach) wokol gracza. */
public final class SandCastleManager {

    /** modified=false oznacza naturalny blok, ktory tylko chronimy (np. grunt pod podloga). */
    private record Saved(BlockData original, UUID owner, EventItem item, boolean modified) { }

    private record Castle(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                          EventItem item) {
        boolean inside(Location l) {
            return l.getWorld().equals(world)
                    && l.getBlockX() >= minX && l.getBlockX() <= maxX
                    && l.getBlockY() >= minY && l.getBlockY() <= maxY
                    && l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ;
        }
    }

    private final EventowkiPlugin plugin;
    private final Map<Location, Saved> tracked = new HashMap<>();
    private final List<Castle> castles = new ArrayList<>();

    public SandCastleManager(EventowkiPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return false jesli nie udalo sie postawic zadnego bloku (brak miejsca) */
    public boolean build(Player p, EventItem item) {
        Location base = p.getLocation().getBlock().getLocation();
        World w = base.getWorld();
        int r = Math.max(1, item.castleRadius);
        int h = Math.max(2, item.castleHeight);
        int bx = base.getBlockX(), by0 = base.getBlockY(), bz = base.getBlockZ();

        List<Location> all = new ArrayList<>();
        int[] built = {0};

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == r;
                boolean crenel = ring && item.castleBattlements && ((dx + dz) & 1) == 0;

                // podloga (y = -1) pod calym obrysem, mury (y = 0..h-1), dach (y = h), krenelaz (y = h)
                int from = item.castleFloor ? -1 : 0;
                int top;
                if (ring) {
                    top = h + (crenel ? 1 : 0);
                } else {
                    top = item.castleRoof ? h + 1 : 0;
                }
                for (int y = from; y < top; y++) {
                    if (!ring && y >= 0 && y < h) continue; // srodek zostaje pusty (poza podloga i dachem)
                    int by = by0 + y;
                    if (by < w.getMinHeight() || by >= w.getMaxHeight()) continue;
                    place(w.getBlockAt(bx + dx, by, bz + dz), p, item, all, built);
                }
            }
        }

        if (built[0] == 0) { // nic nie postawiono - wycofaj ochrone naturalnych blokow
            all.forEach(tracked::remove);
            return false;
        }

        Castle castle = new Castle(w, bx - r + 1, bx + r - 1, by0, by0 + h - 1, bz - r + 1, bz + r - 1, item);
        castles.add(castle);
        w.playSound(base, Sound.BLOCK_SAND_PLACE, 1.5f, 0.8f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Location l : all) {
                Saved s = tracked.remove(l);
                if (s != null && s.modified()) {
                    l.getBlock().setBlockData(s.original(), false);
                }
            }
            castles.remove(castle);
        }, Math.max(1, item.durationSeconds) * 20L);
        return true;
    }

    private void place(Block b, Player p, EventItem item, List<Location> all, int[] built) {
        Location loc = b.getLocation();
        if (canReplace(b)) {
            tracked.put(loc, new Saved(b.getBlockData(), p.getUniqueId(), item, true));
            b.setType(item.castleMaterial, false);
            built[0]++;
        } else if (b.getType().isSolid()) {
            // naturalny, pelny blok juz tworzy sciane/podloge - zostaje, ale jest chroniony
            tracked.put(loc, new Saved(b.getBlockData(), p.getUniqueId(), item, false));
        } else {
            return;
        }
        all.add(loc);
    }

    private boolean canReplace(Block b) {
        if (b.getType().isAir() || b.isReplaceable()) return true;
        if (b.getType().isSolid()) return false;
        return !(b.getState() instanceof TileState);
    }

    public boolean isTracked(Block b) {
        return tracked.containsKey(b.getLocation());
    }

    public void untrack(Block b) {
        tracked.remove(b.getLocation());
    }

    /** Czy ten gracz moze zniszczyc ten blok zamku. */
    public boolean canBreak(Player p, Block b) {
        Saved s = tracked.get(b.getLocation());
        if (s == null || !s.item().castleUnbreakable) return true;
        return s.item().castleOwnerCanBreak && s.owner().equals(p.getUniqueId());
    }

    public boolean isExplosionProof(Block b) {
        Saved s = tracked.get(b.getLocation());
        return s != null && s.item().castleExplosionProof;
    }

    /** Perla/chorus nie moze teleportowac nikogo do wnetrza zamku z zewnatrz. */
    public boolean blocksTeleport(Location from, Location to) {
        for (Castle c : castles) {
            if (c.item().castleBlockPearls && c.inside(to) && !c.inside(from)) return true;
        }
        return false;
    }

    public void clear() {
        for (Map.Entry<Location, Saved> e : tracked.entrySet()) {
            if (e.getValue().modified()) {
                e.getKey().getBlock().setBlockData(e.getValue().original(), false);
            }
        }
        tracked.clear();
        castles.clear();
    }
}
