import java.util.ArrayList;
import java.util.List;

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
        return "1.1.0";
    }
    
    @Override
    public List<String> getPlaceholders() {
        List<String> placeholders = new ArrayList<>();
        
        placeholders.add("%lookat_see_\"false_value\"_\"true_value\"%");
        placeholders.add("%lookat_target_name%");
        placeholders.add("%lookat_target_uuid%");
        placeholders.add("%lookat_target_health%");
        placeholders.add("%lookat_target_max_health%");
        placeholders.add("%lookat_target_level%");
        placeholders.add("%lookat_target_gamemode%");
        placeholders.add("%lookat_target_world%");
        placeholders.add("%lookat_target_food%");
        placeholders.add("%lookat_target_xp%");
        placeholders.add("%lookat_parse_{placeholder}%");
        
        return placeholders;
    }
    
    // Gets the player the viewer is looking at
    private Player getTargetPlayer(Player viewer) {
        double maxDistance = 10.0;
        
        RayTraceResult result = viewer.getWorld().rayTraceEntities(
            viewer.getEyeLocation(),
            viewer.getLocation().getDirection(),
            maxDistance,
            entity -> entity instanceof Player && entity != viewer
        );
        
        if (result != null && result.getHitEntity() instanceof Player) {
            return (Player) result.getHitEntity();
        }
        
        return null;
    }
    
    // Processes the "see" placeholder with quoted values
    private String handleSeePlaceholder(Player viewer, String params) {
        // Remove "see_" prefix
        String remaining = params.substring(4);
        
        // Extract values inside quotes
        List<String> parts = extractQuotedStrings(remaining);
        
        if (parts.size() == 0) {
            return "";
        }
        
        String falseValue = parts.get(0);
        String trueValue = parts.size() > 1 ? parts.get(1) : "";
        
        // Parse {} into real placeholders
        falseValue = parseInlinePlaceholders(viewer, falseValue);
        trueValue = parseInlinePlaceholders(viewer, trueValue);
        
        Player target = getTargetPlayer(viewer);
        
        if (target != null) {
            return PlaceholderAPI.setPlaceholders(target, trueValue);
        } else {
            return PlaceholderAPI.setPlaceholders(viewer, falseValue);
        }
    }
    
    // Extracts strings enclosed in double quotes
    private List<String> extractQuotedStrings(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == '"') {
                if (insideQuotes) {
                    // Closing quote — save the value
                    result.add(current.toString());
                    current = new StringBuilder();
                    insideQuotes = false;
                } else {
                    // Opening quote — start new value
                    insideQuotes = true;
                    current = new StringBuilder();
                }
            } else if (insideQuotes) {
                current.append(c);
            }
            // Everything outside quotes is ignored (separators like _)
        }
        
        // If a quote was never closed, add whatever we have
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
    
    // Replaces {placeholder} with the actual parsed placeholder value
    private String parseInlinePlaceholders(Player viewer, String text) {
        StringBuilder result = new StringBuilder();
        StringBuilder braceContent = new StringBuilder();
        boolean insideBraces = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '{') {
                insideBraces = true;
                braceContent = new StringBuilder();
            } else if (c == '}') {
                insideBraces = false;
                String placeholder = "%" + braceContent.toString() + "%";
                String parsed = PlaceholderAPI.setPlaceholders(viewer, placeholder);
                result.append(parsed);
            } else if (insideBraces) {
                braceContent.append(c);
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String onRequest(OfflinePlayer p, String params) {
        // Ensure the player is online
        if (p == null || !p.isOnline()) {
            return "";
        }
        
        Player viewer = p.getPlayer();
        if (viewer == null) return "";
        
        // Handle "see" placeholder
        if (params.startsWith("see_")) {
            return handleSeePlaceholder(viewer, params);
        }
        
        // Handle direct target data requests
        if (params.startsWith("target_")) {
            Player target = getTargetPlayer(viewer);
            if (target == null) return "";
            
            String dataType = params.substring(7); // Remove "target_"
            
            switch (dataType.toLowerCase()) {
                case "name":
                    return target.getName();
                case "uuid":
                    return target.getUniqueId().toString();
                case "health":
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
        
        // Handle parsing of external placeholders for the target
        if (params.startsWith("parse_")) {
            Player target = getTargetPlayer(viewer);
            if (target == null) return "";
            
            String placeholder = params.substring(6); // Remove "parse_"
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
