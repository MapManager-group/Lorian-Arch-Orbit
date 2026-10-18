package lorian.smoke;
import com.davidblackcn.lorianarchorbit.client.HueBlocksRuntime;
public class SmokeEntrypoint implements net.fabricmc.api.ClientModInitializer { public void onInitializeClient(){ LocalColorSmoke.register(); } }