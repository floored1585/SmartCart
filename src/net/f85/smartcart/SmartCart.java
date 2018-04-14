//
// smartcart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.smartcart;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public class SmartCart extends JavaPlugin {


    private static SmartCartListener listener;
    static SmartCartUtil util;
    static FileConfiguration config;
    static Logger logger;


    @Override
    public void onEnable() {
        getLogger().info("Starting up smartcart");
        //plugin = this;

        // Generate the default config file
        this.saveDefaultConfig();

        config = this.getConfig();

        listener = new SmartCartListener(this);
        util = new SmartCartUtil(this);
        logger = getLogger();

        // Set up command executor
        //commandExecutor = new CommandExecutorUtil(this);
        getLogger().info("Loading commands");
        this.getCommand("sc").setExecutor(new CommandExecutorUtil(this));
        this.getCommand("scSetTag").setExecutor(new CommandSetTag());
        getLogger().info("done");
        getLogger().info("Successfully activated smartcart");
    }


    @Override
    public void onDisable() {
        getLogger().info("Successfully deactivated smartcart");
    }
}
