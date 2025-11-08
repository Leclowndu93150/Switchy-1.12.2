package dev.sisby.switchy.data;

public interface SwitchyComponentHolder<T extends SwitchyComponentHolder<T>> {
    String id();
    
    SwitchyComponentMap components();
    
    default <V> V get(SwitchyComponentType<? extends V> type) {
        return components().get(type);
    }
    
    default <V> V getOrDefault(SwitchyComponentType<? extends V> type, V fallback) {
        return components().getOrDefault(type, fallback);
    }
    
    default <V> V getOrGetDefault(SwitchyComponentType<? extends V> type, java.util.function.Function<T, V> defaultProvider) {
        V value = get(type);
        return value != null ? value : defaultProvider.apply((T) this);
    }
    
    default <V> V set(SwitchyComponentType<? extends V> type, V value) {
        return components().set(type, value);
    }
    
    default <V> V remove(SwitchyComponentType<? extends V> type) {
        return components().remove(type);
    }
}
