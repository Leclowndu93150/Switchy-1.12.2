package dev.sisby.switchy;

import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.data.SwitchyProfile;
import dev.sisby.switchy.exception.NbtException;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SwitchyCommands extends CommandBase {
    @Override
    public String getName() {
        return "switchy";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/switchy [list|switch <profile>|view <profile>|delete <profile>|rename <profile> <new_id>|components [enable|disable] <id>]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("This command can only be used by players");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        SwitchyPlayerData data = SwitchyPlayerData.of(player);

        if (args.length == 0) {
            listProfiles(player, data);
            return;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "list":
                listProfiles(player, data);
                break;
            case "switch":
                if (args.length < 2) {
                    throw new CommandException("Usage: /switchy switch <profile>");
                }
                switchProfile(player, data, args[1].toLowerCase());
                break;
            case "view":
                if (args.length < 2) {
                    throw new CommandException("Usage: /switchy view <profile>");
                }
                viewProfile(player, data, args[1].toLowerCase());
                break;
            case "delete":
                if (args.length < 2) {
                    throw new CommandException("Usage: /switchy delete <profile>");
                }
                deleteProfile(player, data, args[1].toLowerCase());
                break;
            case "rename":
                if (args.length < 3) {
                    throw new CommandException("Usage: /switchy rename <profile> <new_id>");
                }
                renameProfile(player, data, args[1].toLowerCase(), args[2].toLowerCase());
                break;
            case "components":
                handleComponentsCommand(player, data, args);
                break;
            default:
                player.sendMessage(new TextComponentString(getUsage(sender)));
                break;
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!(sender instanceof EntityPlayerMP)) {
            return Collections.emptyList();
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        SwitchyPlayerData data = SwitchyPlayerData.ofEarly(player);
        
        if (data == null) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "list", "switch", "view", "delete", "rename", "components");
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("switch") || subcommand.equals("view") || subcommand.equals("delete")) {
                return getListOfStringsMatchingLastWord(args, data.keySet());
            } else if (subcommand.equals("rename")) {
                return getListOfStringsMatchingLastWord(args, data.keySet());
            } else if (subcommand.equals("components")) {
                return getListOfStringsMatchingLastWord(args, "enable", "disable");
            }
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("components")) {
                boolean disable = "disable".equalsIgnoreCase(args[1]);
                boolean enable = "enable".equalsIgnoreCase(args[1]);
                if (enable || disable) {
                    return getListOfStringsMatchingLastWord(args, getComponentSuggestions(data, disable));
                }
            } else if (subcommand.equals("rename")) {
                return getListOfStringsMatchingLastWord(args, Collections.singletonList("new_id"));
            }
        }

        return Collections.emptyList();
    }

    private void listProfiles(EntityPlayerMP player, SwitchyPlayerData data) {
        TextComponentString header = new TextComponentString("=== Switchy Profiles ===");
        header.getStyle().setColor(TextFormatting.BLUE).setBold(true);
        player.sendMessage(header);

        for (String profileId : data.keySet()) {
            TextComponentString line = new TextComponentString("");
            
            if (profileId.equals(data.current())) {
                TextComponentString current = new TextComponentString("▶ ");
                current.getStyle().setColor(TextFormatting.GREEN);
                line.appendSibling(current);
            } else {
                line.appendText("  ");
            }

            TextComponentString name = new TextComponentString(profileId);
            name.getStyle().setColor(TextFormatting.YELLOW);
            line.appendSibling(name);

            player.sendMessage(line);
        }

        TextComponentString footer = new TextComponentString("Use /switchy switch <profile> to switch");
        footer.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
        player.sendMessage(footer);
    }

    private void switchProfile(EntityPlayerMP player, SwitchyPlayerData data, String profileId) throws CommandException {
        try {
            TextComponentString prefix = new TextComponentString("[Switchy] ");
            prefix.getStyle().setColor(TextFormatting.BLUE);
            
            TextComponentString message = new TextComponentString("Switched to profile ");
            message.getStyle().setColor(TextFormatting.GRAY);
            prefix.appendSibling(message);
            
            TextComponentString profileName = new TextComponentString(profileId);
            profileName.getStyle().setColor(TextFormatting.YELLOW);
            prefix.appendSibling(profileName);

            data.switchOrCreateProfile(profileId, player, prefix);
        } catch (NbtException e) {
            throw new CommandException("Failed to switch profile: " + e.getMessage());
        } catch (Exception e) {
            throw new CommandException(e.getMessage());
        }
    }

    private void viewProfile(EntityPlayerMP player, SwitchyPlayerData data, String profileId) throws CommandException {
        if (!data.profileExists(profileId)) {
            throw new CommandException("Profile '" + profileId + "' doesn't exist!");
        }

        try {
            SwitchyProfile profile = data.getProfile(profileId, player);
            
            TextComponentString header = new TextComponentString("=== Profile: " + profileId + " ===");
            header.getStyle().setColor(TextFormatting.BLUE).setBold(true);
            player.sendMessage(header);

            for (ITextComponent line : profile.asTexts(player)) {
                player.sendMessage(line);
            }
        } catch (NbtException e) {
            throw new CommandException("Failed to view profile: " + e.getMessage());
        }
    }

    private void deleteProfile(EntityPlayerMP player, SwitchyPlayerData data, String profileId) throws CommandException {
        try {
            data.deleteProfile(profileId);
            
            TextComponentString message = new TextComponentString("Deleted profile: ");
            message.getStyle().setColor(TextFormatting.GRAY);
            
            TextComponentString profileName = new TextComponentString(profileId);
            profileName.getStyle().setColor(TextFormatting.RED);
            message.appendSibling(profileName);
            
            player.sendMessage(message);
        } catch (Exception e) {
            throw new CommandException(e.getMessage());
        }
    }

    private void renameProfile(EntityPlayerMP player, SwitchyPlayerData data, String oldId, String newId) throws CommandException {
        try {
            data.renameProfile(oldId, newId);

            TextComponentString message = prefix();
            TextComponentString body = new TextComponentString("Renamed profile ");
            body.getStyle().setColor(TextFormatting.GRAY);
            message.appendSibling(body);

            TextComponentString oldName = new TextComponentString(oldId);
            oldName.getStyle().setColor(TextFormatting.YELLOW);
            message.appendSibling(oldName);

            TextComponentString separator = new TextComponentString(" to ");
            separator.getStyle().setColor(TextFormatting.GRAY);
            message.appendSibling(separator);

            TextComponentString newName = new TextComponentString(newId);
            newName.getStyle().setColor(TextFormatting.GREEN);
            message.appendSibling(newName);

            player.sendMessage(message);
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage());
        }
    }

    private void listComponents(EntityPlayerMP player, SwitchyPlayerData data) {
        TextComponentString header = new TextComponentString("=== Enabled Components ===");
        header.getStyle().setColor(TextFormatting.BLUE).setBold(true);
        player.sendMessage(header);

        for (SwitchyComponentType<?> type : data.componentSet()) {
            TextComponentString line = new TextComponentString("• ");
            line.getStyle().setColor(TextFormatting.GRAY);
            
            TextComponentString componentName = new TextComponentString(type.id().toString());
            componentName.getStyle().setColor(TextFormatting.YELLOW);
            line.appendSibling(componentName);
            
            player.sendMessage(line);
        }

        TextComponentString tip = new TextComponentString("Use /switchy components enable|disable <id>");
        tip.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
        player.sendMessage(tip);
    }

    public static TextComponentString prefix() {
        TextComponentString prefix = new TextComponentString("[Switchy] ");
        prefix.getStyle().setColor(TextFormatting.BLUE);
        return prefix;
    }

    private void handleComponentsCommand(EntityPlayerMP player, SwitchyPlayerData data, String[] args) throws CommandException {
        if (args.length == 1) {
            listComponents(player, data);
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("enable") || action.equals("disable")) {
            if (args.length < 3) {
                throw new CommandException("Usage: /switchy components " + action + " <id>");
            }
            ResourceLocation id = parseId(args[2]);
            if (action.equals("enable")) {
                enableComponents(player, data, id);
            } else {
                disableComponents(player, data, id);
            }
        } else {
            listComponents(player, data);
        }
    }

    private void enableComponents(EntityPlayerMP player, SwitchyPlayerData data, ResourceLocation id) throws CommandException {
        Set<SwitchyComponentType<?>> targets = resolveComponentTargets(id);
        if (targets.isEmpty()) {
            throw new CommandException("Component or group '" + id + "' not found.");
        }

        Set<SwitchyComponentType<?>> pending = targets.stream()
            .filter(t -> !data.componentSet().contains(t))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (pending.isEmpty()) {
            throw new CommandException("Component(s) already enabled.");
        }

        int added = data.initComponents(new LinkedHashSet<>(pending), player);
        if (added <= 0) {
            throw new CommandException("Failed to enable component(s). Check logs for details.");
        }

        TextComponentString message = prefix();
        TextComponentString enabledText = new TextComponentString("Enabled ");
        enabledText.getStyle().setColor(TextFormatting.GRAY);
        message.appendSibling(enabledText);

        TextComponentString count = new TextComponentString(String.valueOf(added));
        count.getStyle().setColor(TextFormatting.GREEN);
        message.appendSibling(count);

        TextComponentString suffix = new TextComponentString(" component(s) for ");
        suffix.getStyle().setColor(TextFormatting.GRAY);
        message.appendSibling(suffix);

        TextComponentString idText = new TextComponentString(id.toString());
        idText.getStyle().setColor(TextFormatting.YELLOW);
        message.appendSibling(idText);
        player.sendMessage(message);
    }

    private void disableComponents(EntityPlayerMP player, SwitchyPlayerData data, ResourceLocation id) throws CommandException {
        Set<SwitchyComponentType<?>> targets = resolveComponentTargets(id);
        if (targets.isEmpty()) {
            throw new CommandException("Component or group '" + id + "' not found.");
        }

        Set<SwitchyComponentType<?>> enabled = targets.stream()
            .filter(t -> data.componentSet().contains(t))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (enabled.isEmpty()) {
            throw new CommandException("Component(s) already disabled.");
        }

        int removed = data.removeComponents(enabled);
        if (removed <= 0) {
            throw new CommandException("Unable to disable component(s). One or more profiles may still contain precious data.");
        }

        TextComponentString message = prefix();
        TextComponentString disabledText = new TextComponentString("Disabled ");
        disabledText.getStyle().setColor(TextFormatting.GRAY);
        message.appendSibling(disabledText);

        TextComponentString count = new TextComponentString(String.valueOf(removed));
        count.getStyle().setColor(TextFormatting.RED);
        message.appendSibling(count);

        TextComponentString suffix = new TextComponentString(" component(s) for ");
        suffix.getStyle().setColor(TextFormatting.GRAY);
        message.appendSibling(suffix);

        TextComponentString idText = new TextComponentString(id.toString());
        idText.getStyle().setColor(TextFormatting.YELLOW);
        message.appendSibling(idText);
        player.sendMessage(message);
    }

    private Set<SwitchyComponentType<?>> resolveComponentTargets(ResourceLocation id) {
        SwitchyComponentTypes types = SwitchyComponentTypes.instance();
        if (types == null) {
            return Collections.emptySet();
        }

        Set<SwitchyComponentType<?>> groupMatches = types.values().stream()
            .filter(t -> id.equals(t.group()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!groupMatches.isEmpty()) {
            return groupMatches;
        }

        SwitchyComponentType<?> direct = types.get(id);
        if (direct != null) {
            Set<SwitchyComponentType<?>> single = new LinkedHashSet<>();
            single.add(direct);
            return single;
        }

        return Collections.emptySet();
    }

    private ResourceLocation parseId(String raw) throws CommandException {
        try {
            return new ResourceLocation(raw);
        } catch (Exception e) {
            throw new CommandException("Invalid resource location: " + raw);
        }
    }

    private List<String> getComponentSuggestions(SwitchyPlayerData data, boolean onlyEnabled) {
        SwitchyComponentTypes types = SwitchyComponentTypes.instance();
        if (types == null) {
            return Collections.emptyList();
        }

        Set<String> suggestions = new LinkedHashSet<>();
        for (SwitchyComponentType<?> type : types.values()) {
            boolean enabled = data.componentSet().contains(type);
            if (enabled == onlyEnabled) {
                if (type.group() != null) {
                    suggestions.add(type.group().toString());
                }
                suggestions.add(types.id(type).toString());
            }
        }

        return new ArrayList<>(suggestions);
    }
}
