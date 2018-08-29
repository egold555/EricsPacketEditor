package org.golde.bukkit.sample;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.golde.bukkit.sample.EricsPacketEditor.PacketRecieveFromPlayerEvent;
import org.golde.bukkit.sample.EricsPacketEditor.PacketSentToPlayerEvent;

import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.PacketPlayInUpdateSign;
import net.minecraft.server.v1_7_R4.PacketPlayOutBed;


public class Main extends JavaPlugin implements Listener{
	
	EricsPacketEditor ericsPacketEditor = new EricsPacketEditor();
	
	@Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        ericsPacketEditor.onEnable(this);
        //ericsPacketEditor.enableDebugging(); //Only if you want too, this spams the console a lot
    }

	@Override
	public void onDisable() {
		ericsPacketEditor.onDisable();
	}
    

	@EventHandler
	public void onPacketSentToPlayer(PacketSentToPlayerEvent event) {
		Player player = event.getPlayer();
		Packet packet = event.getPacket();
		Bukkit.getLogger().info("Sent " + packet.getClass().getSimpleName() + " to " + player.getName());
		
		//Example - You can not sleep in beds (Glitchy)
		if(packet instanceof PacketPlayOutBed) {
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onPacketRecievedFromPlayer(PacketRecieveFromPlayerEvent event) {
		Player player = event.getPlayer();
		Packet packet = event.getPacket();
		
		Bukkit.getLogger().info("Recieved " + packet.getClass().getSimpleName() + " to " + player.getName());
		
		//Example -- You can not write on signs
		if(packet instanceof PacketPlayInUpdateSign) {
			event.setCancelled();
		}
	}

}