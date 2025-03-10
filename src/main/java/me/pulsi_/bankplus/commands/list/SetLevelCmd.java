package me.pulsi_.bankplus.commands.list;

import me.pulsi_.bankplus.bankSystem.BankReader;
import me.pulsi_.bankplus.commands.BPCommand;
import me.pulsi_.bankplus.utils.BPArgs;
import me.pulsi_.bankplus.utils.BPMessages;
import me.pulsi_.bankplus.utils.BPUtils;
import me.pulsi_.bankplus.values.Values;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SetLevelCmd extends BPCommand {

    public SetLevelCmd(String... aliases) {
        super(aliases);
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean skipUsageWarn() {
        return false;
    }

    @Override
    public boolean onCommand(CommandSender s, String args[]) {
        OfflinePlayer p = Bukkit.getPlayerExact(args[1]);
        if (!p.hasPlayedBefore()) {
            BPMessages.send(s, "Invalid-Player");
            return false;
        }

        if (args.length == 2) {
            BPMessages.send(s, "Specify-Number");
            return false;
        }

        String level = args[2];
        if (BPUtils.isInvalidNumber(level, s)) return false;

        String bankName = Values.CONFIG.getMainGuiName();
        if (args.length > 3) bankName = args[3];

        BankReader reader = new BankReader(bankName);
        if (!reader.exist()) {
            BPMessages.send(s, "Invalid-Bank");
            return false;
        }
        if (!reader.getLevels().contains(level)) {
            BPMessages.send(s, "Invalid-Bank-Level");
            return false;
        }
        if (confirm(s)) return false;

        new BankReader(bankName).setLevel(p, Integer.parseInt(level));
        BPMessages.send(s, "Set-Level-Message", "%player%$" + p.getName(), "%level%$" + level);
        return true;
    }

    @Override
    public List<String> tabCompletion(CommandSender s, String[] args) {
        if (args.length == 3)
            return BPArgs.getArgs(args, "1", "2", "3");

        if (args.length == 4)
            return BPArgs.getBanks(args);
        return null;
    }
}