package top.speedcubing.serverlist;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ServerListSpigot extends JavaPlugin implements Listener {
    List<?> networkManagers;
    ServerPing.ServerPingPlayerSample playerlist;

    public void reload() {
        reloadConfig();
        FileConfiguration config = getConfig();
        ServerListConfig.Name = config.getBoolean("name.enable");
        ServerListConfig.name = ChatColor.translateAlternateColorCodes('&', config.getString("name.value"));
        ServerListConfig.Forge = config.getBoolean("forgeicon.enable");
        ServerListConfig.forge = config.getString("forgeicon.value");
        ServerListConfig.Protocol = config.getBoolean("protocol.enable");
        ServerListConfig.protocol = config.getInt("protocol.value");
        ServerListConfig.Motd = config.getBoolean("motd.enable");
        StringBuilder motd = new StringBuilder();
        for (String s : config.getStringList("motd.value")) {
            motd.append(ChatColor.translateAlternateColorCodes('&', s)).append("\n");
        }
        if (motd.length() != 0)
            motd = new StringBuilder(motd.substring(0, motd.length() - 1));
        ServerListConfig.motd = motd.toString();
        ServerListConfig.Players = config.getBoolean("players.enable");

        List<String> players = config.getStringList("players.value");
        int size = players.size();
        playerlist = new ServerPing.ServerPingPlayerSample(1, 1);
        playerlist.a(new GameProfile[size]);
        for (int i = 0; i < size; i++) {
            playerlist.c()[i] = new GameProfile(UUID.randomUUID(), ChatColor.translateAlternateColorCodes('&', players.get(i)));
        }
    }

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginCommand("serverlist").setExecutor(((commandSender, command, s, strings) -> {
            reload();
            return true;
        }));
        try {
            ServerConnection conn = MinecraftServer.getServer().aq();
            for (Method method : conn.getClass().getDeclaredMethods()) {
                method.setAccessible(true);
                if (method.getReturnType() == List.class) {
                    networkManagers = Collections.synchronizedList((List<?>) method.invoke(null, conn));
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        saveDefaultConfig();

        reload();
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent e) throws Exception {
        Field field = NetworkManager.class.getDeclaredField("channel");
        field.setAccessible(true);
        for (Object manager : networkManagers.toArray()) {
            Channel channel = (Channel) field.get(manager);
            if (channel.pipeline().get("speedcubingServerList") == null) {
                channel.pipeline().addBefore("packet_handler", "speedcubingServerList", new ChannelDuplexHandler() {
                    @Override
                    public void write(ChannelHandlerContext channel, Object byteBuf, ChannelPromise promise) throws Exception {
                        if (byteBuf instanceof PacketStatusOutServerInfo) {
                            PacketStatusOutServerInfo packet = (PacketStatusOutServerInfo) byteBuf;
                            Field serverPing = packet.getClass().getDeclaredField("b");
                            serverPing.setAccessible(true);
                            ServerPing ping = (ServerPing) serverPing.get(packet);
                            ping.setServerInfo(new ServerPing.ServerData(ServerListConfig.Name ? ServerListConfig.name : ping.c().a(), ServerListConfig.Protocol ? ServerListConfig.protocol : ping.c().b()));
                            if (ServerListConfig.Forge)
                                ping.setFavicon(ServerListConfig.forge);
                            if (ServerListConfig.Motd)
                                ping.setMOTD(new ChatComponentText(ServerListConfig.motd));
                            if (ServerListConfig.Players)
                                ping.setPlayerSample(playerlist);
                        }
                        super.write(channel, byteBuf, promise);
                    }
                });
            }
        }
    }
}
