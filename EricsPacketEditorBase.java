package org.golde.bukkit.sample;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class EricsPacketEditorBase implements Listener {

	public abstract void onEnable(JavaPlugin plugin);
	public abstract void onDisable();
	public abstract void enableDebugging();
	public abstract void onjoin(PlayerJoinEvent event);
	public abstract void onleave(PlayerQuitEvent event);

	
	private static class EventBase extends Event implements Cancellable {

		private static final HandlerList handlers = new HandlerList();

		private Object packet = null;
		
		private final Player player;
		
		public EventBase(Player player, Object packet) {
			this.player = player;
			this.packet = packet;
		}
		
		public void setPacket(Object packet) {
			this.packet = packet;
		}
		
		public Object getPacket() {
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

		public PacketSentToPlayerEvent(Player player, Object packet) {
			super(player, packet);
		}

	}
	
	public static class PacketRecieveFromPlayerEvent extends EventBase {

		public PacketRecieveFromPlayerEvent(Player player, Object packet) {
			super(player, packet);
		}

	}
	
}
