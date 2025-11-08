package dev.sisby.switchy.data;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.FormatUtils;
import dev.sisby.switchy.util.NBTSerializers;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
    private static SwitchyComponentTypes INSTANCE = null;
    private static final SwitchyComponentTypes STATIC = new SwitchyComponentTypes();

    public static final ResourceLocation NAME_ID = new ResourceLocation(Switchy.ID, "name");
    public static final ResourceLocation DIMENSION = new ResourceLocation("minecraft", "location/dimension");
    public static final ResourceLocation POS = new ResourceLocation("minecraft", "location/pos");
    public static final ResourceLocation YAW = new ResourceLocation("minecraft", "location/yaw");
    public static final ResourceLocation PITCH = new ResourceLocation("minecraft", "location/pitch");
    public static final ResourceLocation SPAWN_X = new ResourceLocation("minecraft", "spawn/x");
    public static final ResourceLocation SPAWN_Y = new ResourceLocation("minecraft", "spawn/y");
    public static final ResourceLocation SPAWN_Z = new ResourceLocation("minecraft", "spawn/z");
    public static final ResourceLocation SPAWN_FORCED = new ResourceLocation("minecraft", "spawn/forced");
    public static final ResourceLocation SPAWN_ANGLE = new ResourceLocation("minecraft", "spawn/angle");
    public static final ResourceLocation SPAWN_DIMENSION = new ResourceLocation("minecraft", "spawn/dimension");
    public static final ResourceLocation EFFECTS = new ResourceLocation("minecraft", "effects");
    public static final ResourceLocation HEALTH = new ResourceLocation("minecraft", "health");
    public static final ResourceLocation FOOD = new ResourceLocation("minecraft", "hunger/food");
    public static final ResourceLocation SATURATION = new ResourceLocation("minecraft", "hunger/saturation");
    public static final ResourceLocation EXHAUSTION = new ResourceLocation("minecraft", "hunger/exhaustion");
    public static final ResourceLocation XP = new ResourceLocation("minecraft", "xp/progress");
    public static final ResourceLocation LEVEL = new ResourceLocation("minecraft", "xp/level");
    public static final ResourceLocation INVENTORY = new ResourceLocation("minecraft", "inventory/inventory");
    public static final ResourceLocation ENDER_CHEST = new ResourceLocation("minecraft", "inventory/ender_chest");

    public static final SwitchyComponentType<String> NAME = registerStatic(NAME_ID, builder -> {
        return builder
            .nbtSerializer(NBTSerializers.STRING)
            .textProvider(s -> s != null ? new TextComponentString(s) : new TextComponentString(""));
    });

    public static <T> SwitchyComponentType<T> registerStatic(ResourceLocation id, Function<SwitchyComponentType.Builder<T>, SwitchyComponentType.Builder<T>> operations) {
        return STATIC.registerType(id, i -> operations.apply(SwitchyComponentType.<T>builder(i)).build());
    }

    public static void init() {
        Switchy.LOGGER.info("Initializing Switchy component types...");
        setInstance(STATIC);
        
        registerStatic(DIMENSION, (SwitchyComponentType.Builder<Integer> builder) -> {
            return builder
                .nbtSwitcher("Dimension", NBTSerializers.INT)
                .textProvider(dim -> new TextComponentString(String.valueOf(dim)))
                .group(new ResourceLocation("minecraft", "location"))
                .previewPriority(10);
        });
        
        registerStatic(POS, (SwitchyComponentType.Builder<Vec3d> builder) -> {
            return builder
                .nbtSwitcher("Pos", NBTSerializers.VEC3D)
                .textProvider(pos -> new TextComponentString(String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)))
                .group(new ResourceLocation("minecraft", "location"))
                .previewPriority(9);
        });
        
        registerStatic(YAW, (SwitchyComponentType.Builder<Float> builder) -> {
            return builder
                .nbtSwitcher("yaw", NBTSerializers.FLOAT)
                .textProvider(yaw -> new TextComponentString(String.format("%.1f", yaw)))
                .group(new ResourceLocation("minecraft", "location"))
                .hidden(true);
        });
        
        registerStatic(PITCH, (SwitchyComponentType.Builder<Float> builder) -> {
            return builder
                .nbtSwitcher("pitch", NBTSerializers.FLOAT)
                .textProvider(pitch -> new TextComponentString(String.format("%.1f", pitch)))
                .group(new ResourceLocation("minecraft", "location"))
                .hidden(true);
        });
        
        registerStatic(HEALTH, (SwitchyComponentType.Builder<Float> builder) -> {
            return builder
                .nbtSwitcher("Health", NBTSerializers.FLOAT)
                .textProvider(FormatUtils::statText)
                .previewPriority(8);
        });
        
        registerStatic(FOOD, (SwitchyComponentType.Builder<Integer> builder) -> {
            return builder
                .nbtSwitcher("foodLevel", NBTSerializers.INT)
                .textProvider(food -> new TextComponentString(String.valueOf(food)))
                .group(new ResourceLocation("minecraft", "hunger"))
                .previewPriority(7);
        });
        
        registerStatic(SATURATION, (SwitchyComponentType.Builder<Float> builder) -> {
            return builder
                .nbtSwitcher("foodSaturationLevel", NBTSerializers.FLOAT)
                .textProvider(sat -> new TextComponentString(String.format("%.1f", sat)))
                .group(new ResourceLocation("minecraft", "hunger"))
                .hidden(true);
        });
        
        registerStatic(EXHAUSTION, (SwitchyComponentType.Builder<Float> builder) -> {
            return builder
                .nbtSwitcher("foodExhaustionLevel", NBTSerializers.FLOAT)
                .textProvider(exh -> new TextComponentString(String.format("%.1f", exh)))
                .group(new ResourceLocation("minecraft", "hunger"))
                .hidden(true);
        });
        
        registerStatic(XP, (SwitchyComponentType.Builder<Float> builder) -> {
            return builder
                .nbtSwitcher("XpP", NBTSerializers.FLOAT)
                .textProvider(xp -> new TextComponentString(String.format("%.1f%%", xp * 100)))
                .group(new ResourceLocation("minecraft", "xp"))
                .previewPriority(5);
        });
        
        registerStatic(LEVEL, (SwitchyComponentType.Builder<Integer> builder) -> {
            return builder
                .nbtSwitcher("XpLevel", NBTSerializers.INT)
                .textProvider(lvl -> new TextComponentString("Level " + lvl))
                .group(new ResourceLocation("minecraft", "xp"))
                .previewPriority(6);
        });
        
        registerStatic(INVENTORY, (SwitchyComponentType.Builder<NonNullList<ItemStack>> builder) -> {
            return builder
                .nbtSwitcher("Inventory", NBTSerializers.INVENTORY)
                .textProvider(FormatUtils::inventoryText)
                .emptyChecker(inv -> inv.stream().allMatch(ItemStack::isEmpty))
                .group(new ResourceLocation("minecraft", "inventory"))
                .previewPriority(10);
        });
        
        registerStatic(ENDER_CHEST, (SwitchyComponentType.Builder<NonNullList<ItemStack>> builder) -> {
            return builder
                .nbtSwitcher("EnderItems", NBTSerializers.INVENTORY)
                .textProvider(FormatUtils::inventoryText)
                .emptyChecker(inv -> inv.stream().allMatch(ItemStack::isEmpty))
                .group(new ResourceLocation("minecraft", "inventory"))
                .previewPriority(9);
        });
        
        registerStatic(EFFECTS, (SwitchyComponentType.Builder<NBTBase> builder) -> {
            return builder
                .nbtSwitcher("ActiveEffects", NBTSerializers.NBT)
                .textProvider(nbt -> {
                    if (nbt instanceof NBTTagList) {
                        NBTTagList list = (NBTTagList) nbt;
                        return new TextComponentString("x" + list.tagCount());
                    }
                    return FormatUtils.nbtText(nbt);
                })
                .emptyChecker(nbt -> {
                    if (nbt instanceof NBTTagList) {
                        return ((NBTTagList) nbt).tagCount() == 0;
                    }
                    return FormatUtils.isEmpty(nbt);
                })
                .previewPriority(4);
        });
        
        Switchy.LOGGER.info("Registered {} component types", STATIC.values().size());
    }

    public static void setInstance(SwitchyComponentTypes types) {
        INSTANCE = types;
    }

    public <T> SwitchyComponentType<T> registerComponent(ResourceLocation id, Function<SwitchyComponentType.Builder<T>, SwitchyComponentType.Builder<T>> operations) {
        return registerType(id, i -> operations.apply(SwitchyComponentType.<T>builder(i)).build());
    }

    @Nullable
    public static SwitchyComponentTypes instance() {
        return INSTANCE;
    }

    public static SwitchyComponentTypes getStatic() {
        return STATIC;
    }

    public static Map<ResourceLocation, List<SwitchyComponentType<?>>> grouped(Set<SwitchyComponentType<?>> keyset) {
        Comparator<ResourceLocation> prioritizedIdComparator = Comparator
            .comparing((Function<ResourceLocation, Boolean>) id -> !id.getNamespace().equals(Switchy.ID))
            .thenComparing(id -> !id.getNamespace().equals("minecraft"))
            .thenComparing(ResourceLocation::toString);
        
        Map<ResourceLocation, List<SwitchyComponentType<?>>> grouped = new TreeMap<>(prioritizedIdComparator);
        
        Comparator<SwitchyComponentType<?>> comparator = Comparator
            .comparing((Function<SwitchyComponentType<?>, Integer>) SwitchyComponentType::previewPriority, Comparator.reverseOrder())
            .thenComparing(t -> t.group() != null ? t.group() : t.id(), prioritizedIdComparator);
        
        List<SwitchyComponentType<?>> sorted = keyset.stream().sorted(comparator).collect(Collectors.toList());
        
        for (SwitchyComponentType<?> t : sorted) {
            ResourceLocation group = t.group();
            ResourceLocation key = group != null ? group : t.id();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
        
        return grouped;
    }
}
