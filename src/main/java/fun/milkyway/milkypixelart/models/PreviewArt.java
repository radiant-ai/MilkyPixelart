package fun.milkyway.milkypixelart.models;

import org.bukkit.entity.Entity;

import java.util.List;
import java.util.UUID;

public record PreviewArt(UUID playerUuid, List<Entity> frames) {}
