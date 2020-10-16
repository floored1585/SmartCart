//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.SmartCart;

import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.block.Block;

import java.util.ArrayList;
import static org.bukkit.Bukkit.getLogger;


public class SmartCartListener implements Listener {

    private net.f85.SmartCart.SmartCart plugin;

    SmartCartListener(SmartCart plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onVehicleUpdate(VehicleUpdateEvent event) {

        Vehicle vehicle = event.getVehicle();

        // Return if vehicle is not a minecart
        if (!( vehicle instanceof Minecart )) {
            return;
        }

        SmartCartVehicle cart = SmartCart.util.getCartFromList( (Minecart) vehicle );

        cart.saveCurrentLocation();
        if (cart.getCart().getPassengers().isEmpty()) {
            cart.setEmptyCartTimer();
        } else {
            cart.resetEmptyCartTimer();
        }

        // Return if minecart is marked for removal, or off rails for any reason
        if ( cart.getCart().isDead() || cart.isNotOnRail() ) {
            return;
        }

        // Return if it isn't a player in the cart
        if ( cart.getCart().getPassengers().isEmpty() || cart.getCart().getPassengers().get(0) != null && cart.getCart().getPassengers().get(0).getType() != EntityType.PLAYER ) {
            return;
        }

        if (cart.isNewBlock()) {
            cart.readControlSign();
        }

        if ( cart.isOnControlBlock() ) {
            cart.executeControl();
        }
        else {
            cart.setPreviousWoolColor(null);
            cart.setSpeed( cart.getConfigSpeed() );
        }

    }


    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {

        Vehicle vehicle = event.getVehicle();

        // Return if vehicle is not a minecart
        if (!( vehicle instanceof Minecart )) {
            return;
        }

        // Return if it wasn't a player that entered
        if ( event.getEntered().getType() != EntityType.PLAYER) {
            return;
        }

        SmartCartVehicle cart = SmartCart.util.getCartFromList( (Minecart) vehicle );

        // Return if minecart is marked for removal, or off rails for any reason
        if ( cart.getCart().isDead() || cart.isNotOnRail() ) {
            return;
        }
    }


    @EventHandler
    public void onVehicleDestroyed(VehicleDestroyEvent event) {

        Vehicle vehicle = event.getVehicle();

        if (vehicle instanceof Minecart) {
            SmartCart.util.getCartFromList( (Minecart) vehicle ).remove(false);
        }
    }


    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (SmartCart.isDebug) {
            getLogger().info("Checking RedstoneBlocks...");
        }
        // Return if the redstone current is turning off instead of on
        if ( event.getOldCurrent() > event.getNewCurrent() ) {
            return;
        }

        // Function takes a location, radius, and material to search for -- get all command blocks
        int search_radius = 1;


        // let's create a variable for all command blocks to be stored
        ArrayList<Block> cmdBlockList = new ArrayList<>();

        // iterate the set of wools and check each of them if they exist around the cart and store it into the BlockList
        SmartCart.woolTypes.forEach(thisWool -> {
            cmdBlockList.addAll(SmartCart.util.getBlocksNearby(event.getBlock(), search_radius, thisWool));
        });

        // Return if we didn't find any command blocks
        if (cmdBlockList.size() == 0) {
            return;
        }

        ArrayList<Block> spawnBlocks = new ArrayList<>();

        // Check each of the command blocks and put spawn blocks in an arraylist
        for (Block thisBlock : cmdBlockList) {
            if (SmartCart.util.isSpawnBlock(thisBlock)) {
                if (SmartCart.isDebug) {
                    getLogger().info("Spawnblock found");
                }
                spawnBlocks.add(thisBlock);
            }
        }

        if (spawnBlocks.size() == 0) {
            return;
        }

        // Now we know block is a control block and the redstone was activating.
        //   Time to take action!

        Block block = spawnBlocks.get(0).getLocation().add(0D, 1D, 0D).getBlock();

        // spawn a cart
        Minecart cart = SmartCart.util.spawnCart(block).getCart();
        if (cart == null) {
            return;
        }

        // pick up a nearby player
        double r = SmartCart.config.getDouble("pickup_radius");
        for (Entity entity : cart.getNearbyEntities(r, r, r)) {
            if (entity instanceof Player && cart.getPassengers().isEmpty() && entity.getVehicle() == null) {
                cart.addPassenger(entity);
                SmartCartVehicle smartCart = SmartCart.util.getCartFromList(cart);
                boolean foundSignNearby = false;

                // let's iterate all blocks around the rail and check for signs.
                for (int[] nextBlock : SmartCart.nextBlocks) {
                    Block thisBlock = smartCart.getCart().getLocation().add(nextBlock[0], nextBlock[1], nextBlock[2]).getBlock();
                    if(SmartCart.util.isSign(thisBlock)){
                        if (SmartCart.isDebug) {
                            getLogger().info("Sign found");
                        }
                        foundSignNearby = true;
                    }
                }
                if (foundSignNearby) {
                    smartCart.executeControl();
                } else {
                    SmartCart.util.sendMessage(entity, "Move in the direction you wish to go.");
                }
                break;
            }
        }
    }
}
