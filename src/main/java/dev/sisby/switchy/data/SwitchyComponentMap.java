package dev.sisby.switchy.data;

import dev.sisby.switchy.util.FormatUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SwitchyComponentMap {
    private final Map<SwitchyComponentType<?>, Object> map;

    public static SwitchyComponentMap empty() {
        return create(new HashMap<>());
    }

    private static SwitchyComponentMap create(Map<SwitchyComponentType<?>, Object> components) {
        return new SwitchyComponentMap(new HashMap<>(components));
    }

    private SwitchyComponentMap(Map<SwitchyComponentType<?>, Object> map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(SwitchyComponentType<? extends T> type) {
        return (T) this.map.get(type);
    }

    public boolean contains(SwitchyComponentType<?> type) {
        return this.map.containsKey(type);
    }

    public <T> T getOrDefault(SwitchyComponentType<? extends T> type, T fallback) {
        T object = this.get(type);
        return object != null ? object : fallback;
    }

    public Set<SwitchyComponentType<?>> keySet() {
        return this.map.keySet();
    }

    public int size() {
        return this.map.size();
    }

    @Override
    public String toString() {
        return map.entrySet().stream()
            .map(e -> String.format("%s: %s", e.getKey().id().getPath(), Objects.toString(e.getValue())))
            .collect(Collectors.joining("\n"));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T set(SwitchyComponentType<? extends T> type, @Nullable T value) {
        return (T) this.map.put(type, value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T remove(SwitchyComponentType<? extends T> type) {
        return (T) this.map.remove(type);
    }

    public List<ITextComponent> asTexts() {
        Map<ResourceLocation, List<SwitchyComponentType<?>>> grouped = SwitchyComponentTypes.grouped(keySet());
        List<ITextComponent> result = new ArrayList<>();
        
        for (Map.Entry<ResourceLocation, List<SwitchyComponentType<?>>> entry : grouped.entrySet()) {
            List<SwitchyComponentType<?>> visibleTypes = entry.getValue().stream()
                .filter(t -> !t.hidden() && this.get(t) != null && (t.emptyChecker() == null || t.isPrecious(this)))
                .collect(Collectors.toList());
            
            if (visibleTypes.isEmpty()) continue;
            
            TextComponentString line = new TextComponentString(entry.getKey().getPath() + ": ");
            line.getStyle().setColor(TextFormatting.GRAY);
            
            for (int i = 0; i < visibleTypes.size(); i++) {
                SwitchyComponentType<?> t = visibleTypes.get(i);
                line.appendSibling(t.asText(this));
                if (i < visibleTypes.size() - 1) {
                    TextComponentString comma = new TextComponentString(", ");
                    comma.getStyle().setColor(TextFormatting.GRAY);
                    line.appendSibling(comma);
                }
            }
            
            result.add(line);
        }
        
        return result;
    }

    public NBTTagCompound toNBT(SwitchyComponentTypes types) {
        NBTTagCompound nbt = new NBTTagCompound();
        
        for (Map.Entry<SwitchyComponentType<?>, Object> entry : map.entrySet()) {
            SwitchyComponentType<?> type = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                ResourceLocation id = types.id(type);
                if (id != null) {
                    nbt.setTag(id.toString(), type.toNBT(value));
                }
            }
        }
        
        return nbt;
    }

    public static SwitchyComponentMap fromNBT(NBTTagCompound nbt, SwitchyComponentTypes types) {
        Map<SwitchyComponentType<?>, Object> components = new HashMap<>();
        
        for (String key : nbt.getKeySet()) {
            ResourceLocation id = new ResourceLocation(key);
            SwitchyComponentType<?> type = types.get(id);
            if (type != null) {
                Object value = type.fromNBT(nbt.getTag(key));
                if (value != null) {
                    components.put(type, value);
                }
            }
        }
        
        return create(components);
    }
}
