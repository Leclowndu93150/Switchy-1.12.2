package dev.sisby.switchy.util;

import dev.sisby.switchy.data.SwitchyComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class NBTSerializers {
    public static final SwitchyComponentType.NBTSerializer<String> STRING = new SwitchyComponentType.NBTSerializer<String>() {
        @Override
        public NBTBase toNBT(String value) {
            return new NBTTagString(value);
        }

        @Override
        public String fromNBT(NBTBase nbt) {
            return ((NBTTagString) nbt).getString();
        }
    };

    public static final SwitchyComponentType.NBTSerializer<Integer> INT = new SwitchyComponentType.NBTSerializer<Integer>() {
        @Override
        public NBTBase toNBT(Integer value) {
            return new NBTTagInt(value);
        }

        @Override
        public Integer fromNBT(NBTBase nbt) {
            return ((NBTTagInt) nbt).getInt();
        }
    };

    public static final SwitchyComponentType.NBTSerializer<Float> FLOAT = new SwitchyComponentType.NBTSerializer<Float>() {
        @Override
        public NBTBase toNBT(Float value) {
            return new NBTTagFloat(value);
        }

        @Override
        public Float fromNBT(NBTBase nbt) {
            return ((NBTTagFloat) nbt).getFloat();
        }
    };

    public static final SwitchyComponentType.NBTSerializer<Boolean> BOOLEAN = new SwitchyComponentType.NBTSerializer<Boolean>() {
        @Override
        public NBTBase toNBT(Boolean value) {
            return new NBTTagByte((byte) (value ? 1 : 0));
        }

        @Override
        public Boolean fromNBT(NBTBase nbt) {
            return ((NBTTagByte) nbt).getByte() != 0;
        }
    };

    public static final SwitchyComponentType.NBTSerializer<Vec3d> VEC3D = new SwitchyComponentType.NBTSerializer<Vec3d>() {
        @Override
        public NBTBase toNBT(Vec3d value) {
            NBTTagList list = new NBTTagList();
            list.appendTag(new NBTTagDouble(value.x));
            list.appendTag(new NBTTagDouble(value.y));
            list.appendTag(new NBTTagDouble(value.z));
            return list;
        }

        @Override
        public Vec3d fromNBT(NBTBase nbt) {
            if (nbt instanceof NBTTagList) {
                NBTTagList list = (NBTTagList) nbt;
                double x = list.tagCount() > 0 ? list.getDoubleAt(0) : 0.0D;
                double y = list.tagCount() > 1 ? list.getDoubleAt(1) : 0.0D;
                double z = list.tagCount() > 2 ? list.getDoubleAt(2) : 0.0D;
                return new Vec3d(x, y, z);
            } else if (nbt instanceof NBTTagCompound) {
                NBTTagCompound compound = (NBTTagCompound) nbt;
                return new Vec3d(compound.getDouble("x"), compound.getDouble("y"), compound.getDouble("z"));
            }
            throw new ClassCastException("Expected list or compound for Vec3d, got " + nbt.getClass().getSimpleName());
        }
    };

    public static final SwitchyComponentType.NBTSerializer<ResourceLocation> RESOURCE_LOCATION = new SwitchyComponentType.NBTSerializer<ResourceLocation>() {
        @Override
        public NBTBase toNBT(ResourceLocation value) {
            return new NBTTagString(value.toString());
        }

        @Override
        public ResourceLocation fromNBT(NBTBase nbt) {
            return new ResourceLocation(((NBTTagString) nbt).getString());
        }
    };

    public static final SwitchyComponentType.NBTSerializer<NBTBase> NBT = new SwitchyComponentType.NBTSerializer<NBTBase>() {
        @Override
        public NBTBase toNBT(NBTBase value) {
            return value.copy();
        }

        @Override
        public NBTBase fromNBT(NBTBase nbt) {
            return nbt.copy();
        }
    };

    public static final SwitchyComponentType.NBTSerializer<NonNullList<ItemStack>> INVENTORY = new SwitchyComponentType.NBTSerializer<NonNullList<ItemStack>>() {
        @Override
        public NBTBase toNBT(NonNullList<ItemStack> value) {
            NBTTagList list = new NBTTagList();
            for (int i = 0; i < value.size(); i++) {
                ItemStack stack = value.get(i);
                if (!stack.isEmpty()) {
                    NBTTagCompound itemNBT = new NBTTagCompound();
                    itemNBT.setByte("Slot", (byte) i);
                    stack.writeToNBT(itemNBT);
                    list.appendTag(itemNBT);
                }
            }
            return list;
        }

        @Override
        public NonNullList<ItemStack> fromNBT(NBTBase nbt) {
            NBTTagList list = (NBTTagList) nbt;
            NonNullList<ItemStack> inventory = NonNullList.withSize(54, ItemStack.EMPTY);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound itemNBT = list.getCompoundTagAt(i);
                int slot = itemNBT.getByte("Slot") & 255;
                if (slot >= 0 && slot < inventory.size()) {
                    inventory.set(slot, new ItemStack(itemNBT));
                }
            }
            return inventory;
        }
    };
}
