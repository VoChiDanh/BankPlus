package me.pulsi_.bankplus.listeners.playerChat;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.account.BPPlayer;
import me.pulsi_.bankplus.bankSystem.BankUtils;
import me.pulsi_.bankplus.economy.BPEconomy;
import me.pulsi_.bankplus.utils.BPMessages;
import me.pulsi_.bankplus.utils.BPSets;
import me.pulsi_.bankplus.utils.BPUtils;
import me.pulsi_.bankplus.values.Values;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

import java.math.BigDecimal;

public class PlayerChatMethod {

    public static void process(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!isTyping(p)) return;

        BPPlayer player = BankPlus.INSTANCE.getPlayerRegistry().get(p);
        String identifier = player.getOpenedBank().getIdentifier();
        String message = ChatColor.stripColor(e.getMessage());

        if (hasTypedExit(message, p, e, identifier)) return;
        e.setCancelled(true);

        BigDecimal amount;
        try {
            amount = new BigDecimal(message);
        } catch (NumberFormatException ex) {
            BPMessages.send(p, "Invalid-Number");
            return;
        }

        BPEconomy economy = BankPlus.getBPEconomy();

        if (BPUtils.isDepositing(p)) {
            BPSets.removePlayerFromDepositing(p);
            economy.deposit(p, amount, identifier);
        }
        if (BPUtils.isWithdrawing(p)) {
            BPSets.removePlayerFromWithdrawing(p);
            economy.withdraw(p, amount, identifier);
        }
        reopenBank(p, identifier);
    }

    private static boolean hasTypedExit(String message, Player p, AsyncPlayerChatEvent e, String identifier) {
        if (isTyping(p) && !message.toLowerCase().contains(Values.CONFIG.getChatExitMessage().toLowerCase())) return false;
        e.setCancelled(true);
        executeExitCommands(p);
        reopenBank(p, identifier);
        return true;
    }

    private static boolean isTyping(Player p) {
        return BPUtils.isDepositing(p) || BPUtils.isWithdrawing(p);
    }

    public static void reopenBank(Player p, String identifier) {
        Bukkit.getScheduler().runTask(BankPlus.INSTANCE, () -> {
            BukkitTask task = BankPlus.INSTANCE.getPlayerRegistry().get(p).getClosingTask();
            if (task != null) task.cancel();

            BPSets.playerDepositing.remove(p.getUniqueId());
            BPSets.playerWithdrawing.remove(p.getUniqueId());
            if (Values.CONFIG.isReopeningBankAfterChat()) BankUtils.openBank(p, identifier, true);
        });
    }

    private static void executeExitCommands(Player p) {
        Bukkit.getScheduler().runTask(BankPlus.INSTANCE, () -> {
            for (String cmd : Values.CONFIG.getExitCommands()) {
                if (cmd.startsWith("[CONSOLE]")) {
                    String s = cmd.replace("[CONSOLE] ", "").replace("%player%", p.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
                }
                if (cmd.startsWith("[PLAYER]")) {
                    String s = cmd.replace("[PLAYER] ", "");
                    p.chat("/" + s);
                }
            }
        });
    }
}
