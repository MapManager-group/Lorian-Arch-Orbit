package lorian.smoke;
import com.davidblackcn.lorianarchorbit.client.HueBlocksRuntime;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public final class LocalColorSmoke {
    private static List<String> original;
    private static Path pack;
    private static int baseline;
    private static boolean started;
    private static int ticks, readyTicks;
    public static void register() {
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(client -> {
            if (started) return;
            if (repository().data() != null && !repository().checking() && ++readyTicks > 60) {
                started = true; start(client);
            } else if (++ticks > 2400) { started = true; fail(client, new AssertionError("initial resources timed out")); }
        });
    }
    private static void start(Minecraft client) {
        try {
            require(repository().data() != null, "initial local data");
            require(repository().data().blocks().size() > 400, "vanilla coverage");
            baseline = rgb("minecraft:block/stone");
            original = List.copyOf(client.getResourcePackRepository().getSelectedIds());
            pack = client.getResourcePackDirectory().resolve("lorian-local-smoke");
            if (Files.exists(pack) && (!Files.isRegularFile(pack.resolve("pack.mcmeta"))
                    || !Files.readString(pack.resolve("pack.mcmeta")).contains("Disposable local color integration fixture")))
                throw new IllegalStateException("Test pack path already contains unrelated resources");
            write("pack.mcmeta", "{\"pack\":{\"description\":\"Disposable local color integration fixture\",\"min_format\":[88,0],\"max_format\":[97,1]}}");
            pixel("assets/minecraft/textures/block/stone.png", 0xFFFF0000);
            pixel("assets/lorian_test/textures/block/folder/new.png", 0xFF0000FF);
            write("assets/minecraft/blockstates/dirt.json", "{\"variants\":{\"\":{\"model\":\"lorian_test:block/new\",\"y\":90}}}");
            write("assets/lorian_test/models/block/new.json", "{\"textures\":{\"all\":\"lorian_test:block/folder/new\"},\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{\"north\":{\"texture\":\"#all\"}}}]}");
            client.getResourcePackRepository().reload();
            require(client.getResourcePackRepository().addPack("file/lorian-local-smoke"), "test pack discovered");
            client.reloadResourcePacks().thenRunAsync(() -> first(client), client).exceptionally(error -> fail(client,error));
        } catch (Throwable ex) { fail(client, ex); }
    }
    private static void first(Minecraft client) {
        try {
            require(rgb("minecraft:block/stone") == 0xFF0000, "active pack red override");
            require(rgb("lorian_test:block/folder/new") == 0x0000FF, "new namespaced texture");
            var faces = surfaces("minecraft:dirt");
            require(faces.size() == 1 && faces.getFirst().faces().equals(Set.of("east")), "model override and rotation");
            pixel("assets/minecraft/textures/block/stone.png", 0xFF00FF00);
            client.reloadResourcePacks().thenRunAsync(() -> second(client), client).exceptionally(error -> fail(client,error));
        } catch (Throwable ex) { fail(client,ex); }
    }
    private static void second(Minecraft client) {
        try {
            require(rgb("minecraft:block/stone") == 0x00FF00, "same pack name with changed bytes");
            client.getResourcePackRepository().setSelected(original);
            client.reloadResourcePacks().thenRunAsync(() -> {
                require(rgb("minecraft:block/stone") == baseline, "pack removal restores vanilla colors");
                require(repository().data().blocks().stream().noneMatch(b -> b.texture().startsWith("lorian_test:")), "removed resources leave candidates");
                System.out.println("LOCAL_COLOR_SMOKE PASS: initial scan, override, new namespace, model rotation, reload invalidation, pack removal");
                client.stop();
            },client).exceptionally(error -> fail(client,error));
        } catch (Throwable ex) { fail(client,ex); }
    }
    private static HueBlocksRuntime repository() {
        try { var method=HueBlocksRuntime.class.getDeclaredMethod("repository"); method.setAccessible(true); return (HueBlocksRuntime)method.invoke(null); }
        catch(ReflectiveOperationException error){throw new RuntimeException(error);}
    }
    @SuppressWarnings("unchecked")
    private static java.util.List<com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlockFaces.Surface> surfaces(String id) {
        try { var method=HueBlocksRuntime.class.getDeclaredMethod("surfaces",String.class); method.setAccessible(true); return (java.util.List<com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlockFaces.Surface>)method.invoke(null,id); }
        catch(ReflectiveOperationException error){throw new RuntimeException(error);}
    }
    private static int rgb(String id) { return repository().data().blocks().stream().filter(b -> b.texture().equals(id)).findFirst().orElseThrow().rgb(); }
    private static void require(boolean value, String label) { if (!value) throw new AssertionError(label); }
    private static void write(String path, String text) throws Exception { Path file=pack.resolve(path); Files.createDirectories(file.getParent()); Files.writeString(file,text); }
    private static void pixel(String path, int argb) throws Exception {
        Path file=pack.resolve(path); Files.createDirectories(file.getParent());
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)image.setRGB(x,y,argb);
        ImageIO.write(image,"png",file.toFile());
    }
    private static Void fail(Minecraft client, Throwable error) {
        System.out.println("LOCAL_COLOR_SMOKE FAIL"); error.printStackTrace();
        if(original!=null)client.getResourcePackRepository().setSelected(original);
        client.stop(); return null;
    }
}
