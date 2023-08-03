package me.pulsi_.bankplus.utils;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.account.economy.MultiEconomyManager;
import me.pulsi_.bankplus.account.economy.SingleEconomyManager;
import me.pulsi_.bankplus.bankSystem.BankReader;
import me.pulsi_.bankplus.managers.ConfigManager;
import me.pulsi_.bankplus.managers.TaskManager;
import me.pulsi_.bankplus.values.Values;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BPMethods {

    public static String formatTime(long milliseconds) {
        if (!Values.CONFIG.isInterestEnabled()) return BPChat.color("&cInterest disabled.");

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        String format = Values.CONFIG.getInterestTimeFormat();
        String[] parts = format.split("%");
        int amount = parts.length;

        for (int i = 1; i < amount; i++) {
            String identifier = parts[i];
            long time = 0;

            switch (identifier) {
                case "s":
                    time = seconds - (minutes * 60);
                    break;
                case "m":
                    time = minutes - (hours * 60);
                    break;
                case "h":
                    time = hours - (days * 24);
                    break;
                case "d":
                    time = days;
                    break;
            }
            if (time <= 0) format = format.replace("%" + identifier, "");
        }

        parts = format.split("%");
        amount = parts.length;

        for (int i = 1; i < amount; i++) {
            String identifier = parts[i], timeIdentifier = "";
            long time = 0;

            switch (identifier) {
                case "s":
                    time = seconds - (minutes * 60);
                    timeIdentifier = "seconds";
                    break;
                case "m":
                    time = minutes - (hours * 60);
                    timeIdentifier = "minutes";
                    break;
                case "h":
                    time = hours - (days * 24);
                    timeIdentifier = "hours";
                    break;
                case "d":
                    time = days;
                    timeIdentifier = "days";
                    break;
            }

            int last = i + 2;
            String separator = last > amount ? "" : last == amount ? Values.CONFIG.getInterestTimeFinalSeparator() : Values.CONFIG.getInterestTimeSeparator();
            String replacer = time <= 0 ? "" : time + getTimeIdentifier(timeIdentifier, time) + separator;

            format = format.replace("%" + identifier, replacer);
        }

        return format;
    }

    private static String getTimeIdentifier(String id, long time) {
        switch (id) {
            case "seconds":
                return time == 1 ? Values.CONFIG.getSecond() : Values.CONFIG.getSeconds();
            case "minutes":
                return time == 1 ? Values.CONFIG.getMinute() : Values.CONFIG.getMinutes();
            case "hours":
                return time == 1 ? Values.CONFIG.getHour() : Values.CONFIG.getHours();
            case "days":
                return time == 1 ? Values.CONFIG.getDay() : Values.CONFIG.getDays();
        }
        return "";
    }

    /**
     * BankPlus does not accept negative numbers, if a number is lower than 0, it will return true.
     *
     * @param number The number to check.
     * @return true if is invalid or false if is not.
     */
    public static boolean isInvalidNumber(String number) {
        return isInvalidNumber(number, null);
    }

    /**
     * BankPlus does not accept negative numbers, if a number is lower than 0, it will return true.
     *
     * @param number The number to check.
     * @param s      The command sender to automatically alert if number is invalid.
     * @return true if is invalid or false if is not.
     */
    public static boolean isInvalidNumber(String number, CommandSender s) {
        if (number == null) {
            BPMessages.send(s, "Invalid-Number");
            return true;
        }
        if (number.contains("%")) number = number.replace("%", "");

        try {
            BigDecimal num = new BigDecimal(number);
            if (num.doubleValue() < 0) {
                BPMessages.send(s, "Cannot-Use-Negative-Number");
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            BPMessages.send(s, "Invalid-Number");
            return true;
        }
    }

    public static boolean isDepositing(Player p) {
        return BPSets.playerDepositing.contains(p.getUniqueId());
    }

    public static boolean isWithdrawing(Player p) {
        return BPSets.playerWithdrawing.contains(p.getUniqueId());
    }

    public static boolean isPlayer(CommandSender s) {
        if (s instanceof Player) return true;
        BPMessages.send(s, "Not-Player");
        return false;
    }

    public static boolean hasPermission(CommandSender s, String permission) {
        if (s.hasPermission(permission)) return true;
        BPMessages.send(s, "No-Permission", "%permission%$" + permission);
        return false;
    }

    public static void startSavingBalancesTask() {
        TaskManager tasks = BankPlus.INSTANCE.getTaskManager();
        BukkitTask task = tasks.getSavingTask();
        if (task != null) task.cancel();

        if (Values.CONFIG.getSaveBalancedDelay() <= 0) return;

        // Cache the values out the runnable to improve a bit the performance.
        long delay = Values.CONFIG.getSaveBalancedDelay();
        boolean multi = Values.MULTIPLE_BANKS.isMultipleBanksModuleEnabled(), saveBroadcast = Values.CONFIG.isSaveBalancesBroadcast();

        tasks.setSavingTask(Bukkit.getScheduler().runTaskTimer(BankPlus.INSTANCE, () -> {
            if (multi) Bukkit.getOnlinePlayers().forEach(p -> new MultiEconomyManager(p).saveBankBalance(true));
            else Bukkit.getOnlinePlayers().forEach(p -> new SingleEconomyManager(p).saveBankBalance(true));
            if (saveBroadcast) BPLogger.info("All player balances have been saved!");
        }, delay * 1200L, delay * 1200L));
    }

    public static void customWithdraw(Player p) {
        customWithdraw(p, Values.CONFIG.getMainGuiName());
    }

    public static void customWithdraw(Player p, String identifier) {
        if (!hasPermission(p, "bankplus.withdraw")) return;

        if (Values.MESSAGES.isTitleCustomAmountEnabled())
            BPMethods.sendTitle(BankPlus.INSTANCE.getConfigManager().getConfig(ConfigManager.Type.MESSAGES).getString("Title-Custom-Transaction.Title-Withdraw"), p);
        BPMessages.send(p, "Chat-Withdraw");
        BPSets.addPlayerToWithdraw(p);
        p.closeInventory();
        BankPlus.INSTANCE.getPlayerRegistry().get(p).setOpenedBank(BankPlus.INSTANCE.getBankGuiRegistry().getBanks().get(identifier));
    }

    public static void customDeposit(Player p) {
        customDeposit(p, Values.CONFIG.getMainGuiName());
    }

    public static void customDeposit(Player p, String identifier) {
        if (!hasPermission(p, "bankplus.deposit")) return;

        if (Values.MESSAGES.isTitleCustomAmountEnabled())
            BPMethods.sendTitle(BankPlus.INSTANCE.getConfigManager().getConfig(ConfigManager.Type.MESSAGES).getString("Title-Custom-Transaction.Title-Deposit"), p);
        BPMessages.send(p, "Chat-Deposit");
        BPSets.addPlayerToDeposit(p);
        p.closeInventory();
        BankPlus.INSTANCE.getPlayerRegistry().get(p).setOpenedBank(BankPlus.INSTANCE.getBankGuiRegistry().getBanks().get(identifier));
    }

    public static void sendTitle(String title, Player p) {
        if (title == null) return;

        if (title.contains(",")) {
            String[] titles = title.split(",");
            String title1 = titles[0], title2 = titles[1];

            if (titles.length == 2) p.sendTitle(BPChat.color(title1), BPChat.color(title2));
            else {
                int[] values = {20, 20, 20};
                boolean error = false;

                for (int i = 2; i < titles.length; i++) {
                    try {
                        values[i - 2] = Integer.parseInt(titles[i]);
                    } catch (NumberFormatException e) {
                        error = true;
                    }
                }
                if (error)
                    BPLogger.warn("Invalid number in the title fades values! Please correct it as soon as possible! (Title: " + title + "&a)");

                try {
                    p.sendTitle(BPChat.color(title1), BPChat.color(title2), values[0], values[1], values[2]);
                } catch (NoSuchMethodError e) {
                    p.sendTitle(BPChat.color(title1), BPChat.color(title2));
                }
            }
        } else p.sendTitle(BPChat.color(title), "");
    }

    public static void playSound(String input, Player p) {

        String sound;
        switch (input) {
            case "WITHDRAW": {
                if (!Values.CONFIG.isWithdrawSoundEnabled()) return;
                sound = Values.CONFIG.getWithdrawSound();
                if (sound == null) {
                    BPLogger.warn("You are missing a string! &8(&ePath: General.Withdraw-Sound.Sound in config.yml&8)");
                    return;
                }
            }
            break;

            case "DEPOSIT": {
                if (!Values.CONFIG.isDepositSoundEnabled()) return;
                sound = Values.CONFIG.getDepositSound();
                if (sound == null) {
                    BPLogger.warn("You are missing a string! &8(&ePath: General.Deposit-Sound.Sound in config.yml&8)");
                    return;
                }
            }
            break;

            case "VIEW": {
                if (!Values.CONFIG.isViewSoundEnabled()) return;
                sound = Values.CONFIG.getViewSound();
                if (sound == null) {
                    BPLogger.warn("You are missing a string! &8(&ePath: General.View-Sound.Sound in config.yml&8)");
                    return;
                }
            }
            break;

            case "PERSONAL": {
                if (!Values.CONFIG.isPersonalSoundEnabled()) return;
                sound = Values.CONFIG.getPersonalSound();
                if (sound == null) {
                    BPLogger.warn("You are missing a string! &8(&ePath: General.Personal-Sound.Sound in config.yml&8)");
                    return;
                }
            }
            break;

            default:
                return;
        }

        if (!sound.contains(",")) {
            BPLogger.warn("The format of the sound \"" + sound + "\" is wrong! ");
            BPLogger.warn("Please correct it in the config!");
            return;
        }
        String[] pathSlitted = sound.split(",");
        String soundType;
        int volume, pitch;

        try {
            soundType = pathSlitted[0];
            volume = Integer.parseInt(pathSlitted[1]);
            pitch = Integer.parseInt(pathSlitted[2]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            BPLogger.warn("The format of the sound \"" + sound + "\" is wrong! ");
            BPLogger.warn("Please correct it in the config!");
            return;
        }

        try {
            p.playSound(p.getLocation(), Sound.valueOf(soundType), volume, pitch);
        } catch (IllegalArgumentException e) {
            BPLogger.warn("\"" + sound + "\" is an invalid sound type for your server version!");
            BPLogger.warn("Please change it in the config!");
        }
    }

    public static long secondsInMilliseconds(int seconds) {
        return seconds * 1000L;
    }

    public static long minutesInMilliseconds(int minutes) {
        return minutes * secondsInMilliseconds(60);
    }

    public static long hoursInMilliseconds(int hours) {
        return hours * minutesInMilliseconds(60);
    }

    public static long daysInMilliseconds(int days) {
        return days * hoursInMilliseconds(24);
    }

    public static boolean checkPreRequisites(BigDecimal money, BigDecimal amount, Player p) {
        if (amount.doubleValue() < 0) {
            BPMessages.send(p, "Cannot-Use-Negative-Number");
            return false;
        }
        if (money.doubleValue() <= 0) {
            BPMessages.send(p, "Insufficient-Money");
            return false;
        }
        return true;
    }

    public static List<String> placeValues(Player p, BigDecimal amount) {
        List<String> values = new ArrayList<>();
        values.add("%player%$" + p.getName());
        values.add("%player_name%$" + p.getName());

        values.add("%amount%$" + BPFormatter.formatCommas(amount));
        values.add("%amount_long%$" + amount);
        values.add("%amount_formatted%$" + BPFormatter.format(amount));
        values.add("%amount_formatted_long%$" + BPFormatter.formatLong(amount));
        return values;
    }

    public static List<String> placeValues(OfflinePlayer p, BigDecimal amount) {
        List<String> values = new ArrayList<>();
        values.add("%player%$" + p.getName());
        values.add("%player_name%$" + p.getName());

        values.add("%amount%$" + BPFormatter.formatCommas(amount));
        values.add("%amount_long%$" + amount);
        values.add("%amount_formatted%$" + BPFormatter.format(amount));
        values.add("%amount_formatted_long%$" + BPFormatter.formatLong(amount));
        return values;
    }

    public static List<String> placeValues(OfflinePlayer p, BigDecimal amount, BigDecimal taxes) {
        List<String> values = new ArrayList<>();
        values.add("%player%$" + p.getName());
        values.add("%player_name%$" + p.getName());

        values.add("%amount%$" + BPFormatter.formatCommas(amount));
        values.add("%amount_long%$" + amount);
        values.add("%amount_formatted%$" + BPFormatter.format(amount));
        values.add("%amount_formatted_long%$" + BPFormatter.formatLong(amount));

        values.add("%taxes%$" + BPFormatter.formatCommas(taxes));
        values.add("%taxes_long%$" + taxes);
        values.add("%taxes_formatted%$" + BPFormatter.format(taxes));
        values.add("%taxes_formatted_long%$" + BPFormatter.formatLong(taxes));
        return values;
    }

    public static boolean isBankFull(Player p, String bankName) {
        BigDecimal capacity = new BankReader(bankName).getCapacity(p);
        if (capacity.doubleValue() <= 0d) return false;

        if (new MultiEconomyManager(p).getBankBalance(bankName).doubleValue() >= capacity.doubleValue()) {
            BPMessages.send(p, "Cannot-Deposit-Anymore");
            return true;
        }
        return false;
    }

    public static boolean isBankFull(Player p) {
        BigDecimal capacity = new BankReader(Values.CONFIG.getMainGuiName()).getCapacity(p);
        if (capacity.doubleValue() <= 0d) return false;

        if (new SingleEconomyManager(p).getBankBalance().doubleValue() >= capacity.doubleValue()) {
            BPMessages.send(p, "Cannot-Deposit-Anymore");
            return true;
        }
        return false;
    }

    public static boolean hasFailed(Player p, EconomyResponse response) {
        if (!response.transactionSuccess()) {
            BPMessages.send(p, "Internal-Error");
            BPLogger.warn("Warning! (THIS IS NOT A BANKPLUS ERROR!) Vault has failed his transaction task. To" +
                    " avoid dupe bugs also bankplus has cancelled the transaction.");
            return true;
        }
        return false;
    }

    public static boolean isLegacyServer() {
        String v = BankPlus.INSTANCE.getServerVersion();
        return v.contains("1.7") || v.contains("1.8") || v.contains("1.9") ||
                v.contains("1.10") || v.contains("1.11") || v.contains("1.12");
    }
}