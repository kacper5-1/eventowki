package pl.eventowki;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Przechowuje aktywne Pola Smierci i pilnuje, zeby w nich nie dzialala elytra. */
public final class FieldManager {

    private static final class Field {
        final UUID owner;
        final World world;
        final double x, y, z, radius;
        final long expiresAt;
        final boolean affectOwner;
        final long lingerMillis;

        Field(UUID owner, Location loc, double radius, long expiresAt, boolean affectOwner, long lingerMillis) {
            this.owner = owner;
            this.world = loc.getWorld();
            this.x = loc.getX();
            this.y = loc.getY();
            this.z = loc.getZ();
            this.radius = radius;
            this.expiresAt = expiresAt;
            this.affectOwner = affectOwner;
            this.lingerMillis = lingerMillis;
        }

        boolean affects(Player p) {
            if (!p.getWorld().equals(world)) return false;
            if (!affectOwner && p.getUniqueId().equals(owner)) return false;
            Location l = p.getLocation();
            double dx = l.getX() - x, dy = l.getY() - y, dz = l.getZ() - z;
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }
    }

    private final EventowkiPlugin plugin;
    private final List<Field> fields = new LinkedList<>();
    /** Gracz -> do kiedy (ms) ma zablokowana elytre. Odswiezane, dopoki stoi w polu. */
    private final Map<UUID, Long> blockedUntil = new HashMap<>();

    public FieldManager(EventowkiPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    public void place(Player owner, Location loc, EventItem item) {
        long expires = System.currentTimeMillis() + item.durationSeconds * 1000L;
        fields.add(new Field(owner.getUniqueId(), loc.clone(), item.radius, expires, item.affectOwner,
                item.blockAfterLeaveSeconds * 1000L));
    }

    /**
     * Gracz ma zablokowana elytre, gdy jest w aktywnym polu LUB wyszedl z niego
     * mniej niz block-after-leave-seconds temu.
     */
    public boolean isBlocked(Player p) {
        long now = System.currentTimeMillis();
        for (Field f : fields) {
            if (f.expiresAt > now && f.affects(p)) {
                // w polu: odswiez blokade (liczy sie od momentu wyjscia)
                blockedUntil.merge(p.getUniqueId(), now + f.lingerMillis, Math::max);
                return true;
            }
        }
        Long until = blockedUntil.get(p.getUniqueId());
        return until != null && until > now;
    }

    public void clear() {
        fields.clear();
        blockedUntil.clear();
    }

    private void tick() {
        if (fields.isEmpty() && blockedUntil.isEmpty()) return;
        long now = System.currentTimeMillis();

        Iterator<Field> it = fields.iterator();
        while (it.hasNext()) {
            Field f = it.next();
            if (f.expiresAt <= now) {
                it.remove();
                continue;
            }
            drawRing(f);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean blocked = isBlocked(p); // odswieza blokade graczom stojacym w polu
            if (blocked && p.isGliding()) {
                p.setGliding(false);
                p.sendActionBar(plugin.msg("elytra-blocked"));
            }
        }

        blockedUntil.values().removeIf(until -> until <= now);
    }

    private void drawRing(Field f) {
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.6f);
        int points = Math.max(24, (int) (f.radius * 8));
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            double px = f.x + Math.cos(a) * f.radius;
            double pz = f.z + Math.sin(a) * f.radius;
            f.world.spawnParticle(Particle.DUST, px, f.y + 0.2, pz, 1, 0, 0, 0, 0, dust);
        }
        f.world.spawnParticle(Particle.DUST, f.x, f.y + 0.5, f.z, 6,
                f.radius / 3, 0.4, f.radius / 3, 0, dust);
    }
}
