package dev.sisby.switchy.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FormatUtils {
    @SuppressWarnings("deprecation")
    public static String prettify(String s) {
        return WordUtils.capitalize(s.replace("_", " "));
    }

    public static ITextComponent statText(float f) {
        return new TextComponentString(NumberFormat.getNumberInstance(Locale.ROOT).format(Math.ceil(f) / 2F));
    }

    public static ITextComponent inventoryText(NonNullList<ItemStack> inventory) {
        if (inventory == null || inventory.stream().allMatch(ItemStack::isEmpty)) {
            TextComponentString text = new TextComponentString("(empty)");
            text.getStyle().setColor(TextFormatting.GRAY);
            return text;
        }
        
        List<ITextComponent> contents = inventory.stream()
            .filter(i -> !i.isEmpty())
            .map(i -> {
                TextComponentString line = new TextComponentString("- ");
                line.getStyle().setColor(TextFormatting.GRAY);
                line.appendText(String.valueOf(i.getCount()));
                line.appendText("x ");
                line.appendSibling(i.getTextComponent());
                return (ITextComponent) line;
            })
            .collect(Collectors.toList());
        
        TextComponentString hover = new TextComponentString("Contents:\n");
        hover.getStyle().setColor(TextFormatting.GRAY);
        for (int i = 0; i < contents.size(); i++) {
            hover.appendSibling(contents.get(i));
            if (i < contents.size() - 1) {
                hover.appendText("\n");
            }
        }
        
        TextComponentString result = new TextComponentString(String.valueOf(inventory.stream().filter(i -> !i.isEmpty()).count()));
        result.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
        
        TextComponentString suffix = new TextComponentString(" stacks");
        suffix.getStyle().setColor(TextFormatting.GRAY);
        result.appendSibling(suffix);
        
        return result;
    }

    public static ITextComponent nbtText(NBTBase element) {
        if (element == null) {
            TextComponentString text = new TextComponentString("(null)");
            text.getStyle().setColor(TextFormatting.GRAY);
            return text;
        }
        
        if (element instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) element;
            TextComponentString text = new TextComponentString("x" + compound.getKeySet().size());
            text.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(compound.toString())));
            return text;
        } else if (element instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) element;
            TextComponentString text = new TextComponentString("x" + list.tagCount());
            text.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(list.toString())));
            return text;
        } else if (element instanceof NBTTagString) {
            String str = ((NBTTagString) element).getString();
            try {
                ResourceLocation id = new ResourceLocation(str);
                TextComponentString text = new TextComponentString(id.getPath());
                text.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(id.toString())));
                return text;
            } catch (Exception e) {
                return new TextComponentString(str);
            }
        }
        
        return new TextComponentString(element.toString());
    }

    public static boolean isEmpty(NBTBase element) {
        if (element == null) return true;
        if (element instanceof NBTTagCompound) {
            return ((NBTTagCompound) element).isEmpty();
        } else if (element instanceof NBTTagList) {
            return ((NBTTagList) element).tagCount() == 0;
        } else if (element instanceof NBTTagString) {
            String str = ((NBTTagString) element).getString();
            return str.isEmpty() || str.equals("minecraft:air");
        }
        return false;
    }
}
