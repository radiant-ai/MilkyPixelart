package fun.milkyway.milkypixelart.models;

import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;

public class Lang {
    private final List<Component> components;

    public Lang(List<Component> components) {
        this.components = Collections.unmodifiableList(components);
    }

    public List<Component> getComponents() {
        return components;
    }

    public Component get(int index) {
        return components.get(index);
    }
}
