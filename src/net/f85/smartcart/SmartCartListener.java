//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.smartcart;

import org.bukkit.block.Sign;
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
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SmartCartListener implements Listener {


    private SmartCart plugin;


    public SmartCartListener(SmartCart plugin) {
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

        if(cart.isNewBlock()) {
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

        // Return if the redstone current is turning off instead of on
        if ( event.getOldCurrent() > event.getNewCurrent() ) {
            return;
        }

        // Function takes a location, radius, and material to search for -- get all command blocks
        int search_radius = 1;
        ArrayList<Block> cmdBlockList = SmartCart.util.getBlocksNearby(event.getBlock(), search_radius, Material.WOOL);

        // Return if we didn't find any command blocks
        if (cmdBlockList.size() == 0) {
            return;
        }

        ArrayList<Block> spawnBlocks = new ArrayList<>();

        // Check each of the command blocks and put spawn blocks in an arraylist
        for (Block thisBlock : cmdBlockList) {
            if (SmartCart.util.isSpawnBlock(thisBlock)) {
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
                Block block1 = cart.getLocation().add(0, -2, 0).getBlock();
                if (block1.getType() == Material.SIGN || block1.getType() == Material.SIGN_POST || block1.getType() == Material.WALL_SIGN) {
                    Sign sign = (Sign) block1.getState();
                    if(cart.getVelocity().getX() == 0 && cart.getVelocity().getZ() == 0) {
                        for (Pair<String, String> pair : SmartCartVehicle.parseSign(sign)) {
                            if (pair.left().equals("$LNC"))
                                switch (pair.right()) {
                                    case "N":
                                        if(SmartCart.util.isRail(cart.getLocation().add(0, 0, -1).getBlock())) {
                                            cart.teleport(cart.getLocation().add(0, 0, -1));
                                            cart.setVelocity(new Vector(0, 0, -1));
                                        }
                                        break;
                                    case "E":
                                        if(SmartCart.util.isRail(cart.getLocation().add(1, 0, 0).getBlock())) {
                                            cart.teleport(cart.getLocation().add(1, 0, 0));
                                            cart.setVelocity(new Vector(1, 0, 0));
                                        }
                                        break;
                                    case "W":
                                        if(SmartCart.util.isRail(cart.getLocation().add(-1, 0, 0).getBlock())) {
                                            cart.teleport(cart.getLocation().add(-1, 0, 0));
                                            cart.setVelocity(new Vector(-1, 0, 0));
                                        }
                                        break;
                                    case "S":
                                        if(SmartCart.util.isRail(cart.getLocation().add(0, 0, 1).getBlock())) {
                                            cart.teleport(cart.getLocation().add(0, 0, 1));
                                            cart.setVelocity(new Vector(0, 0, 1));
                                        }
                                        break;
                                }
                        }
                    }
                }
                else SmartCart.util.sendMessage(entity, "Move in the direction you wish to go.");
                break;
            }
        }
    }
}
