//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.smartcart;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.material.Wool;

import java.util.ArrayList;
import java.util.List;

class SmartCartUtil {

    private ArrayList<SmartCartVehicle> cartList = new ArrayList<>();
    private SmartCart plugin;

    SmartCartUtil(SmartCart plugin) {
        this.plugin = plugin;
    }


    // Accessors!
    ArrayList<SmartCartVehicle> getCartList() {
        return new ArrayList<>(cartList);
    }


    // This returns an ArrayList of all carts in the provided world
    ArrayList<SmartCartVehicle> getCartList(World world) {

        // This is where the carts we find, if any, will go
        ArrayList<SmartCartVehicle> worldCarts = new ArrayList<>();

        // Loop through all carts, saving the ones that match
        for (SmartCartVehicle cart : cartList) {
            if (cart.getLocation().getWorld() == world) {
                worldCarts.add(cart);
            }
        }

        return worldCarts;
    }


    SmartCartVehicle getCartFromList(Minecart requestedCart) {

        // Search for an existing SmartCartVehicle first
        for (SmartCartVehicle cart : cartList) {
            if (cart.getEntityId() == requestedCart.getEntityId()) {
                return cart;
            }
        }

        // If the cart doesn't already exist as a SmartCartVehicle, create it
        SmartCartVehicle newCart = new SmartCartVehicle(requestedCart);
        cartList.add(newCart);
        return newCart;
    }

    SmartCartVehicle getCartFromList(int entityID) {
        // Search for an existing SmartCartVehicle first
        for (SmartCartVehicle cart : cartList) {
            if (cart.getEntityId() == entityID) {
                return cart;
            }
        }
        return null;
    }


    // Find & remove the cart from cartList
    void removeCart(SmartCartVehicle deadCart) {
        // Find the cart that is being removed
        for (SmartCartVehicle cart : cartList) {
            if (cart.getEntityId() == deadCart.getEntityId()) {
                cartList.remove(cart);
                break;
            }
        }
    }

    void killCarts(ArrayList<SmartCartVehicle> removeCartList) {
        for (SmartCartVehicle cart : removeCartList) {
            cart.remove(true);
        }
    }


    boolean isControlBlock(Block block) {
        Block blockAbove = block.getLocation().add(0,1,0).getBlock();
        return (block.getType() == Material.WOOL && blockAbove.getType() == Material.RAILS);
    }


    private boolean isElevatorBlock(Block block) {
        return isControlBlock(block) && (block.getState().getData() instanceof Wool && ((Wool)block.getState().getData()).getColor() == DyeColor.RED);
    }


    boolean isSpawnBlock(Block block) {
        return isControlBlock(block) && (block.getState().getData() instanceof Wool && ((Wool)block.getState().getData()).getColor() == DyeColor.BLACK);
    }


    // This searches methodically through a cube with a side length of (radius * 2 + 1)
    //   for the material passed.  Returns an ArrayList of Blocks containing all matching
    //   material found
    ArrayList<Block> getBlocksNearby(Block centerBlock, int radius, Material material) {

        ArrayList<Block> blockList = new ArrayList<>();

        for (double xOffset = radius; xOffset >= radius * -1; xOffset--) {
            for (double yOffset = radius; yOffset >= radius * -1; yOffset--) {
                for (double zOffset = radius; zOffset >= radius * -1; zOffset--) {
                    Block testBlock = centerBlock.getLocation().add(xOffset, yOffset, zOffset).getBlock();
                    if (testBlock.getType() == material) {
                        blockList.add(testBlock);
                    }
                }
            }
        }
        return blockList;
    }


    // Send a message to the player
    void sendMessage(Entity entity, String message) {
        message = "§6[SmartCart] §7" + message;
        if (entity instanceof Player) {
            ((Player) entity).sendRawMessage(message);
        }
    }


    // This method returns a delineated list of the worlds on the server
    String getWorldList(String separator) {

        List<World> worldList = Bukkit.getWorlds();
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 1;

        for (World world : worldList) {
            stringBuilder.append(world.getName());
            // If we still have more worlds to add, insert separator
            if (worldList.size() > counter) {
                stringBuilder.append(separator);
            }
            counter += 1;
        }
        return stringBuilder.toString();
    }


    // Send the provided list of carts to the provided entity
    void sendCartList(ArrayList<SmartCartVehicle> sendCartList, Entity entity) {

        // These values are used to format the columns
        int padID = 7;
        int padWorld = 10;
        int padLocation = 18;
        int padPassenger = 20;
        int padAge = 5;

        String message = "\n"
                + padRight("ID", padID)
                + padRight("World", padWorld)
                + padRight("Location", padLocation)
                + padRight("Passenger", padPassenger)
                + padRight("Age", padAge)
                + "\n";
        StringBuilder stringBuilder = new StringBuilder();
        for (SmartCartVehicle cart : sendCartList) {
            stringBuilder.
                    append(padRight(Integer.toString(cart.getEntityId()), padID)).
                    append(padRight(cart.getLocation().getWorld().getName(), padWorld)).
                    append(padRight(getLocationString(cart.getLocation()), padLocation)).
                    append(padRight(cart.getPassengerName(), padPassenger)).
                    append(padRight(getAgeString(cart.getCart()), padAge)).
                    append("\n");
        }
        message += stringBuilder.toString();
        message += "Total: " + Integer.toString(sendCartList.size());

        sendMessage(entity, message);
    }


    private static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    //public static String padLeft(String s, int n) {
    //    return String.format("%1$" + n + "s", s);
    //}


    private String getLocationString(Location loc) {
        return Integer.toString( loc.getBlockX() ) + ","
                + Integer.toString( loc.getBlockY() ) + ","
                + Integer.toString( loc.getBlockZ() );
    }


    private String getAgeString(Entity entity) {
        int ticks = entity.getTicksLived();
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds/60, seconds%60);
    }


    boolean isRail(Block block) {
        return block != null && block.getType() == Material.RAILS;
    }


    //public boolean isRail(Location loc) {
    //   return isRail(loc.getBlock());
    //}


    // Checks to see if a string is really an integer!
    static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c <= '/' || c >= ':') {
                return false;
            }
        }
        return true;
    }


    private Minecart getCartAtBlock(Block block) {
        for (Entity entity : block.getLocation().getChunk().getEntities() ) {
            // If the entity is a minecart and on the location of the block passed, return the cart
            if (entity instanceof Minecart
                    && entity.getLocation().getBlockX() == block.getLocation().getX()
                    && entity.getLocation().getBlockY() == block.getLocation().getY()
                    && entity.getLocation().getBlockZ() == block.getLocation().getZ() )
            {
                return (Minecart)entity;
            }
        }
        return null;
    }


    SmartCartVehicle spawnCart(Block block) {
        // Check to make sure the block is a rail and no cart already exists here
        Minecart cartAtBlock = getCartAtBlock(block);
        if ( isRail(block) && cartAtBlock == null ) {
            Location loc = block.getLocation().add(0.5D,0D,0.5D);
            Minecart cart = loc.getWorld().spawn(loc, Minecart.class);
            return getCartFromList(cart);
        }
        return getCartFromList(cartAtBlock);
    }


    Block getElevatorBlock(Location loc) {
        Location cmdLoc = loc.clone();
        for (int y = 0; y < 256; y++) {
            if (y == cmdLoc.getBlockY()) {
                continue;
            }
            loc.setY(y);
            if (isElevatorBlock( loc.getBlock() )) {
                return loc.getBlock();
            }
        }
        return null;
    }

    boolean isSign(Block block){
        return block.getType() == Material.SIGN || block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN;
    }
}