package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.davidblackcn.lorianarchorbit.config.NioAtomicFileWriter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One background worker owns disk/network I/O; readers see only complete immutable snapshots. */
public final class HueBlocksRepository implements AutoCloseable {
    public static final URI SOURCE = URI.create("https://1280px.github.io/hueblocks/blocksets/");
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final System.Logger LOGGER = System.getLogger("lorian_arch_orbit.hueblocks");
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "lorian-hueblocks-download");
        thread.setDaemon(true);
        return thread;
    });
    private final Path cache;
    private final String version;
    private final URI source;
    private JsonObject cached;
    private boolean loadedCache;
    private volatile HueBlocksData data;
    private volatile State state = State.EMPTY;
    private volatile boolean checking;
    private boolean closed;

    public enum State { EMPTY, READY, CACHED, FAILED, UNSUPPORTED }

    public HueBlocksRepository(Path directory, String version) {
        this(directory, version, SOURCE);
    }

    HueBlocksRepository(Path directory, String version, URI source) {
        if (!version.matches("[0-9A-Za-z._-]{1,40}")) throw new IllegalArgumentException("Invalid game version");
        this.cache = directory.resolve("hueblocks").resolve(version + ".json");
        this.version = version;
        this.source = source;
    }

    public HueBlocksData data() { return data; }
    public State state() { return state; }
    public boolean checking() { return checking; }

    public synchronized void refresh() {
        if (closed || checking) return;
        checking = true;
        worker.execute(() -> {
            try {
                loadCache();
                JsonArray index = JsonParser.parseString(download(source.resolve("_blocksets.json"), "").body()).getAsJsonArray();
                if (index.size() > 100) throw new IOException("Version index too large");
                String directory = null;
                for (var entry : index) {
                    if (!entry.isJsonObject()) continue;
                    JsonObject object = entry.getAsJsonObject();
                    if (("Minecraft " + version).equals(object.get("name").getAsString())) {
                        directory = object.get("dir").getAsString();
                        break;
                    }
                }
                if (directory == null) {
                    state = State.UNSUPPORTED;
                    return;
                }
                if (!directory.matches("Minecraft [0-9A-Za-z ._-]{1,64}")) throw new IOException("Unsafe data directory");
                URI base = source.resolve(URLEncoder.encode(directory, StandardCharsets.UTF_8).replace("+", "%20") + "/");
                // Fetch and validate both files before replacing the combined cache. A partial update never wins.
                JsonObject replacement = new JsonObject();
                replacement.addProperty("version", version);
                for (String kind : new String[]{"blocks", "palettes"}) {
                    String file = kind.equals("blocks") ? "_blockdata.json" : "_palettes.json";
                    String etag = cached == null ? "" : cached.get(kind + "_etag").getAsString();
                    Download download = download(base.resolve(file), etag);
                    if (download.body() == null) {
                        if (cached == null) throw new IOException("304 without cached data");
                        replacement.add(kind, cached.get(kind).deepCopy());
                    } else {
                        replacement.add(kind, JsonParser.parseString(download.body()).getAsJsonArray());
                    }
                    replacement.addProperty(kind + "_etag", download.etag());
                }
                HueBlocksData parsed = decode(replacement);
                new NioAtomicFileWriter(LOGGER).write(cache, replacement.toString().getBytes(StandardCharsets.UTF_8));
                cached = replacement;
                data = parsed;
                state = State.READY;
            } catch (Exception exception) {
                state = data == null ? State.FAILED : State.CACHED;
                LOGGER.log(System.Logger.Level.WARNING, "HueBlocks update failed; keeping the last valid dataset", exception);
            } finally {
                checking = false;
            }
        });
    }

    private void loadCache() {
        if (loadedCache) return;
        loadedCache = true;
        if (!Files.exists(cache)) return;
        try {
            if (Files.size(cache) > MAX_BYTES * 2L) throw new IOException("Cache too large");
            JsonObject document = JsonParser.parseString(Files.readString(cache)).getAsJsonObject();
            HueBlocksData parsed = decode(document);
            document.get("blocks_etag").getAsString();
            document.get("palettes_etag").getAsString();
            cached = document;
            data = parsed;
            state = State.CACHED;
        } catch (Exception exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Ignoring invalid HueBlocks cache", exception);
        }
    }

    private HueBlocksData decode(JsonObject object) {
        if (!version.equals(object.get("version").getAsString())) throw new IllegalArgumentException("Cache version mismatch");
        return HueBlocksData.decode(version, object.getAsJsonArray("blocks"), object.getAsJsonArray("palettes"));
    }

    private static Download download(URI uri, String etag) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Lorian-Arch-Orbit/HueBlocks");
        if (!etag.isBlank() && etag.length() <= 512) connection.setRequestProperty("If-None-Match", etag);
        try {
            int status = connection.getResponseCode();
            String nextEtag = connection.getHeaderField("ETag");
            nextEtag = nextEtag == null || nextEtag.length() > 512 ? "" : nextEtag;
            if (status == 304) return new Download(null, nextEtag.isBlank() ? etag : nextEtag);
            if (status != 200) throw new IOException("HueBlocks HTTP " + status);
            if (connection.getContentLengthLong() > MAX_BYTES) throw new IOException("Response too large");
            try (InputStream stream = connection.getInputStream()) {
                byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw new IOException("Response too large");
                return new Download(new String(bytes, StandardCharsets.UTF_8), nextEtag);
            }
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        worker.shutdownNow();
    }

    private record Download(String body, String etag) {}
}
