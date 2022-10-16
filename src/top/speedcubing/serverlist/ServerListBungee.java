package top.speedcubing.serverlist;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import top.speedcubing.serverlist.ServerListConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;


public class ServerListBungee extends Plugin implements Listener {
    ServerPing.PlayerInfo[] playerlist;

    public void reload() {
        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
            ServerListConfig.Name = config.getBoolean("name.enable");
            ServerListConfig.name = ChatColor.translateAlternateColorCodes('&', config.getString("name.value"));
            ServerListConfig.Forge = config.getBoolean("forgeicon.enable");
            ServerListConfig.forge = config.getString("forgeicon.value");
            ServerListConfig.Protocol = config.getBoolean("protocol.enable");
            ServerListConfig.protocol = config.getInt("protocol.value");
            ServerListConfig.Motd = config.getBoolean("motd.enable");
            ServerListConfig.motd = ChatColor.translateAlternateColorCodes('&', config.getString("motd.line1") + "\n" + config.getString("motd.line2"));
            ServerListConfig.Players = config.getBoolean("players.enable");

            List<String> players = config.getStringList("players.value");
            int size = players.size();
            playerlist = new ServerPing.PlayerInfo[size];
            for (int i = 0; i < size; i++) {
                playerlist[i] = new ServerPing.PlayerInfo(ChatColor.translateAlternateColorCodes('&', players.get(i)), UUID.randomUUID());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onEnable() {
        BungeeCord.getInstance().getPluginManager().registerListener(this, this);
        BungeeCord.getInstance().getPluginManager().registerCommand(this, new Command("serverlist", "serverlist.reload") {
            @Override
            public void execute(CommandSender commandSender, String[] strings) {
                reload();
                commandSender.sendMessage("ServerList reloaded.");
            }
        });

        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        reload();
    }

    @EventHandler
    public void onProxyPing(ProxyPingEvent e) {
        ServerPing server = e.getResponse();
        if (ServerListConfig.Name)
            server.getVersion().setName(ServerListConfig.name);
        if (ServerListConfig.Forge)
            server.getModinfo().setType(ServerListConfig.forge);
        if (ServerListConfig.Protocol)
            server.getVersion().setProtocol(ServerListConfig.protocol);
        if (ServerListConfig.Motd)
            server.setDescription(ServerListConfig.motd);
        if (ServerListConfig.Players)
            server.getPlayers().setSample(playerlist);
    }
}
