package dev.sisby.switchy.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

public abstract class TypeRegistry<T extends TypeRegistry.Type> {
    private final BiMap<ResourceLocation, T> map = HashBiMap.create();

    public boolean contains(ResourceLocation id) {
        return map.containsKey(id);
    }

    public boolean contains(T id) {
        return map.containsValue(id);
    }

    public T get(ResourceLocation id) {
        return map.get(id);
    }

    public ResourceLocation id(T type) {
        return map.inverse().get(type);
    }

    public Set<ResourceLocation> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public Set<T> values() {
        return Collections.unmodifiableSet(map.values());
    }

    public <B extends T> B registerType(ResourceLocation id, Function<ResourceLocation, B> typeSupplier) {
        B type = typeSupplier.apply(id);
        if (contains(id) || contains(type)) {
            throw new IllegalArgumentException(String.format("Type double-registration with ID: %s", id));
        }
        map.put(id, type);
        return type;
    }

    public interface Type {
        ResourceLocation id();
    }
}
