package org.golde.bukkit.sample;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.netty.channel.ChannelPipeline;
import net.minecraft.server.v1_8_R3.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class EricsPacketEditor18 extends EricsPacketEditorBase {

	private HashMap<Channel, UUID> playerMap = new HashMap<Channel, UUID>();
	private boolean debugging = false;
	
	@Override
	public void onEnable(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	public void onDisable() {
		for(Player player : Bukkit.getOnlinePlayers()) {
			removePlayer(player);
		}
	}
	
	@Override
	public void enableDebugging() {
		debugging = true;
	}
	
	@Override
	@EventHandler
    public void onjoin(PlayerJoinEvent event){
        injectPlayer(event.getPlayer());
    }

	@Override
    @EventHandler
    public void onleave(PlayerQuitEvent event){
        removePlayer(event.getPlayer());
    }
    
    protected void removePlayer(final Player player) {
    	try {
            Field channelField = ((CraftPlayer)player).getHandle().playerConnection.networkManager.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            Channel channel = (Channel) channelField.get(((CraftPlayer)player).getHandle().playerConnection.networkManager);
            channelField.setAccessible(false);
            channel.eventLoop().submit(() -> {
                channel.pipeline().remove("eric-injector-" + player.getUniqueId().toString());
                return null;
            });
            playerMap.remove(channel);
            
        }
        catch (NoSuchFieldException | SecurityException | IllegalAccessException e){
            e.printStackTrace();
        }
    	
    	
    	if(debugging) {Bukkit.getLogger().info("Removed player: " + player.getName());}
    }

    private void injectPlayer(Player player) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {

            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception {
            	
            	if(packet == null || !(packet instanceof Packet)){
            		super.channelRead(channelHandlerContext, packet);
            		return;
            	}
            	
            	final Player player = getPlayerFromChannel(channelHandlerContext);
            	String playerName = "null";
            	
            	if(player != null && player.getName() != null) {
            		playerName = player.getName();
            	}
            	if(debugging) { Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "PACKET READ: " + ChatColor.RED + packet.toString() + " @ " + playerName); }
            	
            	PacketRecieveFromPlayerEvent packetEvent = new PacketRecieveFromPlayerEvent(player, packet);
            	
            	Bukkit.getPluginManager().callEvent(packetEvent);
            	
            	if(packetEvent.isCancelled()) {
            		return;
            	}
            	
            	packet = packetEvent.getPacket();
            	
                super.channelRead(channelHandlerContext, packet);
            }

            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise channelPromise) throws Exception {
            	
            	if(packet == null || !(packet instanceof Packet)){
            		super.write(channelHandlerContext, packet, channelPromise);
            		return;
            	}
            	
            	final Player player = getPlayerFromChannel(channelHandlerContext);
            	String playerName = "null";
            	
            	if(player != null && player.getName() != null) {
            		playerName = player.getName();
            	}
            	
            	if(debugging) {Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET WRITE: " + ChatColor.GREEN + packet.toString() +  "@ " + playerName);}
            	
            	PacketSentToPlayerEvent eventPacket = new PacketSentToPlayerEvent(player, packet);
            	
            	Bukkit.getPluginManager().callEvent(eventPacket);
            	
            	if(eventPacket.isCancelled()) {
            		return;
            	}
            	
            	packet = eventPacket.getPacket();
            	
                super.write(channelHandlerContext, packet, channelPromise);
            }


        };

        try {
            Field channelField = ((CraftPlayer)player).getHandle().playerConnection.networkManager.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            Channel channel = (Channel) channelField.get(((CraftPlayer)player).getHandle().playerConnection.networkManager);
            channelField.setAccessible(false);
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addBefore("packet_handler", "eric-injector-" + player.getUniqueId().toString(), channelDuplexHandler);
            playerMap.put(channel, player.getUniqueId());
        }
        catch (NoSuchFieldException | SecurityException | IllegalAccessException e){
            e.printStackTrace();
        }
        
        if(debugging) {Bukkit.getLogger().info("Injected player: " + player.getName());}

    }
    
    private Player getPlayerFromChannel(ChannelHandlerContext channelHandlerContext) {
    	Channel channel = channelHandlerContext.channel();
    	if(channel == null) {
    		return null;
    	}
    	
    	if(playerMap.containsKey(channel)) {
    		return Bukkit.getPlayer(playerMap.get(channel));
    	}
    	
    	return null;
    }

}
