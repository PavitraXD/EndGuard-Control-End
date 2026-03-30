package com.endmastercontrol.command;

import com.endmastercontrol.config.ConfigManager;
import com.endmastercontrol.control.EndController;
import com.endmastercontrol.control.TimerManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class EndCommand implements CommandExecutor, TabCompleter {

    private final EndController endController;
    private final ConfigManager configManager;
    private final TimerManager timerManager;

    public EndCommand(EndController endController, ConfigManager configManager, TimerManager timerManager) {
        this.endController = endController;
        this.configManager = configManager;
        this.timerManager = timerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("end.admin")) {
            sender.sendMessage(configManager.message("noPermission"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "lock" -> endController.lockEnd(sender);
            case "unlock" -> endController.unlockEnd(sender, true);
            case "status" -> sendStatus(sender);
            case "dragon" -> handleDragon(sender, args);
            case "timer" -> handleTimer(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleDragon(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /end dragon <on|off>"));
            return;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        if ("on".equals(mode)) {
            endController.setDragonEnabled(sender, true, true);
        } else if ("off".equals(mode)) {
            endController.setDragonEnabled(sender, false, true);
        } else {
            sender.sendMessage(Component.text("Usage: /end dragon <on|off>"));
        }
    }

    private void handleTimer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /end timer <minutes>"));
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("Minutes must be a whole number."));
            return;
        }

        if (minutes <= 0) {
            sender.sendMessage(Component.text("Minutes must be greater than 0."));
            return;
        }

        endController.scheduleTimer(sender, minutes);
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(Component.text("End: ").append(configManager.message(endController.isEndLocked() ? "statusLocked" : "statusUnlocked")));
        sender.sendMessage(Component.text("Dragon: ").append(configManager.message(endController.isDragonEnabled() ? "statusDragonOn" : "statusDragonOff")));

        if (timerManager.hasActiveTimer() && timerManager.getActiveAction() != null) {
            sender.sendMessage(Component.text(
                "Timer: " + timerManager.getActiveAction().description() + " in " + timerManager.getRemainingSeconds() + "s"
            ));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /end <lock|unlock|dragon on|dragon off|status|timer <minutes>>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!sender.hasPermission("end.admin")) {
            return suggestions;
        }

        if (args.length == 1) {
            return partial(args[0], List.of("lock", "unlock", "dragon", "status", "timer"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("dragon")) {
            return partial(args[1], List.of("on", "off"));
        }

        return suggestions;
    }

    private List<String> partial(String input, List<String> values) {
        String lowered = input.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(value -> value.startsWith(lowered))
            .toList();
    }
}
