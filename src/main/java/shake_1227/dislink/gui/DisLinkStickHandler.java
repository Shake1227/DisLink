package shake_1227.dislink.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import shake_1227.dislink.DisLink;

public class DisLinkStickHandler implements Listener {
    private final DisLink plugin;
    public DisLinkStickHandler(DisLink plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && item.getType() == Material.STICK && hasCustomModelData(item, 1)) {
                player.openInventory(DisLinkGUI.getMainGUI(player));
                event.setCancelled(true);
            }
        }
    }
    private boolean hasCustomModelData(ItemStack item, int customModelData) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == customModelData;
    }
}
