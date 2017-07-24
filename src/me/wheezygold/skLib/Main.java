package me.wheezygold.skLib;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import me.wheezygold.metrics.Metrics;
import me.wheezygold.skLib.WIP.EffCreateScrollSB;
import me.wheezygold.skLib.common.RedisConfig;
import me.wheezygold.skLib.common.Util;
import me.wheezygold.skLib.common.redis.Subscriber;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Main extends JavaPlugin implements Listener {
	
    public static Plugin plugin;
    public static Subscriber subscriber;
    public static JedisPool pool;
    public static String ip;
    public static String password;
    public static int port;
    public static String[] channels;
    public static boolean connected;
    
    static {
        connected = false;
    }
	
	public void loadConfiguration() {
		String _kickpath = "values.pastebin.apikey";
		getConfig().addDefault(_kickpath, "api-key-here");
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	private static Main instance;

	@SuppressWarnings("rawtypes")
	@Override
	public void onEnable() {
		Util.sendCMsg("Creating Instance...");
		instance = this;
		plugin = this;
		Util.sendCMsg("Instance has been Created!");
		Util.sendCMsg("Loading Configuration...");
		loadConfiguration();
		reloadConfig();
		RedisConfig.getConfig();
		Util.sendCMsg("Loaded Configuration...(most likely)");
		Util.sendCMsg("Registering Events...");
		Bukkit.getPluginManager().registerEvents(this, this);
		Util.sendCMsg("Registered Events!");
		Util.sendCMsg("Loading Metrics...");
		Metrics metrics = new Metrics(this);
		metrics.addCustomChart(new Metrics.SimplePie("skript_version") {
            @Override
            public String getValue() {
                return Bukkit.getServer().getPluginManager().getPlugin("Skript").getDescription().getVersion();
            }
        });
		metrics.addCustomChart(new Metrics.SimplePie("plugin_version") {
            @Override
            public String getValue() {
                return getInstance().getDescription().getVersion();
            }
        });
		Util.sendCMsg("Loaded Metrics!");
		if (getServer().getPluginManager().getPlugin("Skript")!=null) {
			Util.sendCMsg("Skript has been found!");
			Util.sendCMsg("Registing Addon...");
			SkriptAddon sk = Skript.registerAddon(this);
			Util.sendCMsg("Registered Addon!");
			if (Skript.isAcceptRegistrations()) {
				Util.sendCMsg("Looks like Skript is looking for syntax so lets throw some shit at it...");
				Skript.registerEffect(EffCreateScrollSB.class, "create a magic scoreboard with title %string% for %player%");
				Util.sendCMsg("Going to start to load the syntax...");
				try {
					sk.loadClasses("me.wheezygold.skLib", "skript");
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (Bukkit.getBukkitVersion().contains("1.8")) {
					Util.sendCMsg("Looks like you are running MC 1.8...Loading the 1.8 Skript syntax...");
					try {
						sk.loadClasses("me.wheezygold.skLib.skript", "V1_8");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				Util.sendCMsg("Loaded all of the Skript syntax!");
			} else {
				Util.sendCMsg("Skript is not looking to accept syntax/registrations. Did you restart the server?");
			}
			if (getServer().getPluginManager().getPlugin("RediSK")!=null) {
				Util.sendCMsg("You already have RediSK installed so we are not going to load the RediSK syntax or stuff.");
			} else {
				Util.sendCMsg("You do not have RediSK so let's will load the syntax now!");
				Util.sendCMsg("Loading the RediSK stuff now...");
				ip = RedisConfig.getConfig().getString("redis-ip");
		        port = RedisConfig.getConfig().getInt("redis-port");
		        String password = RedisConfig.getConfig().getString("redis-password");
		        List chnls = RedisConfig.getConfig().getList("channels");
		        this.getLogger().info("Listening for channels:");
		        channels = new String[chnls.size()];
		        int counter = 0;
		        Iterator iterator = chnls.iterator();
		        while (iterator.hasNext()) {
		            String s;
		            Main.channels[counter] = s = (String)iterator.next();
		            this.getLogger().info(String.valueOf(Integer.toString(++counter)) + ": " + s);
		        }
		        Util.sendCMsg("Starting the Jedis Pool...");
		        pool = password == null || password.equals("") ? new JedisPool((GenericObjectPoolConfig)new JedisPoolConfig(), ip, port, 0) : new JedisPool(new JedisPoolConfig(), ip, port, 0, password);
		        new BukkitRunnable(){

		            public void run() {
		                Main.subscriber = new Subscriber();
		                Jedis jedis = Main.pool.getResource();
		                try {
		                	Main.connected = true;
		                    jedis.subscribe(Main.subscriber, Main.channels);
		                }
		                catch (Exception e) {
		                    Main.this.getLogger().severe("Can't connect to Redis! Are you sure it's running and your config is correct?");
		                }
		                jedis.close();
		                Main.connected = false;
		            }
		        }.runTaskAsynchronously((Plugin)this);
		        Util.sendCMsg("Loading the RediSK syntax...");
		        new me.wheezygold.skLib.common.redis.RegisterSkript(this);
		        Util.sendCMsg("Loaded the RediSK Syntax!");
		        Util.sendCMsg("Finished loading the RediSK stuff, ily MFN <3.");
			}
		} else {
			Util.sendCMsg("Skript has not been found, you idiot what do you think a skript addon is, so expect nothing to register.");
		}
		Util.sendCMsg("skLib has been loaded. Enjoy!");
	}
	
	@Override
	public void onDisable() {
		if (connected) {
			Util.sendCMsg("You are connected to Redis");
            subscriber.unsubscribe();
            pool.destroy();
        }
		Util.sendCMsg("skLib has been disabled! o/ Cya next time.");
	}
	
	public static Main getInstance() {
		return instance;
	}
	
	public static void sendMessage(final String channel, final String msg) {
        if (!connected) {
            return;
        }
        Jedis jedis = pool.getResource();
        new BukkitRunnable(){

            public void run() {
                if (!Main.connected) {
                    return;
                }
                try {
                	jedis.publish(channel, msg);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                jedis.close();
            }
        }.runTaskAsynchronously(plugin);
    }
	
	 String[] args = null;

	 @Override
	    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	        if (command.getName().equalsIgnoreCase("skLib")) {
	        	if (sender instanceof Player) {
	        		final Player p = (Player) sender;
		        	Util.sendMsg(p, "Reloaded the config!");
	        	} else {
		        	Util.sendCMsg("Reloaded the config!");
	        	}
		        reloadConfig();
		        return true;
	        }
			return false;
	 }
}