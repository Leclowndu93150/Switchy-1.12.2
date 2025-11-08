package dev.sisby.switchy.exception;

import dev.sisby.switchy.data.SwitchyComponentMap;
import dev.sisby.switchy.data.SwitchyComponentType;

import java.util.Set;

public class ProfilePreciousException extends IllegalArgumentException {
    private final SwitchyComponentMap preciousComponents;

    public ProfilePreciousException(Set<SwitchyComponentType<?>> preciousComponents, SwitchyComponentMap map) {
        super(String.format("profile contains %sx precious components!", preciousComponents.size()));
        this.preciousComponents = SwitchyComponentMap.empty();
        for (SwitchyComponentType<?> t : preciousComponents) {
            this.preciousComponents.set(t, map.get(t));
        }
    }

    public SwitchyComponentMap getPreciousComponents() {
        return preciousComponents;
    }
}
