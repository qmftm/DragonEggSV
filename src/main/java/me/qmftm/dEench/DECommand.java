package me.qmftm.dEench;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;

/**
 * Handles {@code /DE config overench <enchant> <level>}.
 */
public class DECommand implements CommandExecutor, TabCompleter {

    private final DEench plugin;

    public DECommand(DEench plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("start")) {
            return handleStart(sender, label, args);
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("config") || !args[1].equalsIgnoreCase("overench")) {
            sendUsage(sender, label);
            return true;
        }

        if (args.length < 4) {
            sendUsage(sender, label);
            return true;
        }

        Enchantment ench = DEench.resolveEnchantment(args[2]);
        if (ench == null) {
            sender.sendMessage(Component.text("Unknown enchantment: " + args[2], NamedTextColor.RED));
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Level must be a whole number: " + args[3], NamedTextColor.RED));
            return true;
        }

        int vanillaMax = ench.getMaxLevel();
        if (level < vanillaMax) {
            sender.sendMessage(Component.text(
                    "Over level (" + level + ") cannot be lower than the vanilla max ("
                            + vanillaMax + ") for " + ench.getKey().getKey() + ".",
                    NamedTextColor.RED));
            return true;
        }

        plugin.setOverench(ench, level);
        sender.sendMessage(Component.text(
                "Set over-enchant max for " + ench.getKey().getKey() + " to " + level + ".",
                NamedTextColor.GREEN));
        return true;
    }

    private boolean handleStart(CommandSender sender, String label, String[] args) {
        int startDay = 1;
        if (args.length >= 2) {
            try {
                startDay = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Start day must be a whole number: " + args[1], NamedTextColor.RED));
                return true;
            }
            if (startDay < 1) {
                sender.sendMessage(Component.text("Start day must be at least 1.", NamedTextColor.RED));
                return true;
            }
        }

        plugin.getGameClock().startGame(startDay);
        sender.sendMessage(Component.text(
                "DragonEggSV started at day " + startDay + ".", NamedTextColor.GREEN));
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text(
                "Usage: /" + label + " start [day]  |  /" + label + " config overench <enchant> <level>",
                NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of("config", "start"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
            return filter(List.of("overench"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("overench")) {
            List<String> names = new ArrayList<>();
            for (Enchantment ench : Registry.ENCHANTMENT) {
                names.add(ench.getKey().getKey());
            }
            return filter(names, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("overench")) {
            Enchantment ench = DEench.resolveEnchantment(args[2]);
            if (ench != null) {
                // Suggest the lowest allowed value (the vanilla max).
                return List.of(String.valueOf(ench.getMaxLevel()));
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
