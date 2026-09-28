package noppes.mpm.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
   bus = Bus.MOD,
   modid = "moreplayermodels",
   value = {Dist.CLIENT}
)
public class MpmKeys {
   public static KeyMapping Screen;
   public static KeyMapping MPM1;
   public static KeyMapping MPM2;
   public static KeyMapping MPM3;
   public static KeyMapping MPM4;
   public static KeyMapping MPM5;
   public static KeyMapping Camera;

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void register(RegisterKeyMappingsEvent event) {
      event.register(Screen = new KeyMapping("CharacterScreen", 301, "key.categories.gameplay"));
      event.register(MPM1 = new KeyMapping("MPM 1", 90, "key.categories.gameplay"));
      event.register(MPM2 = new KeyMapping("MPM 2", -1, "key.categories.gameplay"));
      event.register(MPM3 = new KeyMapping("MPM 3", -1, "key.categories.gameplay"));
      event.register(MPM4 = new KeyMapping("MPM 4", -1, "key.categories.gameplay"));
      event.register(MPM5 = new KeyMapping("MPM 5", -1, "key.categories.gameplay"));
      event.register(Camera = new KeyMapping("MPM Camera", 341, "key.categories.gameplay"));
   }
}
