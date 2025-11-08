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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SwitchyCommands extends CommandBase {
    @Override
    public String getName() {
        return "switchy";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/switchy [list|switch <profile>|view <profile>|delete <profile>]";
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
            case "components":
                listComponents(player, data);
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
            return getListOfStringsMatchingLastWord(args, "list", "switch", "view", "delete", "components");
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("switch") || subcommand.equals("view") || subcommand.equals("delete")) {
                return getListOfStringsMatchingLastWord(args, data.keySet());
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
    }

    public static TextComponentString prefix() {
        TextComponentString prefix = new TextComponentString("[Switchy] ");
        prefix.getStyle().setColor(TextFormatting.BLUE);
        return prefix;
    }
}
