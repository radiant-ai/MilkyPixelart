package fun.milkyway.milkypixelart.managers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fun.milkyway.milkypixelart.MilkyPixelart;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LangManager {
    private static LangManager instance;
    private final LoadingCache<CacheQuery, Component> componentCache;
    private final LoadingCache<String, List<Component>> componentListCache;

    private LangManager() {
        componentCache = CacheBuilder.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
            @Override
            public @NotNull Component load(@NotNull CacheQuery query) {
                return getLangInternal(query);
            }
        });
        componentListCache = CacheBuilder.newBuilder()
                .maximumSize(50)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull List<Component> load(@NotNull String key) {
                        return getLangListInternal(key);
                    }
                });
    }

    public synchronized static LangManager getInstance() {
        if (instance == null) {
            instance = new LangManager();
        }
        return instance;
    }

    public synchronized static void reload() {
        instance = new LangManager();
    }

    public @NotNull Component getLang(@NotNull String key, @NotNull String... args) {
        var query = new CacheQuery(key, args);
        return componentCache.getUnchecked(query);
    }

    public @NotNull String getLangPlain(@NotNull String key, @NotNull String... args) {
        var query = new CacheQuery(key, args);
        return PlainTextComponentSerializer.plainText().serialize(componentCache.getUnchecked(query));
    }

    public @NotNull List<Component> getLangList(@NotNull String key) {
        return componentListCache.getUnchecked(key);
    }

    public @NotNull List<String> getLangPlainList(@NotNull String key) {
        return componentListCache.getUnchecked(key).stream().map(PlainTextComponentSerializer.plainText()::serialize)
                .collect(Collectors.toList());
    }

    private @NotNull Component getLangInternal(@NotNull CacheQuery query) {
        var key = query.getKey();
        var args = query.getArgs();

        String format = MilkyPixelart.getInstance().getLang().getString(key);

        if (format == null) {
            return Component.empty();
        }

        if (args != null && args.length > 0) {
            TagResolver[] placeholders = new TagResolver[args.length];

            for (int i = 0; i < args.length; i++) {
                placeholders[i] = TagResolver.resolver(Placeholder.parsed("arg"+(i+1), args[i]));
            }

            return MiniMessage.miniMessage().deserialize(format, placeholders);
        }
        else {
            return MiniMessage.miniMessage().deserialize(format);
        }
    }

    private List<Component> getLangListInternal(String key) {
        var format = MilkyPixelart.getInstance().getLang().getStringList(key);
        if (format.isEmpty()) {
            return Collections.emptyList();
        }
        return format.stream().map(MiniMessage.miniMessage()::deserialize).collect(Collectors.toList());
    }

    static class CacheQuery {
        private final String key;
        private final String[] args;

        public CacheQuery(@NotNull String key, @NotNull String[] args) {
            this.key = key;
            this.args = args;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheQuery cacheQuery = (CacheQuery) o;
            return key.equals(cacheQuery.key) &&
                    Arrays.equals(args, cacheQuery.args);
        }

        @Override
        public int hashCode() {
            return key.hashCode() * 31 + Arrays.hashCode(args);
        }

        public @NotNull String getKey() {
            return key;
        }

        public @NotNull String[] getArgs() {
            return args;
        }
    }
}
