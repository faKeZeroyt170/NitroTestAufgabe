package net.nitromc.api.model;

import java.util.Map;

public record PlayerProfile(
        String uuid,
        String name,
        String skinUrl,
        Map<String, Long> names
) {

    public PlayerProfile {
        names = names == null ? Map.of() : Map.copyOf(names);
    }

    public String getProfileUrl() {
        return "https://namemc.com/profile/" + uuid;
    }
}