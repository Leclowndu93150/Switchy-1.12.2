package dev.sisby.switchy.data;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SwitchyProfile implements SwitchyComponentHolder<SwitchyProfile> {
    private final String id;
    private final SwitchyComponentMap components;

    public SwitchyProfile(String id, SwitchyComponentMap components) {
        this.id = id;
        this.components = components;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SwitchyComponentMap components() {
        return components;
    }

    @Override
    public String toString() {
        return id + "\n" + components.toString();
    }

    public Collection<ITextComponent> asTexts(EntityPlayerMP player) {
        List<ITextComponent> outList = new ArrayList<>();
        
        TextComponentString idLine = new TextComponentString("id: ");
        idLine.getStyle().setColor(TextFormatting.GRAY);
        idLine.appendText(id);
        outList.add(idLine);
        
        outList.addAll(components().asTexts());
        return outList;
    }

    public SwitchyProfile withId(String newId) {
        return new SwitchyProfile(newId, components);
    }

    public NBTTagCompound toNBT(SwitchyComponentTypes types) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("id", id);
        nbt.setTag("components", components.toNBT(types));
        return nbt;
    }

    public static SwitchyProfile fromNBT(NBTTagCompound nbt, SwitchyComponentTypes types) {
        String id = nbt.getString("id");
        SwitchyComponentMap components = SwitchyComponentMap.fromNBT(nbt.getCompoundTag("components"), types);
        return new SwitchyProfile(id, components);
    }
}
