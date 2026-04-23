
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class LookAtExpansion extends PlaceholderExpansion {
    
    @Override
    public String getAuthor() {
        return "AllF1RE";
    }    
    @Override
    public String getIdentifier() {
        return "lookat";
    }    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    // Gets the player the viewer is looking at
    private Player getTargetPlayer(Player viewer) {
        double maxDistance = 10.0;      
        RayTraceResult result = viewer.getWorld().rayTraceEntities(
            viewer.getEyeLocation(),
            viewer.getLocation().getDirection(),
            maxDistance,
            entity -> entity instanceof Player && entity != viewer);
        if (result != null && result.getHitEntity() instanceof Player) {
            return (Player) result.getHitEntity();
        }    
        return null;
    }
    
    // Processing see placeholder with custom format
    private String handleSeePlaceholder(Player viewer, String params) {
        // Remove "see_"
        String formatParams = params.substring(4); 
        // Split by the last underscore to get two values
        String[] parts;
        if (formatParams.contains("_")) {
            // Search last _
            int lastUnderscore = formatParams.lastIndexOf('_');
            String falseValue = formatParams.substring(0, lastUnderscore);
            String trueValue = formatParams.substring(lastUnderscore + 1);     
            // Replace the remaining underscores back to falseValue
            falseValue = falseValue.replace('_', ' ');        
            parts = new String[]{falseValue, trueValue};
        } else {
            // If there is only one value, use it for true, 
            // and empty for false
            parts = new String[]{"", formatParams};
        }
        
        Player target = getTargetPlayer(viewer);
        if (target != null) {
            // If look at player - return second value
            return parts[1];
        } else {
            // If dont look at player - return first value :)
            return parts[0];
        }
    }
    
    @Override
    public String onRequest(OfflinePlayer p, String params) {
        // Check for online status player
        if (p == null || !p.isOnline()) {
            return "";
        }

        Player viewer = p.getPlayer();
        if (viewer == null) return "";       
        //  Placeholder processing see
        if (params.startsWith("see_")) {
            return handleSeePlaceholder(viewer, params);
        }
        
        // Processing direct requests
        if (params.startsWith("target_")) {
            Player target = getTargetPlayer(viewer);
            if (target == null) return "";    
            String dataType = params.substring(7); // remove "target_"        
            switch (dataType.toLowerCase()) {
                case "name"Placeholder processing:
                    return target.getName();
                case "uuid":
                    return target.getUniqueId().toString();
              case Processing direct requests "health":
                    return String.format("%.1f", target.getHealth());
                case "max_health":
                    return String.format("%.1f", target.getMaxHealth());
                case "level":
                    return String.valueOf(target.getLevel());
                case "gamemode":
                    return target.getGameMode().name();
                case "world":
                    return target.getWorld().getName();
                case "food":
                    return String.valueOf(target.getFoodLevel());
                case "xp":
                    return String.valueOf(target.getTotalExperience());
                default:
                    return "";
            }
        }
        
        // Processing arbitrary placeholders for purposes
        if (params.startsWith("parse_")) {
            Player target = getTargetPlayer(viewer);
            if (target == null) return "";
            String placeholder = params.substring(6); // remove "parse_"
            placeholder = "%" + placeholder + "%";
            return PlaceholderAPI.setPlaceholders(target, placeholder);
        }       
        return null;
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
}
