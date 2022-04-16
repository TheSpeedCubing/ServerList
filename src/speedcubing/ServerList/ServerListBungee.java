package speedcubing.ServerList;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class ServerListBungee extends Plugin implements Listener {
    public static ServerListBungee bungee;

    String name;
    String forge;
    String motd;
    ServerPing.PlayerInfo[] players;
    int protocol;
    boolean Name;
    boolean Forge;
    boolean Protocol;
    boolean Motd;
    boolean Players;

    public void reload() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
            Name = config.getBoolean("name.enable");
            name = ChatColor.translateAlternateColorCodes('&', config.getString("name.value"));
            Forge = config.getBoolean("forgeicon.enable");
            forge = config.getString("forgeicon.value");
            Protocol = config.getBoolean("protocol.enable");
            protocol = config.getInt("protocol.value");
            Motd = config.getBoolean("motd.enable");
            motd = ChatColor.translateAlternateColorCodes('&', config.getString("motd.value"));
            Players = config.getBoolean("players.enable");
            List<String> p = config.getStringList("players.value");
            players = new ServerPing.PlayerInfo[p.size()];
            for (int i = 0; i < p.size(); i++) {
                players[i] = new ServerPing.PlayerInfo(ChatColor.translateAlternateColorCodes('&', p.get(i)), UUID.randomUUID());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Configuration config;

    public void onEnable() {
        bungee = this;
        BungeeCord.getInstance().getPluginManager().registerListener(this, this);
        BungeeCord.getInstance().getPluginManager().registerCommand(this, new reload());
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
        if (Name)
            server.getVersion().setName(name);
        if (Forge)
            server.getModinfo().setType(forge);
        if (Protocol)
            server.getVersion().setProtocol(protocol);
        if (Motd)
            server.setDescription(motd);
        if (Players)
            server.getPlayers().setSample(players);
    }
}
