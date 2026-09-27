package dev.jouzepe.p2pmarket.social;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SocialMarketStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;

    public SocialMarketStorage(Path file) {
        this.file = file;
    }

    public SocialMarketState load() throws IOException {
        if (!Files.exists(file)) return new SocialMarketState();
        String json = Files.readString(file, StandardCharsets.UTF_8);
        SocialMarketState state = GSON.fromJson(json, SocialMarketState.class);
        if (state == null) state = new SocialMarketState();
        state.normalize();
        return state;
    }

    public void save(SocialMarketState state) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(state), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Path path() {
        return file;
    }
}
