package me.pulsi_.bankplus.bankSystem;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.account.BPPlayerFiles;
import me.pulsi_.bankplus.economy.BPEconomy;
import me.pulsi_.bankplus.utils.BPLogger;
import me.pulsi_.bankplus.utils.BPMessages;
import me.pulsi_.bankplus.utils.BPUtils;
import me.pulsi_.bankplus.values.Values;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is used to receive information from the selected bank and many more usefully methods to manage the bank and the player.
 */
public class BankReader {

    private final Bank bank;
    private final BPEconomy economy;

    public BankReader() {
        bank = null;
        economy = BankPlus.getBPEconomy();
    }

    public BankReader(String bankName) {
        bank = BankPlus.INSTANCE.getBankGuiRegistry().getBanks().get(bankName);
        economy = BankPlus.getBPEconomy();
    }

    public Bank getBank() {
        return bank;
    }

    public boolean exist() {
        return bank != null;
    }

    /**
     * To get the upgrades use {@link Bank}#getUpgrades();
     */
    public boolean hasUpgrades() {
        return bank.getUpgrades() != null;
    }

    /**
     * To get the upgrades use {@link Bank}#getPermission();
     */
    public boolean hasPermission() {
        return bank.getPermission() != null;
    }

    /**
     * Get the current bank capacity based on the bank level of this player.
     *
     * @param p The player.
     * @return The capacity amount.
     */
    public BigDecimal getCapacity(OfflinePlayer p) {
        return getCapacity(getCurrentLevel(p));
    }

    /**
     * Get the bank capacity of that specified level.
     *
     * @param level The bank level.
     * @return The capacity amount.
     */
    public BigDecimal getCapacity(int level) {
        if (!hasUpgrades()) return Values.CONFIG.getMaxBankCapacity();

        String capacity = bank.getUpgrades().getString(level + ".Capacity");
        return new BigDecimal(capacity == null ? Values.CONFIG.getMaxBankCapacity().toString() : capacity);
    }

    /**
     * Get the interest rate of the player's bank level.
     *
     * @param p The player.
     * @return The interest amount.
     */
    public BigDecimal getInterest(OfflinePlayer p) {
        return getInterest(p, getCurrentLevel(p));
    }

    /**
     * Get the interest rate of that bank level.
     *
     * @param level The bank level.
     * @return The interest amount.
     */
    public BigDecimal getInterest(OfflinePlayer p, int level) {
        if (Values.CONFIG.enableInterestLimiter()) return getLimiterInterest(p, level, Values.CONFIG.getInterestMoneyGiven());
        if (!hasUpgrades()) return Values.CONFIG.getInterestMoneyGiven();

        String interest = bank.getUpgrades().getString(level + ".Interest");

        if (BPUtils.isInvalidNumber(interest)) {
            if (interest != null)
                BPLogger.warn("Invalid interest amount in the " + level + "* upgrades section, file: " + bank.getIdentifier() + ".yml");
            return Values.CONFIG.getInterestMoneyGiven();
        }

        return new BigDecimal(interest.replace("%", ""));
    }

    /**
     * Get the offline interest rate of the player's bank level.
     *
     * @param p The player
     * @return The offline interest amount.
     */
    public BigDecimal getOfflineInterest(Player p) {
        return getOfflineInterest(p, getCurrentLevel(p));
    }

    /**
     * Get the offline interest rate of the player's bank level.
     *
     * @param p The player
     * @return The offline interest amount.
     */
    public BigDecimal getOfflineInterest(OfflinePlayer p) {
        return getOfflineInterest(p, getCurrentLevel(p));
    }

    /**
     * Get the offline interest rate of that bank level.
     *
     * @param level The bank level.
     * @return The offline interest amount.
     */
    public BigDecimal getOfflineInterest(OfflinePlayer p, int level) {
        if (Values.CONFIG.enableInterestLimiter()) return getLimiterInterest(p, level, Values.CONFIG.getOfflineInterestMoneyGiven());
        if (!hasUpgrades()) return Values.CONFIG.getOfflineInterestMoneyGiven();

        String interest = bank.getUpgrades().getString(level + ".Offline-Interest");

        if (BPUtils.isInvalidNumber(interest)) {
            if (interest != null)
                BPLogger.warn("Invalid offline interest amount in the " + level + "* upgrades section, file: " + bank.getIdentifier() + ".yml");
            return Values.CONFIG.getOfflineInterestMoneyGiven();
        }

        return new BigDecimal(interest.replace("%", ""));
    }

    /**
     * Get the list of interest limiter
     * @param level The bank level.
     */
    public List<String> getInterestLimiter(int level) {
        if (!hasUpgrades()) return Values.CONFIG.getInterestLimiter();

        List<String> limiter = bank.getUpgrades().getStringList(level + ".Interest-Limiter");
        return limiter.isEmpty() ? Values.CONFIG.getInterestLimiter() : limiter;
    }

    /**
     * Get the cost of this bank level.
     *
     * @param level The level to check.
     * @return The cost, zero if no cost is specified.
     */
    public BigDecimal getLevelCost(int level) {
        if (!hasUpgrades()) return new BigDecimal(0);

        String cost = bank.getUpgrades().getString(level + ".Cost");
        return new BigDecimal(cost == null ? "0" : cost);
    }

    /**
     * Get the items required to level up the bank to this level.
     *
     * @param level The level to check.
     * @return The itemstack representing the item needed with its amount set, null if not specified.
     */
    public List<ItemStack> getLevelRequiredItems(int level) {
        List<ItemStack> items = new ArrayList<>();
        if (!hasUpgrades()) return items;

        String requiredItemsString = bank.getUpgrades().getString(level + ".Required-Items");
        if (requiredItemsString == null || requiredItemsString.isEmpty()) return items;

        List<String> configItems = new ArrayList<>();
        if (!requiredItemsString.contains(",")) configItems.add(requiredItemsString);
        else configItems.addAll(Arrays.asList(requiredItemsString.split(",")));

        for (String splitItem : configItems) {
            if (!splitItem.contains("-")) {
                try {
                    items.add(new ItemStack(Material.valueOf(splitItem)));
                } catch (IllegalArgumentException e) {
                    BPLogger.warn("The bank \"" + bank.getIdentifier() + "\" contains an invalid item in the \"Required-Items\" path at level *" + level + ".");
                }
            } else {
                String[] split = splitItem.split("-");
                ItemStack item;
                try {
                    item = new ItemStack(Material.valueOf(split[0]));
                } catch (IllegalArgumentException e) {
                    BPLogger.warn("The bank \"" + bank.getIdentifier() + "\" contains an invalid item in the \"Required-Items\" path at level *" + level + ".");
                    continue;
                }
                int amount = 1;
                try {
                    amount = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    BPLogger.warn("The bank \"" + bank.getIdentifier() + "\" contains an invalid number in the \"Required-Items\" path at level *" + level + ".");
                }

                item.setAmount(amount);
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Check if the plugin should take the required items from the player inventory.
     *
     * @param level The level to check.
     */
    public boolean removeRequiredItems(int level) {
        return hasUpgrades() && bank.getUpgrades().getBoolean(level + ".Remove-Required-Items");
    }

    /**
     * Get a list of all levels that this bank have, if it has none, it will return a list of 1.
     *
     * @return A string list with all levels.
     */
    public List<String> getLevels() {
        List<String> levels = new ArrayList<>();
        if (!hasUpgrades()) {
            levels.add("1");
            return levels;
        }

        levels.addAll(bank.getUpgrades().getKeys(false));
        return levels;
    }

    /**
     * Get the current bank level for that player.
     *
     * @param p The player.
     * @return The current level.
     */
    public int getCurrentLevel(OfflinePlayer p) {
        FileConfiguration config = new BPPlayerFiles(p).getPlayerConfig();
        return Math.max(config.getInt("banks." + bank.getIdentifier() + ".level"), 1);
    }

    /**
     * Check if the bank of that player has a next level.
     *
     * @param p The player.
     * @return true if it has a next level, false otherwise.
     */
    public boolean hasNextLevel(Player p) {
        return hasNextLevel(getCurrentLevel(p));
    }

    /**
     * Check if the selected bank has another level next the one specified.
     *
     * @param currentLevel The current level of the bank.
     * @return true if it has a next level, false otherwise.
     */
    public boolean hasNextLevel(int currentLevel) {
        return hasUpgrades() && bank.getUpgrades().getConfigurationSection(String.valueOf(currentLevel + 1)) != null;
    }

    /**
     * This method does not require to specify a bank in the constructor.
     *
     * @param p The player.
     * @return A list with all names of available banks for this player. To get a list of ALL banks use the BanksGuiRegistry class through the main class.
     */
    public List<String> getAvailableBanks(OfflinePlayer p) {
        List<String> availableBanks = new ArrayList<>();
        if (p == null) return availableBanks;

        if (p.isOnline()) {
            for (String bankName : BankPlus.INSTANCE.getBankGuiRegistry().getBanks().keySet())
                if (new BankReader(bankName).isAvailable(p.getPlayer()))
                    availableBanks.add(bankName);
        } else {
            for (String bankName : BankPlus.INSTANCE.getBankGuiRegistry().getBanks().keySet())
                if (new BankReader(bankName).isAvailable(p))
                    availableBanks.add(bankName);
        }
        return availableBanks;
    }

    /**
     * Check if this bank is available for the player.
     *
     * @param p The player
     * @return true if available, false otherwise.
     */
    public boolean isAvailable(Player p) {
        return !hasPermission() || bank.getPermission().isEmpty() || p.hasPermission(bank.getPermission());
    }

    /**
     * Check if this bank is available for the player.
     *
     * @param p The player
     * @return true if available, false otherwise.
     */
    public boolean isAvailable(OfflinePlayer p) {
        if (!hasPermission()) return true;
        else {
            String wName = Bukkit.getWorlds().get(0).getName();
            return BankPlus.INSTANCE.getPermissions().playerHas(wName, p, bank.getPermission());
        }
    }

    public void setLevel(OfflinePlayer p, int level) {
        BPPlayerFiles files = new BPPlayerFiles(p);
        File file = files.getPlayerFile();
        FileConfiguration config = files.getPlayerConfig(file);
        config.set("banks." + bank.getIdentifier() + ".level", level);
        files.savePlayerFile(config, file, true);
    }

    /**
     * Method used to upgrade the selected bank for the specified player.
     *
     * @param p The player.
     */
    public void upgradeBank(Player p) {
        if (!hasNextLevel(p)) {
            BPMessages.send(p, "Bank-Max-Level");
            return;
        }

        int nextLevel = getCurrentLevel(p) + 1;

        List<ItemStack> requiredItems = getLevelRequiredItems(nextLevel);
        if (!requiredItems.isEmpty()) {
            boolean hasItems = false;

            for (ItemStack item : requiredItems) {
                int amount = item.getAmount();
                int playerAmount = 0;

                boolean hasItem = false;
                for (ItemStack content : p.getInventory().getContents()) {
                    if (content == null || content.getType() != item.getType()) continue;
                    playerAmount += content.getAmount();

                    if (playerAmount < amount) continue;
                    hasItem = true;
                    break;
                }
                if (!hasItem) {
                    hasItems = false;
                    break;
                }
                hasItems = true;
            }
            if (!hasItems) {
                BPMessages.send(p, "Insufficient-Items", "%items%$" + BPUtils.getRequiredItems(requiredItems));
                return;
            }
        }

        BigDecimal cost = getLevelCost(nextLevel);
        if (Values.CONFIG.useBankBalanceToUpgrade()) {

            BigDecimal balance = economy.getBankBalance(p, bank.getIdentifier());
            if (balance.doubleValue() < cost.doubleValue()) {
                BPMessages.send(p, "Insufficient-Money");
                return;
            }

            if (removeRequiredItems(nextLevel) && !requiredItems.isEmpty()) for (ItemStack item : requiredItems) p.getInventory().removeItem(item);
            economy.removeBankBalance(p, cost, bank.getIdentifier());
        } else {

            Economy economy = BankPlus.INSTANCE.getVaultEconomy();
            double balance = economy.getBalance(p);

            if (balance < cost.doubleValue()) {
                BPMessages.send(p, "Insufficient-Money");
                return;
            }

            if (removeRequiredItems(nextLevel) && !requiredItems.isEmpty()) for (ItemStack item : requiredItems) p.getInventory().removeItem(item);
            economy.withdrawPlayer(p, cost.doubleValue());
        }

        setLevel(p, nextLevel);
        BPMessages.send(p, "Bank-Upgraded");

        if (!hasNextLevel(nextLevel)) {
            for (String line : Values.MULTIPLE_BANKS.getAutoBanksUnlocker()) {
                if (!line.contains(":")) continue;

                String[] parts = line.split(":");
                String bankName = parts[0];
                if (!bankName.equals(bank.getIdentifier())) continue;

                for (int i = 1; i < parts.length; i++) {
                    String cmd = parts[i].replace("%player%", p.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        }
    }

    private BigDecimal getLimiterInterest(OfflinePlayer p, int level, BigDecimal fallBack) {
        for (String limiter : getInterestLimiter(level)) {
            if (!limiter.contains(":")) continue;

            String[] split1 = limiter.split(":");
            if (BPUtils.isInvalidNumber(split1[1])) continue;

            String[] split2 = split1[0].split("-");
            if (BPUtils.isInvalidNumber(split2[0]) || BPUtils.isInvalidNumber(split2[1])) continue;

            String interest = split1[1].replace("%", ""), from = split2[0], to = split2[1];
            BigDecimal interestRate = new BigDecimal(interest), fromNumber = new BigDecimal(from), toNumber = new BigDecimal(to);

            if (fromNumber.doubleValue() > toNumber.doubleValue()) {
                BigDecimal temp = toNumber;
                fromNumber = toNumber;
                toNumber = temp;
            }

            BigDecimal balance = economy.getBankBalance(p, bank.getIdentifier());
            if (fromNumber.doubleValue() <= balance.doubleValue() && toNumber.doubleValue() >= balance.doubleValue())
                return interestRate;
        }
        return fallBack;
    }
}