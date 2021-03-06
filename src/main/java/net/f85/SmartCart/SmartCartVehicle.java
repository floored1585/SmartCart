//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.SmartCart;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Material.*;
import org.bukkit.Tag;

class SmartCartVehicle{

    private Minecart cart;
    private Material previousWoolColor;
    private Location currentLocation;
    private Location previousLocation;
    private int[] currentRoughLocation;
    private int[] previousRoughLocation;
    private int emptyCartTimer = 0;
    // Settables
    private double configSpeed = SmartCart.config.getDouble("normal_cart_speed");
    String configEndpoint = "";

    SmartCartVehicle(Minecart vehicle){
        cart = vehicle;
        cart.setMaxSpeed(SmartCart.config.getDouble("max_cart_speed"));
    }


    // Accessors
    Minecart getCart() {
        return cart;
    }

    private Location getPreviousLocation() {
        return previousLocation;
    }
    private Material getPreviousWoolColor() {
        return previousWoolColor;
    }
    double getConfigSpeed() {
        return configSpeed;
    }
    private void setConfigSpeed(Double speed) {
        configSpeed = speed;
    }
    private void setConfigEndpoint(String endpoint) {
        configEndpoint = endpoint;
    }
    void setPreviousWoolColor(Material woolColor) {
        previousWoolColor = woolColor;
    }
    void saveCurrentLocation() {
        previousLocation = currentLocation;
        currentLocation = getLocation();
        previousRoughLocation = currentRoughLocation;
        currentRoughLocation = new int[] {
            currentLocation.getBlockX(), currentLocation.getBlockY(), currentLocation.getBlockZ()
        };
    }


    // These methods just pass through to the Minecart class
    int getEntityId() {
        return getCart().getEntityId();
    }
    private Entity getPassenger() {
        return getCart().getPassengers().isEmpty() ? null : getCart().getPassengers().get(0);
    }
    Location getLocation() {
        return getCart().getLocation();
    }


    boolean isNewBlock() {
        return !Arrays.equals(currentRoughLocation, previousRoughLocation);
    }


    void setEmptyCartTimer() {
        if(
                // These items are not in the default config!
                SmartCart.config.getBoolean("empty_cart_timer_ignore_commandminecart", true) && isCommandMinecart() ||
                SmartCart.config.getBoolean("empty_cart_timer_ignore_explosiveminecart", true) && isExplosiveMinecart() ||
                SmartCart.config.getBoolean("empty_cart_timer_ignore_storagemincart", true) && isStorageMinecart() ||
                SmartCart.config.getBoolean("empty_cart_timer_ignore_hoppermincart", true) && isHopperMinecart() ||
                SmartCart.config.getBoolean("empty_cart_timer_ignore_poweredmincart", true) && isPoweredMinecart() ||
                SmartCart.config.getBoolean("empty_cart_timer_ignore_spawnermincart", true) && isSpawnerMinecart() ||
                SmartCart.config.getInt("empty_cart_timer") == 0
          ) {
            emptyCartTimer = 0;
        } else {
            emptyCartTimer += 1;
            if (emptyCartTimer > net.f85.SmartCart.SmartCart.config.getInt("empty_cart_timer") * 20) {
                remove(true);
            }
        }
    }


    void resetEmptyCartTimer() {
        emptyCartTimer = 0;
    }


    // Returns the block beneath the rail
    private Block getBlockBeneath() {
        return getCart().getLocation().add(0D, -1D, 0D).getBlock();
    }


    // Returns true only if cart is in a rail block
    boolean isNotOnRail() {
        return getCart().getLocation().getBlock().getType() != Material.RAIL;
    }


    // Returns true if the cart is directly above a control block
    boolean isOnControlBlock() {
        return SmartCart.util.isControlBlock( getCart().getLocation().add(0D, -1D, 0D).getBlock() );
    }


    // This looks two blocks below the rail for a sign. Sets the signText variable to
    //  the sign contents if the sign is a valid control sign, otherwise "".
    void readControlSign() {
        for (int[] nextBlock : SmartCart.nextBlocks) {
            Block thisBlock = getCart().getLocation().add(nextBlock[0], nextBlock[1], nextBlock[2]).getBlock();

            if (SmartCart.util.isSign(thisBlock)) {
                executeSign(thisBlock);
            }
        }
    }


    private boolean isMoving() {
        Vector velocity = getCart().getVelocity();
        return (velocity.getX() != 0D || velocity.getZ() != 0D);
    }


    // Sets the speed to the max, in the direction the cart is already travelling
    void setSpeed(double speed) {

        // Check if the cart is empty, and if we should boost empty carts
        if (getCart().isEmpty() && !SmartCart.config.getBoolean("boost_empty_carts")) {
            return;
        }

        Vector velocity = getCart().getVelocity();

        // If the cart is moving
        if (isMoving()) {
            // Maintain velocity

            Vector newVelocity = new Vector();

            // Check to see which axis we're moving along and use that to set the new vector
            //   The signum function just returns 1 if passed a positive, -1 if negative
            if ( Math.abs(velocity.getX()) > Math.abs(velocity.getZ()) ) {
                newVelocity.setX( Math.signum(velocity.getX()) * speed );
            } else {
                newVelocity.setZ( Math.signum(velocity.getZ()) * speed );
            }

            // Update the velocity
            getCart().setVelocity(newVelocity);
        }
    }


    // Destroy the cart (from plugin & server)
    void remove(boolean kill) {

        Entity passenger = getPassenger();

        // Move the passenger out of the cart
        if (passenger != null) {
            // Get location of passenger & add 1 to Y value)
            Location loc = passenger.getLocation();
            loc.setX( cart.getLocation().getBlockX() + 0.5D );
            loc.setY( cart.getLocation().getBlockY());
            loc.setZ( cart.getLocation().getBlockZ() + 0.5D );

            passenger.teleport(loc);
        }

        // Remove from list of carts
        SmartCart.util.removeCart(this);

        // If we need to kill the actual cart, kill it
        if (kill) {
            getCart().remove();
        }
    }


    private void transferSettings(SmartCartVehicle newSC) {
        newSC.setConfigSpeed(configSpeed);
        newSC.setConfigEndpoint(configEndpoint);
    }


    void executeControl() {
        if (getCart().getPassengers().isEmpty()) {
            return;
        }

        Block block = getBlockBeneath();

        if (Tag.WOOL.isTagged(block.getType())) {
            if (block.getType() == ORANGE_WOOL) {
                setPreviousWoolColor(ORANGE_WOOL);
                setSpeed(SmartCart.config.getDouble("slow_cart_speed"));
                if (SmartCart.isDebug) {
                    getLogger().info("[SmartCart DEBUG] Orange Wool Block activated, slowing player");
                }
            }
            if (block.getType() == YELLOW_WOOL) {
                if (SmartCart.isDebug) {
                    getLogger().info("[SmartCart DEBUG] Yellow Wool found...");
                }
                // If the cart is near the center of the block, kill it.  Otherwise, slow it down.
                if (isLeavingBlock()) {
                    Entity passenger = cart.getPassengers().get(0);
                    remove(true);
                    if (SmartCart.isDebug) {
                        getLogger().info("[SmartCart DEBUG] Yellow Wool Block activated, ejecting player");
                    }
                    // checking for signs, so we iterate the nextBlocks line by line to search for one
                    for (int[] nextBlock : SmartCart.nextBlocks) {
                        Block thisBlock = getCart().getLocation().add(nextBlock[0], nextBlock[1], nextBlock[2]).getBlock();
                        if (SmartCart.util.isSign(thisBlock)) {
                            executeEJT(passenger, thisBlock);
                        }
                    }
                } else {
                    setSpeed(0.1D);
                }
            }
            if (block.getType() == GREEN_WOOL) {
                if (SmartCart.isDebug) {
                    getLogger().info("[SmartCart DEBUG] Green Wool found...");
                }
                //   If we have already executed this block and ARE moving, teleport the cart
                //   in the direction the player is facing.
                if (getPreviousWoolColor() == GREEN_WOOL) {
                    if (isMoving() && getBlockAheadPassenger() != null) {
                        Block blockAhead = getBlockAheadPassenger();
                        Entity passenger = getCart().getPassengers().get(0);
                        if (SmartCart.util.isRail(blockAhead)) {
                            remove(true);
                            SmartCartVehicle newSC = SmartCart.util.spawnCart(blockAhead);
                            newSC.getCart().addPassenger(passenger);
                            transferSettings(newSC);
                        }
                    }
                    return;
                }
                // If the cart is near the center of the block, stop it.  Otherwise, slow it down.
                if (isLeavingBlock()) {
                    setPreviousWoolColor(GREEN_WOOL);
                    setSpeed(0D);
                    sendPassengerMessage("Move in the direction you wish to go.", true);
                } else {
                    // Otherwise, slow the cart down
                    setSpeed(0.1D);
                }
            }
            if (block.getType() == RED_WOOL) {
                if (SmartCart.isDebug) {
                    getLogger().info("[SmartCart DEBUG] Red Block found...");
                }
                // If we're not half way through the block, return
                if (!isLeavingBlock()) {
                    setSpeed(0.1D);
                    return;
                }
                // If we just executed the elevator, return
                if (getPreviousWoolColor() == RED_WOOL) {
                    return;
                }

                setPreviousWoolColor(RED_WOOL);

                // Get the tp target, destroy old cart, spawn new cart at tp target,
                //   tp passenger, load passenger into new cart
                Block elevator = SmartCart.util.getElevatorBlock(block.getLocation());
                if (elevator == null) {
                    return;
                }
                Block tpTarget = elevator.getLocation().add(0, 1, 0).getBlock();
                Entity passenger = getCart().getPassengers().get(0);
                Vector cartVelocity = getCart().getVelocity();

                // Set the new passenger location
                Location passengerLoc = passenger.getLocation();
                passengerLoc.setY(tpTarget.getLocation().getBlockY());
                // Kill the cart, spawn a new one
                remove(true);
                SmartCartVehicle newCart = SmartCart.util.spawnCart(tpTarget);
                // Teleport passenger to new location and load them in the cart
                passenger.teleport(passengerLoc);
                newCart.getCart().addPassenger(passenger);
                // Get the cart going!
                newCart.getCart().setVelocity(cartVelocity);
                newCart.setSpeed(1);
                // Set the previous wool color of the new cart, to prevent a tp loop
                newCart.setPreviousWoolColor(RED_WOOL);
                transferSettings(newCart);
            }
            if (block.getType() == BLACK_WOOL) {
                setSpeed(0.2D);
            }
        }
    }


    String getPassengerName() {
        if (getCart().getPassengers().isEmpty()) {
            return "None";
        }
        return getCart().getPassengers().get(0).getName();
    }


    // Returns the block directly ahead of the passenger
    private Block getBlockAheadPassenger() {
        // Get the passenger's direction as an integer
        //   -1/3 = pos x
        //   -2/2 = neg z
        //   -3/1 = neg x
        //   -4/0 = pos z
        if(getCart().getPassengers().isEmpty()) return null;
        int passengerDir = Math.round( getCart().getPassengers().get(0).getLocation().getYaw() / 90f );
        Block block = null;
        switch (passengerDir) {
            case 0:
            case -4:
                block = getCart().getLocation().add(0,0,1).getBlock();
                break;
            case 1:
            case -3:
                block = getCart().getLocation().add(-1,0,0).getBlock();
                break;
            case 2:
            case -2:
                block = getCart().getLocation().add(0,0,-1).getBlock();
                break;
            case 3:
            case -1:
                block = getCart().getLocation().add(1,0,0).getBlock();
                break;
        }
        return block;
    }

    // Find out if the cart is headed towards or away from the middle of the current block
    private boolean isLeavingBlock() {

        // Gotta check to make sure this exists first
        if (getPreviousLocation() == null) {
            return false;
        }
        // If we just moved to a new block, the previous location is invalid for this check
        if (getPreviousLocation().getBlockX() != getLocation().getBlockX()
                || getPreviousLocation().getBlockZ() != getLocation().getBlockZ()) {
            // This lets you chain control blocks by setting the prev wool color to null unless we
            // just got off an elevator.
            if (Math.abs(getPreviousLocation().getBlockY() - getLocation().getBlockY()) < 2) {
                setPreviousWoolColor(null);
            }
            return false;
        }

        // Get the previous and current locations
        double prevX = Math.abs( getPreviousLocation().getX() );
        double prevZ = Math.abs( getPreviousLocation().getZ() );
        double currX = Math.abs( getLocation().getX() );
        double currZ = Math.abs( getLocation().getZ() );

        // Just get the decimal part of the double
        prevX = prevX - (int) prevX;
        prevZ = prevZ - (int) prevZ;
        currX = currX - (int) currX;
        currZ = currZ - (int) currZ;

        // Get distance from the middle of the block
        double prevDistFromMidX = Math.abs( prevX - 0.5 );
        double prevDistFromMidZ = Math.abs( prevZ - 0.5 );
        double currDistFromMidX = Math.abs( currX - 0.5 );
        double currDistFromMidZ = Math.abs( currZ - 0.5 );

        return currDistFromMidX > prevDistFromMidX || currDistFromMidZ > prevDistFromMidZ || (currDistFromMidX < 0.1 && currDistFromMidZ < 0.1);
    }

    private void sendPassengerMessage(String message, boolean prefix){
        if(prefix) message = "§6[SmartCart] §7" + message;
        else message = "§7" + message;
        Entity entity = getPassenger();
        if(entity instanceof Player){
            ((Player)entity).sendRawMessage(message);
        }
    }

    private boolean isCommandMinecart() {
        return getCart() instanceof CommandMinecart;
    }

    private boolean isExplosiveMinecart() {
        return getCart() instanceof ExplosiveMinecart;
    }

    private boolean isHopperMinecart() {
        return getCart() instanceof HopperMinecart;
    }

    private boolean isPoweredMinecart() {
        return getCart() instanceof PoweredMinecart;
    }

    private boolean isSpawnerMinecart() {
        return getCart() instanceof SpawnerMinecart;
    }

    private boolean isStorageMinecart() {
        return getCart() instanceof StorageMinecart;
    }

    private static List<Pair<String, String>> parseSign(Sign sign){
        List<Pair<String, String>> ret = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for( String value : sign.getLines() ) { // Merge all the sign's lines
            stringBuilder.append(value);
        }
        String text = stringBuilder.toString();
        // Check to see if the sign string matches the control sign prefix; return otherwise
        Pattern p = Pattern.compile(SmartCart.config.getString("control_sign_prefix_regex"));
        Matcher m = p.matcher(text);
        // Return if the control prefix isn't matched
        if (!m.find()) return ret;
        String signText = m.replaceAll(""); // Remove the control prefix
        for(String pair : signText.split("\\|")) {
            String[] tokens = pair.split(":");
            if (tokens.length != 2) {
                if (SmartCart.isDebug) {
                    getLogger().info("[SmartCart DEBUG] Invalid sign string: " + pair);
                }
                continue;
            }
            tokens[0] = tokens[0].replaceAll("\\s+", "");
            if(!tokens[0].contains("MSG")){
                tokens[1] = tokens[1].replaceAll("\\s+", "");
            }
            ret.add(new Pair<>(tokens[0], tokens[1]));
        }
        return ret;
    }

    private static void spawnCartInNewDirection(SmartCartVehicle oldCart, String direction){
        // Since it wasn't specified whether only new carts should be redirected, act on all carts
        spawnCartInNewDirection(oldCart, direction, false);
    }
    private static void spawnCartInNewDirection(SmartCartVehicle oldCart, String direction, boolean newCartsOnly){
        // If we're only supposed affect new carts and this one has been alive a while, just return
        if (newCartsOnly && oldCart.cart.getTicksLived() > 1) {
            return;
        }
        if (SmartCart.isDebug) {
            getLogger().info("[SmartCart DEBUG] Spawn block activated");
        }
        Entity passenger = null;
        if (!oldCart.cart.getPassengers().isEmpty()) {
            passenger = oldCart.cart.getPassengers().get(0);
        }
        Block blockAhead = null;
        Vector vector = new Vector(0, 0, 0);
        switch (direction) {
            case "N":
                blockAhead = oldCart.cart.getLocation().add(0D, 0D, -1D).getBlock();
                vector = new Vector(0, 0, -1);
                break;
            case "S":
                blockAhead = oldCart.cart.getLocation().add(0D, 0D, 1D).getBlock();
                vector = new Vector(0, 0, 1);
                break;
            case "E":
                blockAhead = oldCart.cart.getLocation().add(1D, 0D, 0D).getBlock();
                vector = new Vector(1, 0, 0);
                break;
            case "W":
                blockAhead = oldCart.cart.getLocation().add(-1D, 0D, 0D).getBlock();
                vector = new Vector(-1, 0, 0);
                break;
        }
        if (SmartCart.util.isRail(blockAhead)) {
            oldCart.remove(true);
            SmartCartVehicle newSC = SmartCart.util.spawnCart(blockAhead);
            newSC.getCart().addPassenger(passenger);
            newSC.getCart().setVelocity(vector);
            oldCart.transferSettings(newSC);
        }
    }

    private void executeSign(Block block) {
        if (isNotOnRail()) {
            return;
        }
        if (SmartCart.isDebug) {
            getLogger().info("[SmartCart DEBUG] Sign activated, processing command, type is " + block.getType());
        }
        boolean foundEndpoint = false;

        Sign sign = (Sign) block.getState(); // Cast to Sign


        for (Pair<String, String> pair : parseSign(sign)) {
            Pattern p;
            if (pair.left().equals("$LNC")) {
                if (getCart().getLocation().add(0, -1, 0).getBlock().getType() == BLACK_WOOL) {
                    // Spawn a cart in the direction the sign says, ONLY if it's a new cart
                    spawnCartInNewDirection(this, pair.right(), true);
                }
            }
            if (pair.left().equals("$SPD")) {
                p = Pattern.compile("^\\d*\\.?\\d+");
                Double minSpeed = 0D;
                Double maxSpeed = SmartCart.config.getDouble("max_cart_speed");
                if (!p.matcher(pair.right()).find() || Double.parseDouble(pair.right()) > maxSpeed || Double.parseDouble(pair.right()) < minSpeed) {
                    sendPassengerMessage("Bad speed value: \"" + pair.right() + "\". Must be a numeric value (decimals OK) between "
                            + minSpeed + " and " + maxSpeed + ".", true);
                    return;
                }
                configSpeed = Double.parseDouble(pair.right());
            }
            if (pair.left().equals("$MSG")) {
                sendPassengerMessage(pair.right(), false);
            }
            if (pair.left().equals("$END")) {
                configEndpoint = pair.right();
                sendPassengerMessage("Endpoint set to §a" + pair.right(), true);
            }
            if (pair.left().equals("$TAG")) {
                configEndpoint = pair.right();
                sendPassengerMessage("Set tag to §a" + pair.right(), true);
            }
            if (pair.left().equals("$N")) {
                if (cart.getVelocity().getZ() < 0) {
                    spawnCartInNewDirection(this, pair.right());
                }
            }
            if (pair.left().equals("$E")) {
                if (cart.getVelocity().getX() > 0) {
                    spawnCartInNewDirection(this, pair.right());
                }
            }
            if (pair.left().equals("$W")) {
                if (cart.getVelocity().getX() < 0) {
                    spawnCartInNewDirection(this, pair.right());
                }
            }
            if (pair.left().equals("$S")) {
                if (cart.getVelocity().getZ() > 0) {
                    spawnCartInNewDirection(this, pair.right());
                }
            }
            if (pair.left().equals(configEndpoint) || pair.left().equals("$DEF")) {
                // Skip this if we already found and used the endpoint
                Entity passenger = null;
                if (!cart.getPassengers().isEmpty()) {
                    passenger = cart.getPassengers().get(0);
                }
                if (foundEndpoint || passenger == null) {
                    return;
                }
                foundEndpoint = true;
                Block blockAhead = null;
                Vector vector = new Vector(0, 0, 0);
                switch (pair.right()) {
                    case "N":
                        blockAhead = cart.getLocation().add(0D, 0D, -1D).getBlock();
                        vector = new Vector(0, 0, -1);
                        break;
                    case "S":
                        blockAhead = cart.getLocation().add(0D, 0D, 1D).getBlock();
                        vector = new Vector(0, 0, 1);
                        break;
                    case "E":
                        blockAhead = cart.getLocation().add(1D, 0D, 0D).getBlock();
                        vector = new Vector(1, 0, 0);
                        break;
                    case "W":
                        blockAhead = cart.getLocation().add(-1D, 0D, 0D).getBlock();
                        vector = new Vector(-1, 0, 0);
                        break;
                }
                if (SmartCart.util.isRail(blockAhead)) {
                    remove(true);
                    SmartCartVehicle newSC = SmartCart.util.spawnCart(blockAhead);
                    newSC.getCart().addPassenger(passenger);
                    newSC.getCart().setVelocity(vector);
                    transferSettings(newSC);
                }
            }
        }
    }

    private void executeEJT(Entity passenger, Block block){
        Sign sign = (Sign) block.getState();

        for (Pair<String, String> pair : parseSign(sign)) {
            if (pair.left().equals("$EJT") && pair.right().length() >= 2) {
                int dist = Integer.parseInt(pair.right().substring(1, pair.right().length()));
                if (SmartCart.isDebug) {
                    getLogger().info("[SmartCart DEBUG] Ejection sign found, ejecting player towards: " + pair.right().charAt(0));
                }

                switch (pair.right().charAt(0)) {
                    case 'N':
                        passenger.teleport(passenger.getLocation().add(0, 0, -dist));
                        break;
                    case 'E':
                        passenger.teleport(passenger.getLocation().add(dist, 0, 0));
                        break;
                    case 'S':
                        passenger.teleport(passenger.getLocation().add(0, 0, dist));
                        break;
                    case 'W':
                        passenger.teleport(passenger.getLocation().add(-dist, 0, 0));
                        break;
                    case 'U':
                        passenger.teleport(passenger.getLocation().add(0, dist, 0));
                        break;
                    case 'D':
                        passenger.teleport(passenger.getLocation().add(0, -dist, 0));
                        break;
                }
            }
        }

    }
}
