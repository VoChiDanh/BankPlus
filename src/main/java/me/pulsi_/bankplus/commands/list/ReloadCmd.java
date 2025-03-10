package me.pulsi_.bankplus.commands.list;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.commands.BPCommand;
import me.pulsi_.bankplus.utils.BPMessages;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCmd extends BPCommand {

    public ReloadCmd(String... aliases) {
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
    public boolean onCommand(CommandSender s, String args[]) {
        if (confirm(s)) return false;

        long time = System.currentTimeMillis();
        BPMessages.send(s, "Reload-Started");

        boolean reloaded = BankPlus.INSTANCE.getDataManager().reloadPlugin();
        if (reloaded) BPMessages.send(s, "Reload-Ended", "%time%$" + (System.currentTimeMillis() - time));
        else BPMessages.send(s, "Reload-Failed");
        return true;
    }

    @Override
    public List<String> tabCompletion(CommandSender s, String args[]) {
        return null;
    }
}