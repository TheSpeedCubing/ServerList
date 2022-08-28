package top.speedcubing.serverlist.bungee;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class reload extends Command {
    public reload() {
        super("serverlist", "serverlist.reload");
    }

    public void execute(CommandSender commandSender, String[] strings) {
        ServerListBungee.bungee.reload();
        commandSender.sendMessage("ServerList reloaded.");
    }
}