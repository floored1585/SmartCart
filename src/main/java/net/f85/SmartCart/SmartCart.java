//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.SmartCart;

import java.util.Set;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.Tag;

public class SmartCart extends JavaPlugin {

    private static SmartCartListener listener;
    static SmartCartUtil util;
    static FileConfiguration config;
    static Logger logger;
    static boolean isDebug;
    static Set<Material> woolTypes;
    
    // these are the block x/y/z offsets from the minecart positions that we will check
    // for wool or sign blocks around the minecart
    static int[][] nextBlocks = {
            {0, -2, 0},
            {1, -1, 0},
            {-1, -1, 0},
            {0, -1, 1},
            {1, -1, -1},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1},
    };

    @Override
    public void onEnable() {
        getLogger().info("Starting up SmartCart");
        //plugin = this;

        // Generate the default config file
        this.saveDefaultConfig();

        config = this.getConfig();

        listener = new SmartCartListener(this);
        util = new SmartCartUtil(this);
        logger = getLogger();
        isDebug = config.getBoolean("debug");
        woolTypes = Tag.WOOL.getValues();

        // Set up command executor
        //commandExecutor = new CommandExecutorUtil(this);
        getLogger().info("Loading commands");
        this.getCommand("sc").setExecutor(new CommandExecutorUtil(this));
        this.getCommand("scSetTag").setExecutor(new CommandSetTag());

        if (isDebug) {
            getLogger().info("Debug is enabled!");
            
            getLogger().info("Listing wool types:");
            woolTypes.forEach(thisWool -> {
                getLogger().info(thisWool.toString());
            });
        }

        getLogger().info("done");
        getLogger().info("Successfully activated SmartCart");
    }


    @Override
    public void onDisable() {
        getLogger().info("Successfully deactivated SmartCart");
    }
}
