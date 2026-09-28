package noppes.mpm;

import java.io.File;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import noppes.mpm.client.ClientProxy;
import noppes.mpm.config.ConfigLoader;
import noppes.mpm.config.ConfigProp;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.packets.Packets;
import noppes.mpm.util.PixelmonHelper;

@Mod("moreplayermodels")
public class MorePlayerModels {
   public static final String MODID = "moreplayermodels";
   public static final String VERSION = "1.20";
   @ConfigProp
   public static int Tooltips = 2;
   public static CommonProxy proxy = (CommonProxy)DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
   public static MorePlayerModels instance;
   public static int Version = 8;
   public static File dir;
   public static File skinCache;
   public static boolean HasServerSide = false;
   @ConfigProp(
      info = "Enable different perspective heights for different model sizes"
   )
   public static boolean EnablePOV = true;
   @ConfigProp(
      info = "Enables the item on your back"
   )
   public static boolean EnableBackItem = true;
   @ConfigProp(
      info = "Enables chat bubbles"
   )
   public static boolean EnableChatBubbles = true;
   @ConfigProp(
      info = "Enables MorePlayerModels startup update message"
   )
   public static boolean EnableUpdateChecker = true;
   @ConfigProp(
      info = "Set to false if you dont want to see player particles"
   )
   public static boolean EnableParticles = true;
   @ConfigProp(
      info = "Set to true if you dont want to see hide player names"
   )
   public static boolean HidePlayerNames = false;
   @ConfigProp(
      info = "Set to true if you dont want to see hide selection boxes when pointing to blocks"
   )
   public static boolean HideSelectionBox = false;
   @ConfigProp(
      info = "Set to true if you want no flying animation"
   )
   public static boolean DisableFlyingAnimation = false;
   @ConfigProp(
      info = "Type 0 = Normal, Type 1 = Solid"
   )
   public static int HeadWearType = 1;
   @ConfigProp(
      info = "Minimum scaling size, default 0.5. This only changes it for you, other wont see smaller than their min size"
   )
   public static float ScaleSizeMin = 0.2F;
   @ConfigProp(
      info = "Maximum scaling size, default 1.5. This only changes it for you, other wont see larger than their max size"
   )
   public static float ScaleSizeMax = 2.0F;
   @ConfigProp(
      info = "Disables scaling and animations for more compatibilty with other mods"
   )
   public static boolean Compatibility = false;
   @ConfigProp(
      info = "On competitive servers like hipixel you dont want people going around in invisible skins"
   )
   public static boolean AllowFullyInvisibleSkins = false;
   @ConfigProp(
      info = "Used to register buttons to animations"
   )
   public static int button1 = EnumAnimation.SLEEP.ordinal();
   @ConfigProp(
      info = "Used to register buttons to animations"
   )
   public static int button2 = EnumAnimation.SIT.ordinal();
   @ConfigProp(
      info = "Used to register buttons to animations"
   )
   public static int button3 = EnumAnimation.CRAWL.ordinal();
   @ConfigProp(
      info = "Used to register buttons to animations"
   )
   public static int button4 = EnumAnimation.HUG.ordinal();
   @ConfigProp(
      info = "Used to register buttons to animations"
   )
   public static int button5 = EnumAnimation.DANCE.ordinal();
   public ConfigLoader configLoader;
   public static Key<BooleanValue> ALLOW_ENTITY_MODELS = create("mpmAllowEntityModels", true);

   public MorePlayerModels() {
      instance = this;
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
      this.configLoader = new ConfigLoader(this.getClass(), new File(dir, "config"), "MorePlayerModels");
      this.configLoader.loadConfig();
   }

   private void setupClient(FMLLoadCompleteEvent event) {
      proxy.postLoad();
   }

   private void setup(FMLCommonSetupEvent event) {
      LogWriter.info("Loading");
      Packets.register();
      if (ModList.get().isLoaded("Morph")) {
         EnablePOV = false;
      }

      PixelmonHelper.load();
      proxy.load();
      MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
      MinecraftForge.EVENT_BUS.register(new ServerTickHandler());
   }

   private static Key<BooleanValue> create(String key, boolean val) {
      Type<BooleanValue> type = BooleanValue.m_46250_(val);
      return GameRules.m_46189_(key, Category.MISC, type);
   }

   static {
      File dir = new File(FMLPaths.CONFIGDIR.get().toFile(), "..");
      MorePlayerModels.dir = new File(dir, "moreplayermodels");
      if (!MorePlayerModels.dir.exists()) {
         MorePlayerModels.dir.mkdir();
      }

      skinCache = new File(MorePlayerModels.dir, "skincache");
      if (!skinCache.exists()) {
         skinCache.mkdir();
      }
   }
}
