package net.miblue.credits;

import java.io.File;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.*;

public final class MiBlueCredits extends JavaPlugin implements Listener {
	public static HashMap<Player, Integer> kills = new HashMap<Player, Integer>();
	private static Economy economy;
	public static Connection conn = null;
	@Override
	public void onEnable(){
		getLogger().info("MiBlueCredits by milesmcc is being enabled...");
		Server server = getServer();
		getServer().getPluginManager().registerEvents(this, this);
		if(new File(getDataFolder().toString()).exists()){
			// Config exists, why generate a new one?
		}else{
			getLogger().info("Generating MiBlueCredits config!");
			getConfig().options().copyDefaults(true);
			saveDefaultConfig();
			getLogger().info("Generation complete!");
		}
		setupEconomy();
		getLogger().info("Attempting to connect to MySQL...");
		String user = getConfig().getString("database.user");
		String pass = getConfig().getString("database.pass");
		String url = getConfig().getString("database.url");
		conn = null;
		try {
			conn = DriverManager.getConnection(url, user, pass);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			if(conn.isClosed()){
				getLogger().severe("Unable to connect to MySQL. Disabling...");
				this.setEnabled(false);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getLogger().warning("MiBlueCredits expects the table 'credits' to already created and have the columns 'name' and 'credits' existing!");
		getLogger().info("Enabling complete!");
		}
	
	private boolean setupEconomy() {
        @SuppressWarnings("unchecked")
		final RegisteredServiceProvider<Economy> economyProvider = (RegisteredServiceProvider<Economy>)this.getServer().getServicesManager().getRegistration((Class)Economy.class);
        if (economyProvider != null) {
            MiBlueCredits.economy = (Economy)economyProvider.getProvider();
        }
        return MiBlueCredits.economy != null;
    }
	@Override
	public void onDisable(){
		getLogger().info("Disconnecting from MySQL...");
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getLogger().info("MiBlueCredits has been disabled.");
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length == 0){
			if(getConfig().getBoolean("redeem.enabled")){
				sender.sendMessage(ChatColor.GRAY +""+ ChatColor.ITALIC + "Redeem your credits for money with " + ChatColor.GOLD + ChatColor.ITALIC + "/credits redeem <amount of credits> money" + ChatColor.GRAY + ChatColor.ITALIC + ". Exchange rate: 1 credit is worth " + ChatColor.GOLD + getConfig().getInt("redeem.worth") + ChatColor.BLUE + " in-game currency.");
			}
			if(getConfig().getBoolean("killstreak.enabled")){
				sender.sendMessage(ChatColor.GRAY +""+ ChatColor.ITALIC + "Every " + getConfig().getInt("killstreak.multiple") + " kills, you can earn " + ChatColor.GOLD + ChatColor.ITALIC + getConfig().getInt("killstreak.worth") + ChatColor.GRAY + ChatColor.ITALIC + " credits.");
			}
			if(getConfig().getBoolean("hungergames.enabled")){
				sender.sendMessage(ChatColor.GRAY +""+ ChatColor.ITALIC + "If you win the Hunger Games, you will get " + ChatColor.GOLD + ChatColor.ITALIC + getConfig().getInt("hungergames.per-win") + ChatColor.GRAY + ChatColor.ITALIC + " credits.");
			}
			sender.sendMessage(ChatColor.BLUE + "Ⓜ" + ChatColor.STRIKETHROUGH + "-----------------------------------------");
			sender.sendMessage(ChatColor.BLUE + "Ⓜ"+ ChatColor.ITALIC + "Your credits: " + ChatColor.GOLD + ChatColor.ITALIC + getCredits(sender.getName()));
			sender.sendMessage(ChatColor.BLUE + "Ⓜ" + ChatColor.STRIKETHROUGH + "-----------------------------------------");
		}else if(args.length == 3){
			if(args[0].equalsIgnoreCase("redeem")){
				if(getConfig().getBoolean("redeem.enabled")){
					if(args[2].equalsIgnoreCase("money")){
						int toget = 0;
						try {
						    toget = Integer.parseInt(args[1]) * getConfig().getInt("redeem.worth");
						    if(getCredits(sender.getName()) >= Integer.parseInt(args[1])){
									economy.depositPlayer(sender.getName(), toget);
									setCredits(sender.getName(), getCredits(sender.getName()) - Integer.parseInt(args[1]));
									sender.sendMessage(ChatColor.GREEN + "Successfully traded in " + Integer.parseInt(args[1]) + " credits in order to get " + toget + " in-game currency.");
							}else{
								sender.sendMessage(ChatColor.RED + "Not enough credits!");
							}
						} catch (NumberFormatException e) {
						      sender.sendMessage(ChatColor.RED + "Amount of credits not a number!");
						}
					}
				}
			}else if(args[0].equalsIgnoreCase("set")){
				if(sender.hasPermission("mibluecredits.admin") || sender.isOp()){
					int amount = 0;
					String to = args[1];
					try {
					    amount = Integer.parseInt(args[2]);
					    if(amount > -1){
					    	setCredits(to, amount);
						    sender.sendMessage(ChatColor.GREEN + "Success.");
					    }else{
					    	sender.sendMessage(ChatColor.RED + "You can't have negative credits!");
					    }
					    
					} catch (NumberFormatException e) {
					      sender.sendMessage(ChatColor.RED + "Amount of credits not a number!");
					}
				}else{
					sender.sendMessage(ChatColor.RED + "No permission.");
				}
			}else if(args[0].equalsIgnoreCase("add")){
				if(sender.hasPermission("mibluecredits.admin") || sender.isOp()){
					int amount = 0;
					try {
					    amount = Integer.parseInt(args[2]);
					    setCredits(args[1], amount + getCredits(args[1]));
					    sender.sendMessage(ChatColor.GREEN + "Success.");
					} catch (NumberFormatException e) {
					      sender.sendMessage(ChatColor.RED + "Amount of credits not a number!");
					}
				}else{
					sender.sendMessage(ChatColor.RED + "No permission.");
				}
			}else if(args[0].equalsIgnoreCase("remove")){
				if(sender.hasPermission("mibluecredits.admin") || sender.isOp()){
					int amount = 0;
					try {
					    amount = Integer.parseInt(args[2]);
					    if(getCredits(sender.getName()) - amount < 0){
					    	sender.sendMessage(ChatColor.RED + "You can't have a negative amount of credits!");
					    }else{
					    	setCredits(args[1], getCredits(args[1]) - amount);
						    sender.sendMessage(ChatColor.GREEN + "Success.");
					    }
					} catch (NumberFormatException e) {
					      sender.sendMessage(ChatColor.RED + "Amount of credits not a number!");
					}
				}else{
					sender.sendMessage(ChatColor.RED + "No permission.");
				}
			}else{
				sender.sendMessage(ChatColor.RED + "Unknown command.");
			}
		}else{
			sender.sendMessage(ChatColor.RED + "Too many/too few arguments.");
		}
		return true;
	}
	public int getCredits(String player){
		int credits = 0;
		try {
			ResultSet data = conn.createStatement().executeQuery("SELECT `credits` FROM `credits` WHERE `name`='" + player + "'");
			while(data.next() == true){
				credits = data.getInt("credits");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return credits;
	}
	public void setCredits(String player, int amount){
		boolean exists = false;
		try {
			ResultSet data = conn.createStatement().executeQuery("SELECT `credits` FROM `credits` WHERE `name`='" + player + "'");
			while(data.next() == true){
				exists = true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(exists){
			try {
				conn.createStatement().execute("UPDATE `credits` SET `credits` = '" + amount + "' WHERE `name` = '" + player + "'");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			try {
				conn.createStatement().execute("REPLACE INTO `credits` SET `credits` = '" + amount + "', `name` = '" + player + "'");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
}