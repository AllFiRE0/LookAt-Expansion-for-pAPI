import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        return "1.3.1";
    }
    
    @Override
    public List<String> getPlaceholders() {
        List<String> placeholders = new ArrayList<>();
        
        placeholders.add("%lookat_see_\"false\"_\"true\"%");
        placeholders.add("%lookat_see_<false>_<true>%");
        placeholders.add("%lookat_see_[false]_[true]%");
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
    
    // Gets the player the viewer is looking at (thread-safe)
    private Player getTargetPlayer(Player viewer) {
        // If already on main thread, just do it directly
        if (Bukkit.isPrimaryThread()) {
            return getTargetPlayerDirect(viewer);
        }
        
        // If on async thread, schedule on main thread
        try {
            CompletableFuture<Player> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("PlaceholderAPI"),
                () -> future.complete(getTargetPlayerDirect(viewer))
            );
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
    
    // Direct ray trace (must be called from main thread)
    private Player getTargetPlayerDirect(Player viewer) {
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
    
    // Detects which bracket type is used
    private char[] detectBracketType(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') return new char[]{'"', '"'};
            if (c == '<') return new char[]{'<', '>'};
            if (c == '[') return new char[]{'[', ']'};
        }
        return new char[]{'"', '"'};
    }
    
    // Processes the "see" placeholder
    private String handleSeePlaceholder(Player viewer, String params) {
        String remaining = params.substring(4);
        
        char[] brackets = detectBracketType(remaining);
        char openBracket = brackets[0];
        char closeBracket = brackets[1];
        
        List<String> parts = extractBracketedStrings(remaining, openBracket, closeBracket);
        
        if (parts.size() == 0) {
            return "";
        }
        
        String falseValue = parts.get(0);
        String trueValue = parts.size() > 1 ? parts.get(1) : "";
        
        // Parse {} placeholders first (safe to do async)
        falseValue = parseInlinePlaceholders(viewer, falseValue);
        trueValue = parseInlinePlaceholders(viewer, trueValue);
        
        // Get target (thread-safe)
        Player target = getTargetPlayer(viewer);
        
        if (target != null) {
            return PlaceholderAPI.setPlaceholders(target, trueValue);
        } else {
            return PlaceholderAPI.setPlaceholders(viewer, falseValue);
        }
    }
    
    // Extracts strings enclosed in brackets
    private List<String> extractBracketedStrings(String input, char openBracket, char closeBracket) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideBrackets = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == openBracket) {
                if (!insideBrackets) {
                    insideBrackets = true;
                    current = new StringBuilder();
                } else if (openBracket == closeBracket) {
                    result.add(current.toString());
                    current = new StringBuilder();
                    insideBrackets = false;
                }
            } else if (c == closeBracket && insideBrackets) {
                result.add(current.toString());
                current = new StringBuilder();
                insideBrackets = false;
            } else if (insideBrackets) {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
    
    // Replaces {placeholder} with parsed value
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
            
            String dataType = params.substring(7);
            
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
        
        // Handle parsing of external placeholders
        if (params.startsWith("parse_")) {
            Player target = getTargetPlayer(viewer);
            if (target == null) return "";
            
            String placeholder = params.substring(6);
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
