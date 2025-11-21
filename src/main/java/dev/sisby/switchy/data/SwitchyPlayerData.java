package dev.sisby.switchy.data;

import com.google.common.collect.Sets;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.duck.SwitchyPlayer;
import dev.sisby.switchy.exception.NbtException;
import dev.sisby.switchy.exception.ProfileCurrentException;
import dev.sisby.switchy.exception.ProfileExistsException;
import dev.sisby.switchy.exception.ProfileMissingException;
import dev.sisby.switchy.exception.ProfilePreciousException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SwitchyPlayerData {
    private String current;
    private ITextComponent greeting;
    private final Set<SwitchyComponentType<?>> componentTypes;
    private final Map<String, SwitchyProfile> profiles;

    public SwitchyPlayerData(String current, ITextComponent greeting, Set<SwitchyComponentType<?>> componentTypes, Map<String, SwitchyProfile> profiles) {
        this.current = current;
        this.greeting = greeting;
        this.componentTypes = componentTypes;
        this.profiles = profiles;
    }

    public static SwitchyPlayerData of(EntityPlayerMP player) {
        return ((SwitchyPlayer) player).switchy$getOrCreatePlayerData();
    }

    public static SwitchyPlayerData ofEarly(EntityPlayerMP player) {
        return ((SwitchyPlayer) player).switchy$getPlayerData();
    }

    public static SwitchyPlayerData create(EntityPlayerMP player, NBTTagCompound nbt) {
        SwitchyPlayerData data = new SwitchyPlayerData(
            "default",
            null,
            new LinkedHashSet<>(),
            new LinkedHashMap<>()
        );
        data.profiles.put("default", new SwitchyProfile("default", SwitchyComponentMap.empty()));
        for (SwitchyComponentType<?> componentType : Sets.difference(SwitchyComponentTypes.instance().values(), data.componentTypes)) {
            data.initComponent(componentType, player, nbt);
        }
        if (nbt.hasKey("switchy:presets", 10)) {
            data.recoverLegacyData(player, nbt.getCompoundTag("switchy:presets"));
        }
        return data;
    }

    public boolean profileExists(String profileId) {
        return profiles.containsKey(profileId);
    }

    public SwitchyProfile getCurrentProfile(EntityPlayerMP player) throws NbtException {
        return getProfile(current(), player);
    }

    public Set<String> keySet() {
        return profiles.keySet();
    }

    public Set<SwitchyComponentType<?>> componentSet() {
        return componentTypes;
    }

    public Collection<SwitchyProfile> values() {
        return profiles.values();
    }

    public int size() {
        return profiles.size();
    }

    public String current() {
        return current;
    }

    public Optional<ITextComponent> greeting() {
        return Optional.ofNullable(greeting);
    }

    public ITextComponent greet() {
        ITextComponent defaultedGreeting;
        if (greeting != null) {
            defaultedGreeting = greeting;
        } else {
            TextComponentString prefix = new TextComponentString("[Switchy] ");
            prefix.getStyle().setColor(TextFormatting.BLUE);
            
            TextComponentString message = new TextComponentString("welcome back! current profile: ");
            message.getStyle().setColor(TextFormatting.GRAY);
            prefix.appendSibling(message);
            
            prefix.appendSibling(SwitchyComponentTypes.NAME.asText(profiles.get(current).getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)));
            
            TextComponentString suffix = new TextComponentString(". ");
            suffix.getStyle().setColor(TextFormatting.GRAY);
            prefix.appendSibling(suffix);
            
            TextComponentString listCmd = new TextComponentString("/switchy");
            listCmd.getStyle().setUnderlined(true);
            prefix.appendSibling(listCmd);
            
            defaultedGreeting = prefix;
        }
        greeting = null;
        return defaultedGreeting;
    }

    public SwitchyProfile getProfile(String profileId, EntityPlayerMP player) throws NbtException {
        if (profileId.equals(current)) {
            updateFromPlayer(profiles.get(profileId), player);
        }
        return profiles.get(profileId);
    }

    public int initComponents(Set<SwitchyComponentType<?>> types, EntityPlayerMP player) {
        NBTTagCompound compound = new NBTTagCompound();
        player.writeToNBT(compound);
        for (SwitchyComponentType<?> type : types) {
            if (initComponent(type, player, compound)) {
                types.add(type);
            } else {
                for (SwitchyComponentType<?> t : types) {
                    removeComponent(t);
                }
                return 0;
            }
        }
        return types.size();
    }

    public boolean initComponent(SwitchyComponentType<?> componentType, EntityPlayerMP player, NBTTagCompound nbt) {
        try {
            List<SwitchyComponentMap> maps = profiles.values().stream().map(SwitchyProfile::components).collect(Collectors.toList());
            componentType.tryInitialize(maps, nbt, player, player.getGameProfile().getName());
        } catch (Exception e) {
            Switchy.LOGGER.warn("Failed to initialize {} for {}", componentType.id(), player.getGameProfile().getName(), e);
            return false;
        }
        componentTypes.add(componentType);
        return true;
    }

    public int removeComponents(Set<SwitchyComponentType<?>> types) {
        for (SwitchyProfile p : profiles.values()) {
            if (!p.id().equals(current)) {
                for (SwitchyComponentType<?> t : types) {
                    if (t.isPrecious(p.components())) {
                        return 0;
                    }
                }
            }
        }
        for (SwitchyComponentType<?> type : types) {
            for (SwitchyProfile p : profiles.values()) {
                p.remove(type);
            }
            componentTypes.remove(type);
        }
        return types.size();
    }

    public boolean removeComponent(SwitchyComponentType<?> componentType) {
        for (SwitchyProfile p : profiles.values()) {
            if (!p.id().equals(current) && componentType.isPrecious(p.components())) {
                return false;
            }
        }
        for (SwitchyProfile profile : profiles.values()) {
            profile.remove(componentType);
        }
        componentTypes.remove(componentType);
        return true;
    }

    private static final Map<ResourceLocation, Pair<String, String>> LEGACY_RECOVERIES = new HashMap<>();
    
    static {
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.INVENTORY, new Pair<>("switchy_inventories:inventories", "inventory"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.ENDER_CHEST, new Pair<>("switchy_inventories:ender_chests", "inventory"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.LEVEL, new Pair<>("switchy_inventories:experience", "experienceLevel"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.XP, new Pair<>("switchy_inventories:experience", "experienceProgress"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.NAME_ID, new Pair<>("switchy:styled_nicknames", "styled_nickname"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.DIMENSION, new Pair<>("switchy_teleport:last_location", "last_location.dimension"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.YAW, new Pair<>("switchy_teleport:last_location", "last_location.yaw"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.PITCH, new Pair<>("switchy_teleport:last_location", "last_location.pitch"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.SPAWN_X, new Pair<>("switchy_teleport:spawn_point", "respawn_point.x"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.SPAWN_Y, new Pair<>("switchy_teleport:spawn_point", "respawn_point.y"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.SPAWN_Z, new Pair<>("switchy_teleport:spawn_point", "respawn_point.z"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.SPAWN_DIMENSION, new Pair<>("switchy_teleport:spawn_point", "respawn_point.dimension"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.SPAWN_ANGLE, new Pair<>("switchy_teleport:spawn_point", "respawn_point.dimension"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.SPAWN_FORCED, new Pair<>("switchy_teleport:spawn_point", "respawn_point.setSpawn"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.HEALTH, new Pair<>("switchy_status:health", "healthValue"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.EFFECTS, new Pair<>("switchy_status:status_effects", "status_effects"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.FOOD, new Pair<>("switchy_status:hunger", "foodLevel"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.SATURATION, new Pair<>("switchy_status:hunger", "foodSaturationLevel"));
        LEGACY_RECOVERIES.put(SwitchyComponentTypes.EXHAUSTION, new Pair<>("switchy_status:hunger", "exhaustion"));
    }

    public void recoverLegacyData(EntityPlayerMP player, NBTTagCompound legacyData) {
        Switchy.LOGGER.warn("[Switchy] Found legacy switchy profiles in {}, performing data recovery...", player.getGameProfile().getName());
        try {
            MinecraftServer server = player.getServerWorld().getMinecraftServer();
            File playerDataDir = new File(server.getWorld(0).getSaveHandler().getWorldDirectory(), "playerdata");
            File backupFile = new File(playerDataDir, player.getUniqueID().toString() + "-switchy.dat_old");
            
            NBTTagCompound backupNbt = new NBTTagCompound();
            backupNbt.setTag("backup", legacyData.copy());
            
            Switchy.LOGGER.info("[Switchy] Backed up legacy switchy data for {} to {}", player.getGameProfile().getName(), backupFile.getName());
        } catch (Exception e) {
            Switchy.LOGGER.error("[Switchy] Failed to save switchy data backup for {}!", player.getGameProfile().getName(), e);
            throw new RuntimeException("Failed to save switchy data backup!", e);
        }
        
        NBTTagCompound presets = legacyData.getCompoundTag("list");
        boolean containsDefault = false;
        int recovered = 0;
        Set<ResourceLocation> skippedTypeIds = new HashSet<>();
        
        for (String id : presets.getKeySet()) {
            SwitchyProfile profile = getOrCreateProfile(id.toLowerCase(), player);
            if (profile.id().equals("default")) containsDefault = true;
            
            try {
                NBTTagCompound modules = presets.getCompoundTag(id);
                
                for (Map.Entry<ResourceLocation, Pair<String, String>> entry : LEGACY_RECOVERIES.entrySet()) {
                    ResourceLocation typeId = entry.getKey();
                    SwitchyComponentType<?> type = SwitchyComponentTypes.instance().get(typeId);
                    if (type == null) continue;
                    
                    String moduleName = entry.getValue().getLeft();
                    String path = entry.getValue().getRight();
                    
                    NBTTagCompound moduleCompound = modules.getCompoundTag(moduleName);
                    if (moduleCompound.isEmpty()) continue;
                    
                    try {
                        NBTBase element = getNestedTag(moduleCompound, path);
                        if (element != null) {
                            Object value = type.fromNBT(element);
                            if (value != null) {
                                profile.components().set(type, value);
                                recovered++;
                            }
                        }
                    } catch (Exception e) {
                        skippedTypeIds.add(typeId);
                    }
                }
                
                NBTBase posTag = modules.getCompoundTag("switchy_teleport:last_location").getTag("last_location");
                if (posTag instanceof NBTTagCompound) {
                    NBTTagCompound compound = (NBTTagCompound) posTag;
                    if (!compound.isEmpty()) {
                        SwitchyComponentType<Vec3d> positionType = (SwitchyComponentType<Vec3d>) SwitchyComponentTypes.instance().get(SwitchyComponentTypes.POS);
                        if (positionType != null) {
                            Vec3d position = new Vec3d(compound.getFloat("x"), compound.getFloat("y"), compound.getFloat("z"));
                            profile.set(positionType, position);
                            recovered++;
                        }
                    }
                }
            } catch (Exception e) {
                Switchy.LOGGER.error("[Switchy] Failed to recover legacy precious data {} of {}", id, player.getGameProfile().getName(), e);
            }
        }
        
        current = legacyData.getString("current").toLowerCase();
        if (!containsDefault) profiles.remove("default");
        Switchy.LOGGER.info("[Switchy] Finished recovering {} components from {} legacy switchy profiles for {}. Skipped: {}", recovered, presets.getSize(), player.getGameProfile().getName(), skippedTypeIds);
    }

    private NBTBase getNestedTag(NBTTagCompound compound, String path) {
        String[] parts = path.split("\\.");
        NBTBase current = compound;
        
        for (String part : parts) {
            if (current instanceof NBTTagCompound) {
                current = ((NBTTagCompound) current).getTag(part);
                if (current == null) return null;
            } else {
                return null;
            }
        }
        
        return current;
    }

    public SwitchyProfile getOrCreateProfile(String profileId, EntityPlayerMP player) {
        if (profileExists(profileId)) return profiles.get(profileId);
        
        NBTTagCompound nbt = new NBTTagCompound();
        player.writeToNBT(nbt);
        SwitchyComponentMap components = SwitchyComponentMap.empty();
        
        for (SwitchyComponentType<?> componentType : componentTypes) {
            try {
                componentType.tryInitialize(Collections.singletonList(components), nbt, player, profileId);
            } catch (Exception e) {
                Switchy.LOGGER.warn("Failed to initialize {} for {} profile {}", componentType.id(), player.getGameProfile().getName(), profileId, e);
            }
        }
        
        SwitchyProfile newProfile = new SwitchyProfile(profileId, components);
        profiles.put(profileId, newProfile);
        return newProfile;
    }

    public void validate(EntityPlayerMP self, NBTTagCompound nbt) {
        Set<ResourceLocation> groupsChecked = new HashSet<>();
        Set<SwitchyComponentType<?>> typesToCheck = new HashSet<>(componentTypes);
        
        for (SwitchyComponentType<?> type : typesToCheck) {
            ResourceLocation group = type.group();
            if (group != null && !groupsChecked.contains(group)) {
                groupsChecked.add(group);
                for (SwitchyComponentType<?> otherType : SwitchyComponentTypes.instance().values()) {
                    if (group.equals(otherType.group()) && !componentTypes.contains(otherType)) {
                        Switchy.LOGGER.info("[Switchy] Enabling component {} of partially enabled group {} for user {}", otherType.id(), group, self.getGameProfile().getName());
                        initComponent(otherType, self, nbt);
                    }
                }
            }
        }
    }

    private NBTTagCompound updateFromPlayer(SwitchyProfile profile, EntityPlayerMP player) throws NbtException {
        NBTTagCompound nbt = new NBTTagCompound();
        player.writeToNBT(nbt);
        
        for (SwitchyComponentType<?> componentType : componentTypes) {
            if (componentType.nbtReader() != null) {
                Object value = componentType.nbtReader().read(nbt);
                profile.components().set(componentType, value);
            } else if (componentType.playerReader() != null) {
                Object value = componentType.playerReader().read(player, profile.id());
                profile.components().set(componentType, value);
            }
        }
        
        return nbt;
    }

    public void renameProfile(String oldId, String newId) throws IllegalArgumentException {
        if (!profileExists(oldId)) throw new ProfileMissingException(oldId);
        if (profileExists(newId)) throw new ProfileExistsException(newId);
        profiles.put(newId, profiles.remove(oldId).withId(newId));
        if (current.equals(oldId)) current = newId;
    }

    public SwitchyProfile deleteProfile(String profileId) {
        if (current.equals(profileId)) throw new ProfileCurrentException(profileId);
        if (!profileExists(profileId)) throw new ProfileMissingException(profileId);
        SwitchyProfile profile = profiles.get(profileId);
        Set<SwitchyComponentType<?>> preciousComponents = profile.components().keySet().stream()
            .filter(t -> t.isPrecious(profile.components()))
            .collect(Collectors.toSet());
        if (!preciousComponents.isEmpty()) throw new ProfilePreciousException(preciousComponents, profile.components());
        profiles.remove(profileId);
        return profile;
    }

    private void switchProfile(SwitchyProfile nextProfile, EntityPlayerMP player, ITextComponent greeting) throws NbtException {
        SwitchyProfile currentProfile = profiles.get(current);
        boolean selfSwitch = currentProfile == nextProfile;
        
        NBTTagCompound playerNbt;
        if (selfSwitch) {
            playerNbt = new NBTTagCompound();
            player.writeToNBT(playerNbt);
        } else {
            playerNbt = updateFromPlayer(currentProfile, player);
        }
        
        Switchy.LOGGER.info("[Switchy] Applying {} components for profile {}", nextProfile.components().keySet().size(), nextProfile.id());
        for (SwitchyComponentType<?> componentType : nextProfile.components().keySet()) {
            if (componentType == SwitchyComponentTypes.INVENTORY) {
                Switchy.LOGGER.info("[Switchy] Before mutate inventory tag: {}", playerNbt.getTag("Inventory"));
                Switchy.LOGGER.info("[Switchy] Applying inventory value: {}", nextProfile.components().get(componentType));
            }
            componentType.tryMutate(nextProfile.components(), playerNbt, player);
            if (componentType == SwitchyComponentTypes.INVENTORY) {
                Switchy.LOGGER.info("[Switchy] After mutate inventory tag: {}", playerNbt.getTag("Inventory"));
            }
        }

        this.greeting = greeting;
        current = nextProfile.id();

        TextComponentString prefix = new TextComponentString("[Switchy] ");
        prefix.getStyle().setColor(TextFormatting.BLUE);
        
        TextComponentString message = new TextComponentString(selfSwitch ? "Updated current profile " : "Switching to ");
        message.getStyle().setColor(TextFormatting.GRAY);
        prefix.appendSibling(message);
        
        prefix.appendSibling(SwitchyComponentTypes.NAME.asText(nextProfile.getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)));
        
        TextComponentString suffix = new TextComponentString("! Please reconnect.");
        suffix.getStyle().setColor(TextFormatting.GRAY);
        prefix.appendSibling(suffix);
        
        ((SwitchyPlayer) player).switchy$hotSwap(playerNbt, prefix);
    }

    public static SwitchyPlayerData fromNbt(NBTTagCompound playerNbt) {
        if (SwitchyComponentTypes.instance() == null) {
            throw new IllegalStateException("Can't load switchy data while the types aren't loaded!");
        }
        
        NBTTagCompound switchyNbt = playerNbt.getCompoundTag(Switchy.ID);
        
        String current = switchyNbt.getString("current");
        ITextComponent greeting = null;
        if (switchyNbt.hasKey("greeting", 8)) {
            greeting = new TextComponentString(switchyNbt.getString("greeting"));
        }
        
        Set<SwitchyComponentType<?>> componentTypes = new LinkedHashSet<>();
        NBTTagList componentTypesList = switchyNbt.getTagList("componentTypes", 8);
        for (int i = 0; i < componentTypesList.tagCount(); i++) {
            String typeId = componentTypesList.getStringTagAt(i);
            SwitchyComponentType<?> type = SwitchyComponentTypes.instance().get(new ResourceLocation(typeId));
            if (type != null) {
                componentTypes.add(type);
            }
        }
        
        Map<String, SwitchyProfile> profiles = new LinkedHashMap<>();
        NBTTagCompound profilesNbt = switchyNbt.getCompoundTag("profiles");
        for (String profileId : profilesNbt.getKeySet()) {
            SwitchyProfile profile = SwitchyProfile.fromNBT(profilesNbt.getCompoundTag(profileId), SwitchyComponentTypes.instance());
            profiles.put(profileId, profile);
        }
        
        return new SwitchyPlayerData(current, greeting, componentTypes, profiles);
    }

    public void writeNbt(NBTTagCompound playerNbt) {
        if (SwitchyComponentTypes.instance() == null) {
            throw new IllegalStateException("Can't save switchy data while the types aren't loaded!");
        }
        
        if (size() > 1) {
            NBTTagCompound switchyNbt = new NBTTagCompound();
            
            switchyNbt.setString("current", current);
            
            if (greeting != null) {
                switchyNbt.setString("greeting", greeting.getUnformattedText());
            }
            
            NBTTagList componentTypesList = new NBTTagList();
            for (SwitchyComponentType<?> type : componentTypes) {
                ResourceLocation id = SwitchyComponentTypes.instance().id(type);
                if (id != null) {
                    componentTypesList.appendTag(new NBTTagString(id.toString()));
                }
            }
            switchyNbt.setTag("componentTypes", componentTypesList);
            
            NBTTagCompound profilesNbt = new NBTTagCompound();
            for (Map.Entry<String, SwitchyProfile> entry : profiles.entrySet()) {
                profilesNbt.setTag(entry.getKey(), entry.getValue().toNBT(SwitchyComponentTypes.instance()));
            }
            switchyNbt.setTag("profiles", profilesNbt);
            
            playerNbt.setTag(Switchy.ID, switchyNbt);
        }
    }

    public void switchOrCreateProfile(String profileId, EntityPlayerMP player, ITextComponent greeting) throws NbtException {
        SwitchyProfile nextProfile = getOrCreateProfile(profileId.toLowerCase(), player);
        if (nextProfile.id().equals(current)) throw new ProfileCurrentException(nextProfile.id());
        switchProfile(nextProfile, player, greeting);
    }

    public void selfSwitch(SwitchyProfile currentProfile, EntityPlayerMP player, ITextComponent greeting) throws NbtException {
        if (!currentProfile.id().equals(current)) throw new ProfileCurrentException(currentProfile.id());
        switchProfile(currentProfile, player, greeting);
    }
    
    private static class Pair<L, R> {
        private final L left;
        private final R right;
        
        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }
        
        public L getLeft() {
            return left;
        }
        
        public R getRight() {
            return right;
        }
    }
}
