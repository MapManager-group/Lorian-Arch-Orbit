package lorian.smoke;
import com.davidblackcn.lorianarchorbit.client.HueBlocksRuntime;
@net.neoforged.fml.common.Mod(value="lorian_local_smoke",dist=net.neoforged.api.distmarker.Dist.CLIENT) public class SmokeEntrypoint { public SmokeEntrypoint(){ LocalColorSmoke.register(); } }