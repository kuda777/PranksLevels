
package me.milagros.pranklevels;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;

public class PrankLevelsPlugin extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, PlayerData> prankData = new HashMap<>();
    private final Map<Integer, ChatColor> levelColors = Map.of(
            1, ChatColor.GRAY,
            2, ChatColor.GREEN,
            3, ChatColor.YELLOW,
            4, ChatColor.GOLD,
            5, ChatColor.RED
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("prank").setExecutor(this);
        getCommand("setpranklevel").setExecutor(this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            prankData.putIfAbsent(player.getUniqueId(), new PlayerData());
            updateTab(player);
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!checkLimit(event.getPlayer(), "break")) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!checkLimit(event.getPlayer(), "place")) event.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Player p && !checkLimit(p, "explosion")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLavaUse(PlayerBucketEmptyEvent event) {
        Material bucket = event.getBucket();
        if ((bucket == Material.LAVA_BUCKET || bucket == Material.WATER_BUCKET) && !checkLimit(event.getPlayer(), "bucket")) event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!checkLimit(event.getPlayer(), "interact")) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityKill(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && event.getEntity() instanceof Player && !checkLimit(p, "kill")) {
            event.setCancelled(true);
        }
    }

    private boolean checkLimit(Player player, String action) {
        PlayerData data = prankData.get(player.getUniqueId());
        if (data == null || !data.pranking) return true;
        return data.recordAction(action);
    }

    private void updateTab(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("pranklvl", "dummy", ChatColor.BOLD + "Prank");
        obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        PlayerData data = prankData.get(player.getUniqueId());
        int level = data != null ? data.level : 1;
        obj.getScore(player.getName()).setScore(level);
        player.setScoreboard(board);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("prank")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Este comando solo puede usarse en el juego.");
                return true;
            }
            Player player = (Player) sender;
            prankData.putIfAbsent(player.getUniqueId(), new PlayerData());
            PlayerData data = prankData.get(player.getUniqueId());
            data.pranking = !data.pranking;
            player.sendMessage(ChatColor.AQUA + "Modo prank " + (data.pranking ? "activado" : "desactivado"));
            return true;
        } else if (command.getName().equalsIgnoreCase("setpranklevel")) {
            if (args.length != 2) return false;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return true;
            }
            try {
                int newLevel = Integer.parseInt(args[1]);
                prankData.putIfAbsent(target.getUniqueId(), new PlayerData());
                prankData.get(target.getUniqueId()).level = newLevel;
                updateTab(target);
                sender.sendMessage(ChatColor.GREEN + "Nivel de prank de " + target.getName() + " actualizado a " + newLevel);
                target.sendMessage(ChatColor.GREEN + "Tu nivel de prank ahora es " + newLevel);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "El nivel debe ser un número.");
            }
            return true;
        }
        return false;
    }

    static class PlayerData {
        int level = 1;
        boolean pranking = false;
        Map<String, Integer> actions = new HashMap<>();

        boolean recordAction(String type) {
            int used = actions.getOrDefault(type, 0);
            int max = getLimit(type);
            if (used >= max) return false;
            actions.put(type, used + 1);
            return true;
        }

        int getLimit(String type) {
            return switch (type) {
                case "break" -> level * 10;
                case "place" -> level * 5;
                case "explosion" -> level;
                case "bucket" -> level * 2;
                case "kill" -> level * 3;
                case "interact" -> level * 5;
                default -> 0;
            };
        }
    }
}
