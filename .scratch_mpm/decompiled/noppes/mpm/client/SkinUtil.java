package noppes.mpm.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import noppes.mpm.ModelData;
import noppes.mpm.shared.client.ImageDownloadAlt;
import noppes.mpm.shared.client.ResourceDownloader;

public class SkinUtil {
   public static long lastSkinTick = -30L;
   public static final int MaxUrlTicks = 6;

   public static void loadPlayerResource(Player pl, ModelData data) {
      Minecraft minecraft = Minecraft.m_91087_();
      data.textureObject = null;
      AbstractClientPlayer player = (AbstractClientPlayer)pl;
      if (data.url != null && !data.url.isEmpty()) {
         if (!data.url.startsWith("http://") && !data.url.startsWith("https://")) {
            ResourceLocation location;
            try {
               location = new ResourceLocation(data.url);
               data.textureObject = minecraft.m_91097_().m_118506_(location);
            } catch (Exception var7) {
               location = DefaultPlayerSkin.m_118626_();
            }

            data.resourceLocation = location;
            data.resourceLoaded = true;
         } else {
            boolean noEntity = data.getEntity(pl) == null;
            ResourceLocation location = ResourceDownloader.getUrlResourceLocation(data.url, noEntity);
            File file = ResourceDownloader.getUrlFile(data.url, noEntity);
            if (file.exists()) {
               file.delete();
            }

            data.textureObject = loadTexture(data, file, location, DefaultPlayerSkin.m_118626_(), data.url, noEntity);
         }
      }
   }

   private static AbstractTexture loadTexture(ModelData data, File file, ResourceLocation resource, ResourceLocation def, String par1Str, boolean fix64) {
      TextureManager texturemanager = Minecraft.m_91087_().m_91097_();
      AbstractTexture object = texturemanager.m_174786_(resource, null);
      if (object == null) {
         data.resourceLoading = resource;
         ResourceDownloader.load(new ImageDownloadAlt(file, par1Str, resource, def, fix64, () -> {
            if (data.resourceLoading == resource) {
               data.resourceLoaded = true;
               data.resourceLocation = resource;
            }
         }));
      } else {
         data.resourceLoaded = true;
         data.resourceLocation = resource;
      }

      return object;
   }

   public static synchronized void load(ModelData data, Player player) {
      if (!data.resourceInit && lastSkinTick > 6L) {
         data.resourceInit = true;
         loadPlayerResource(player, data);
         lastSkinTick = 0L;
      }
   }

   public static void reloadSkins() {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91073_ != null) {
         for (AbstractClientPlayer player : mc.f_91073_.m_6907_()) {
            ModelData data = ModelData.get(player);
            data.resourceLoaded = false;
            data.resourceInit = false;
            data.webapiInit = false;
            data.webapiActive = false;
            if (data.resourceLocation != null) {
               mc.m_91097_().m_118513_(data.resourceLocation);
               data.resourceLocation = null;
            }
         }
      }
   }
}
