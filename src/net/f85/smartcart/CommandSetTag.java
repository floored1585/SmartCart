package net.f85.smartcart;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;

public class CommandSetTag implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        String help = "Usage: /scSetTag <Tag> -- sets tag of current cart to Tag";
        //if(!cmd.getName().equalsIgnoreCase("scsettag")) return false;
        if(!(sender instanceof Player)) {
            sender.sendMessage("Consoles can't be in vehicles");
            return true;
        }
        if(args.length < 1){
            ((Player) sender).sendRawMessage(help);
            return true;
        }
        if(((Player)sender).getVehicle() == null){
            ((Player) sender).sendRawMessage(help);
            return true;
        }
        if(!(((Player)sender).getVehicle() instanceof Minecart)){
            ((Player) sender).sendRawMessage("Non minecart vehicles aren't supported\n" + help);
            return true;
        }
        SmartCartVehicle cart = SmartCart.util.getCartFromList((Minecart)((Player)sender).getVehicle());
        cart.configEndpoint = args[0];
        ((Player) sender).sendRawMessage("§6[Smart Cart] §7Set tag to §a" + args[0]);
        return true;
    }
}
