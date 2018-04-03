//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.smartcart;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
//import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.util.Vector;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartCartVehicle {

    private Minecart cart;
    private DyeColor previousWoolColor;
    private Location currentLocation;
    private Location previousLocation;
    private int[] currentRoughLocation;
    private int[] previousRoughLocation;
    private SmartCart plugin;
    //private String signText;
    private int emptyCartTimer = 0;
    // Settables
    private double configSpeed = SmartCart.config.getDouble("normal_cart_speed");
    private String configEndpoint = "";


    SmartCartVehicle(SmartCart plugin, Minecart vehicle) {
        this.plugin = plugin;
        cart = vehicle;
        cart.setMaxSpeed(SmartCart.config.getDouble("max_cart_speed"));
    }


    // Accessors
    public Minecart getCart() {
        return cart;
    }
    private Location getPreviousLocation() {
        return previousLocation;
    }
    private DyeColor getPreviousWoolColor() {
        return previousWoolColor;
    }
    public Double getConfigSpeed() {
        return configSpeed;
    }
    private void setConfigSpeed(Double speed) {
        configSpeed = speed;
    }
    //public String getConfigEndpoint() {
    //    return configEndpoint;
    //}
    private void setConfigEndpoint(String endpoint) {
        configEndpoint = endpoint;
    }
    //public String getSignText() {
    //    return signText;
    //}
    public void setPreviousWoolColor(DyeColor color) {
        previousWoolColor = color;
    }
    public void saveCurrentLocation() {
        previousLocation = currentLocation;
        currentLocation = getLocation();
        previousRoughLocation = currentRoughLocation;
        currentRoughLocation = new int[] {
                currentLocation.getBlockX(), currentLocation.getBlockY(), currentLocation.getBlockZ()
        };
    }


    // These methods just pass through to the Minecart class
    public int getEntityId() {
        return getCart().getEntityId();
    }
    private Entity getPassenger() {
        return getCart().getPassengers().isEmpty() ? null : getCart().getPassengers().get(0);
    }
    public Location getLocation() {
        return getCart().getLocation();
    }


    public boolean isNewBlock() {
        return !Arrays.equals(currentRoughLocation, previousRoughLocation);
    }


    public void setEmptyCartTimer() {
        if(
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
            if (emptyCartTimer > SmartCart.config.getInt("empty_cart_timer") * 20) {
                remove(true);
            }
        }
    }


    public void resetEmptyCartTimer() {
        emptyCartTimer = 0;
    }


    // Returns the block beneath the rail
    private Block getBlockBeneath() {
        return getCart().getLocation().add(0D, -1D, 0D).getBlock();
    }


    // Returns true only if cart is in a rail block
    public boolean isNotOnRail() {
        return getCart().getLocation().getBlock().getType() != Material.RAILS;
    }


    // Returns true if the cart is directly above a control block
    public boolean isOnControlBlock() {
        return SmartCart.util.isControlBlock( getCart().getLocation().add(0D, -1D, 0D).getBlock() );
    }


    // This looks two blocks below the rail for a sign. Sets the signText variable to
    //   the sign contents if the sign is a valid control sign, otherwise "".
    public void readControlSign() {
        Block block = getCart().getLocation().add(0D, -2D, 0D).getBlock();
        // Return if we're not over a sign
        if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST) {
            return;
        }
        // Return if we're not on rails
        if (isNotOnRail()) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState(); // Cast to Sign
        for( String value : sign.getLines() ) { // Merge all the sign's lines
            stringBuilder.append(value);
        }
        String text = stringBuilder.toString();
        // Check to see if the sign string matches the control sign prefix; return otherwise
        Pattern p = Pattern.compile(SmartCart.config.getString("control_sign_prefix_regex"));
        Matcher m = p.matcher(text);
        // Return if the control prefix isn't matched
        if (!m.find()) {
            return;
        }
        String signText = m.replaceAll(""); // Remove the control prefix

        Boolean foundEndpoint = false;

        for(String pair : signText.split("\\s*\\|\\s*")) {
            pair = pair.trim();
            p = Pattern.compile("^\\s*([^:]+):([^:]+)\\s*$");
            m = p.matcher(pair);
            if (!m.find()) {
                sendPassengerMessage("Bad sign formatting: \"" + pair + "\".  See https://github.com/floored1585/SmartCart for help.");
                continue; // go to the next pair if it's not really a pair
            }
            String setting = m.group(1).trim();
            String value = m.group(2).trim();

            if(setting.equals("$SPD")) {
                p = Pattern.compile("^\\d*\\.?\\d+$");
                Double minSpeed = 0D;
                Double maxSpeed = SmartCart.config.getDouble("max_cart_speed");
                if(!p.matcher(value).find() || Double.parseDouble(value)>maxSpeed || Double.parseDouble(value)<minSpeed) {
                    sendPassengerMessage("Bad speed value: \"" + value + "\". Must be a numeric value (decimals OK) between "
                            + minSpeed + " and " + maxSpeed + ".");
                    continue;
                }
                configSpeed = Double.parseDouble(value);
            }
            else if(setting.equals("$MSG")) {
                sendPassengerMessage(value);
            }
            else if(setting.equals("$END")) {
                configEndpoint = value;
                sendPassengerMessage("Endpoint set to " + value);
            }
            else if(setting.equals(configEndpoint) || setting.equals("$DEF")) {
                // Skip this if we already found and used the endpoint
                Entity passenger = null;
                if(!cart.getPassengers().isEmpty()) {
                    passenger = cart.getPassengers().get(0);
                }
                if(foundEndpoint || passenger == null) {
                    continue;
                }
                foundEndpoint = true;
                Block blockAhead;
                Vector vector;
                switch(value){
                    case "N":
                        blockAhead = cart.getLocation().add(0D,0D,-1D).getBlock();
                        vector = new Vector(0,0,-1);
                        break;
                    case "S":
                        blockAhead = cart.getLocation().add(0D,0D,1D).getBlock();
                        vector = new Vector(0,0,1);
                        break;
                    case "E":
                        blockAhead = cart.getLocation().add(1D,0D,0D).getBlock();
                        vector = new Vector(1,0,0);
                        break;
                    default:
                        blockAhead = cart.getLocation().add(-1D,0D,0D).getBlock();
                        vector = new Vector(-1,0,0);
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


    private boolean isMoving() {
        Vector velocity = getCart().getVelocity();
        return (velocity.getX() != 0D || velocity.getZ() != 0D);
    }


    // Sets the speed to the max, in the direction the cart is already travelling
    public void setSpeed(double speed) {

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
    public void remove(boolean kill) {

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


    public void executeControl() {
        if (getCart().getPassengers().isEmpty()) {
            return;
        }

        Block block = getBlockBeneath();
        //plugin.getLogger().info(block.getState().getData().toString());
        if(block.getState().getData().toString().contains("WOOL")){
            if (block.getState().getData().toString().contains("ORANGE")) {
                setPreviousWoolColor(DyeColor.ORANGE);
                setSpeed(SmartCart.config.getDouble("slow_cart_speed"));
            }
            if (block.getState().getData().toString().contains("YELLOW")){
                // If the cart is near the center of the block, kill it.  Otherwise, slow it down.
                if (isLeavingBlock()) {
                    remove(true);
                } else {
                    setSpeed(0.1D);
                }
            }
            if(block.getState().getData().toString().contains("GREEN")) {
                //   If we have already executed this block and ARE moving, teleport the cart
                //   in the direction the player is facing.
                if (getPreviousWoolColor() == DyeColor.GREEN) {
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
                    setPreviousWoolColor(DyeColor.GREEN);
                    setSpeed(0D);
                    sendPassengerMessage("Move in the direction you wish to go.");
                } else {
                    // Otherwise, slow the cart down
                    setSpeed(0.1D);
                }
            }
            if(block.getState().getData().toString().contains("RED")){
                // If we're not half way through the block, return
                if ( !isLeavingBlock() ) {
                    setSpeed(0.1D);
                    return;
                }
                // If we just executed the elevator, return
                if ( getPreviousWoolColor() == DyeColor.RED) {
                    return;
                }

                setPreviousWoolColor(DyeColor.RED);

                // Get the tp target, destroy old cart, spawn new cart at tp target,
                //   tp passenger, load passenger into new cart
                Block elevator = SmartCart.util.getElevatorBlock(block.getLocation());
                if (elevator == null) {
                    return;
                }
                Block tpTarget = elevator.getLocation().add(0,1,0).getBlock();
                Entity passenger = getCart().getPassengers().get(0);
                Vector cartVelocity = getCart().getVelocity();

                // Set the new passenger location
                Location passengerLoc = passenger.getLocation();
                passengerLoc.setY( tpTarget.getLocation().getBlockY() );
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
                newCart.setPreviousWoolColor(DyeColor.RED);
                transferSettings(newCart);
            }
        }
    }


    public String getPassengerName() {
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

    private void sendPassengerMessage(String message) {
        message = "§6[SmartCart] " + message;
        Entity entity = getPassenger();
        if (entity instanceof Player) {
            ((Player) entity).sendRawMessage(message);
        }
    }

    private boolean isCommandMinecart() {
        return getCart() instanceof CommandMinecart;
    }

    private boolean isExplosiveMinecart() {
        return  getCart() instanceof ExplosiveMinecart;
    }

    private boolean isHopperMinecart() {
        return getCart() instanceof HopperMinecart;
    }

    private boolean isPoweredMinecart() {
        return getCart() instanceof PoweredMinecart;
    }

    //public boolean isRideableMinecart() {
    //    return getCart() instanceof RideableMinecart;
    //}

    private boolean isSpawnerMinecart() {
        return  getCart() instanceof SpawnerMinecart;
    }

    private boolean isStorageMinecart() {
        return getCart() instanceof StorageMinecart;
    }
}
