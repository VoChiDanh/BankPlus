package me.pulsi_.bankplus.commands.list;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.commands.BPCommand;
import me.pulsi_.bankplus.utils.BPLogger;
import me.pulsi_.bankplus.utils.BPMessages;
import me.pulsi_.bankplus.values.Values;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SaveAllDataCmd extends BPCommand {

    public SaveAllDataCmd(String... aliases) {
        super(aliases);
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean skipUsageWarn() {
        return true;
    }

    @Override
    public boolean onCommand(CommandSender s, String[] args) {
        if (confirm(s)) return false;

        BankPlus.INSTANCE.getPlayerRegistry().forceSave(true);
        if (Values.CONFIG.isSaveBalancesBroadcast()) BPLogger.info("All player data have been saved!");
        BPMessages.send(s, "%prefix% &aSuccessfully saved all player data!", true);
        return true;
    }

    @Override
    public List<String> tabCompletion(CommandSender s, String[] args) {
        return null;
    }
}