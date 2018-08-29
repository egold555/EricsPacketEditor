package org.golde.bukkit.sample;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelPromise;

public class EricsPacketEditor implements Listener {

	private HashMap<Channel, UUID> playerMap = new HashMap<Channel, UUID>();
	private boolean debugging = false;
	
	public void onEnable(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public void onDisable() {
		for(Player player : Bukkit.getOnlinePlayers()) {
			removePlayer(player);
		}
	}
	
	public void enableDebugging() {
		debugging = true;
	}
	
	@EventHandler
    public void onjoin(PlayerJoinEvent event){
        injectPlayer(event.getPlayer());
    }

    @EventHandler
    public void onleave(PlayerQuitEvent event){
        removePlayer(event.getPlayer());
    }
    
    private void removePlayer(final Player player) {
    	try {
            Field channelField = ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)player).getHandle().playerConnection.networkManager.getClass().getDeclaredField("m");
            channelField.setAccessible(true);
            Channel channel = (Channel) channelField.get(((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)player).getHandle().playerConnection.networkManager);
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
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object objPacket) throws Exception {
            	
            	if(!(objPacket instanceof net.minecraft.server.v1_7_R4.Packet)){
            		super.channelRead(channelHandlerContext, objPacket);
            		return;
            	}
            	
            	net.minecraft.server.v1_7_R4.Packet packet = (net.minecraft.server.v1_7_R4.Packet)objPacket;
            	
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
            public void write(ChannelHandlerContext channelHandlerContext, Object objPacket, ChannelPromise channelPromise) throws Exception {
            	
            	if(!(objPacket instanceof net.minecraft.server.v1_7_R4.Packet)){
            		super.write(channelHandlerContext, objPacket, channelPromise);
            		return;
            	}
            	
            	net.minecraft.server.v1_7_R4.Packet packet = (net.minecraft.server.v1_7_R4.Packet)objPacket;
            	
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
            Field channelField = ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)player).getHandle().playerConnection.networkManager.getClass().getDeclaredField("m");
            channelField.setAccessible(true);
            Channel channel = (Channel) channelField.get(((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)player).getHandle().playerConnection.networkManager);
            channelField.setAccessible(false);
            net.minecraft.util.io.netty.channel.ChannelPipeline pipeline = channel.pipeline();
            pipeline.addBefore("packet_handler", "eric-injector-" + player.getUniqueId().toString(), channelDuplexHandler);
            playerMap.put(channel, player.getUniqueId());
        }
        catch (NoSuchFieldException | SecurityException | IllegalAccessException e){
            e.printStackTrace();
        }
        
        Bukkit.getLogger().info("Injected player: " + player.getName());

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
	
	
	
	////////////////////[Start Events]//////////////////////////////////////
	
	private static class EventBase extends Event implements Cancellable {

		private static final HandlerList handlers = new HandlerList();

		private net.minecraft.server.v1_7_R4.Packet packet = null;
		
		private final Player player;
		
		public EventBase(Player player, net.minecraft.server.v1_7_R4.Packet packet) {
			this.player = player;
			this.packet = packet;
		}
		
		public void setPacket(net.minecraft.server.v1_7_R4.Packet packet) {
			this.packet = packet;
		}
		
		public net.minecraft.server.v1_7_R4.Packet getPacket() {
			return packet;
		}
		
		public Player getPlayer() {
			return player;
		}
		
		
		@Override
		public boolean isCancelled() {
			return packet == null;
		}

		@Override
		@Deprecated
		/***
		 * Use setCancelled(); You can not unCancelle this event
		 */
		public void setCancelled(boolean arg0) {
			setCancelled();
		}
		
		public void setCancelled() {
			packet = null;
		}

		@Override
		public HandlerList getHandlers() {
			return handlers;
		}

		public static HandlerList getHandlerList() {
			return handlers;
		}

	}

	public static class PacketSentToPlayerEvent extends EventBase {

		public PacketSentToPlayerEvent(Player player, net.minecraft.server.v1_7_R4.Packet packet) {
			super(player, packet);
		}

	}
	
	public static class PacketRecieveFromPlayerEvent extends EventBase {

		public PacketRecieveFromPlayerEvent(Player player, net.minecraft.server.v1_7_R4.Packet packet) {
			super(player, packet);
		}

	}
	
	//////////////////////[End Events]///////////////////////////////////////////////////

}
