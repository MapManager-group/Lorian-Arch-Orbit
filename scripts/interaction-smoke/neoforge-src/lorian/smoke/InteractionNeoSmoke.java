package lorian.smoke;
import com.davidblackcn.lorianarchorbit.client.*;
import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
@net.neoforged.fml.common.Mod(value="lorian_interaction_smoke",dist=net.neoforged.api.distmarker.Dist.CLIENT)
public final class InteractionNeoSmoke {
    private int ticks;
    private boolean ran;
    public InteractionNeoSmoke() {
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(c -> {
            if (ran) return;
            if (++ticks == 100 && c.level == null) c.createWorldOpenFlows().openWorld("lorian-interaction-smoke",c::stop);
            if (ticks > 2400) {ran=true;System.out.println("INTERACTION_SMOKE FAIL: world timeout");c.stop();return;}
            if (c.player == null || c.gui.screen()!=null || ticks < 120) return;
            ran = true;
            try { test(c); System.out.println("INTERACTION_SMOKE PASS: NeoForge editor"); }
            catch(Throwable e) { System.out.println("INTERACTION_SMOKE FAIL");e.printStackTrace(); }
            finally { c.stop(); }
        });
    }
    private static void test(Minecraft c) throws Exception {
            var sessionConstructor = Class.forName("com.davidblackcn.lorianarchorbit.client.EditorSession").getDeclaredConstructor(net.minecraft.client.gui.screens.Screen.class);
            sessionConstructor.setAccessible(true);
            var session = sessionConstructor.newInstance((Object)null);
            var screen = (PaletteEditorScreen)call(session.getClass(), session, "wheel", new Class<?>[]{}); c.setScreenAndShow(screen);
            var bounds = call(PaletteEditorScreen.class, screen, "layout", new Class<?>[]{});
            require(!(boolean)call(bounds.getClass(),bounds,"compact",new Class<?>[]{}), "wide editor fixture");
            click(screen, number(bounds,"groupLeft")+6, number(bounds,"groupTop")+6, InputConstants.MOUSE_BUTTON_LEFT);
            require(call(PaletteEditorScreen.class, screen, "selectedRef", new Class<?>[]{}) != null, "left group selection");
            var before = (PaletteGroup)call(PaletteEditorScreen.class, screen, "selected", new Class<?>[]{});
            int count = before.members().size();
            click(screen, number(bounds,"memberLeft")+6, number(bounds,"memberTop")+6, InputConstants.MOUSE_BUTTON_LEFT);
            require(selected(screen).members().size()==count, "left click must not delete");
            call(PaletteEditorScreen.class, screen, "updateDragFeedback", new Class<?>[]{bounds.getClass(),int.class,int.class}, bounds,number(bounds,"memberLeft")+6,number(bounds,"memberTop")+24);
            screen.mouseReleased(event(screen,number(bounds,"memberLeft")+6,number(bounds,"memberTop")+24,InputConstants.MOUSE_BUTTON_LEFT));
            require(selected(screen).members().get(1).equals(before.members().getFirst()), "drag reorder");
            click(screen,number(bounds,"memberLeft")+6,number(bounds,"memberTop")+6,InputConstants.MOUSE_BUTTON_RIGHT);
            require(selected(screen).members().size()==count-1,"right click deletes exactly one member");
            click(screen,number(bounds,"memberLeft")+6,number(bounds,"memberTop")+6,InputConstants.MOUSE_BUTTON_MIDDLE);
            require(selected(screen).members().size()==count-1,"middle click does not delete");
            c.setScreenAndShow(null); // Discard the in-memory draft; do not save user configuration.
    }
    private static int number(Object value,String name) throws Exception { return (int)call(value.getClass(),value,name,new Class<?>[]{}); }
    private static PaletteGroup selected(PaletteEditorScreen s) throws Exception { return (PaletteGroup)call(PaletteEditorScreen.class,s,"selected",new Class<?>[]{}); }
    private static void click(PaletteEditorScreen s,int x,int y,int button) throws Exception { s.mouseClicked(event(s,x,y,button),false); }
    private static MouseButtonEvent event(PaletteEditorScreen s,int x,int y,int button) throws Exception {
        var field=AdaptivePaletteScreen.class.getDeclaredField("viewport");field.setAccessible(true);
        var v=(PaletteViewport)field.get(s);
        return new MouseButtonEvent(v.guiX(x),v.guiY(y),new MouseButtonInfo(button,0));
    }
    private static Object call(Class<?> type,Object owner,String name,Class<?>[] params,Object... args) throws Exception {
        var method=type.getDeclaredMethod(name,params); method.setAccessible(true); return method.invoke(owner,args);
    }
    private static void require(boolean value,String label) { if (!value) throw new AssertionError(label); System.out.println("INTERACTION_CHECK " + label); }
}
