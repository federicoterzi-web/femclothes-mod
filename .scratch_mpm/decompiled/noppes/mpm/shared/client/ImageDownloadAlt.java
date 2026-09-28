package noppes.mpm.shared.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.util.NoppesStringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ImageDownloadAlt extends SimpleTexture {
   private static final Logger logger = LogManager.getLogger();
   public final File cacheFile;
   private final String imageUrl;
   private boolean fix64;
   private Runnable r;
   public final ResourceLocation location;
   public boolean uploaded = false;

   public ImageDownloadAlt(File file, String url, ResourceLocation location, ResourceLocation defaultLocation, boolean fix64, Runnable r) {
      super(defaultLocation);
      this.location = location;
      this.cacheFile = file;
      this.imageUrl = url;
      this.fix64 = fix64;
      this.r = r;
   }

   public void setImage(NativeImage image) {
      Minecraft.m_91087_().execute(() -> {
         this.uploaded = true;
         if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> this.upload(image));
         } else {
            this.upload(image);
         }

         this.r.run();
      });
   }

   private void upload(NativeImage imageIn) {
      TextureUtil.prepareImage(this.m_117963_(), imageIn.m_84982_(), imageIn.m_85084_());
      imageIn.m_85040_(0, 0, 0, true);
   }

   public void m_6704_(ResourceManager resourceManager) throws IOException {
      if (this.cacheFile != null && this.cacheFile.isFile()) {
         logger.debug("Loading http texture from local cache ({})", new Object[]{this.cacheFile});
         NativeImage image = null;

         try {
            image = NativeImage.m_85058_(new FileInputStream(this.cacheFile));
            this.setImage(this.parseUserSkin(image));
            return;
         } catch (IOException var5) {
            super.m_6704_(resourceManager);
            logger.error("Couldn't load skin " + this.cacheFile, var5);
         }
      }

      if (!this.uploaded) {
         try {
            this.uploaded = true;
            super.m_6704_(resourceManager);
         } catch (Exception var4) {
         }
      }
   }

   public void loadTextureFromServer() {
      this.load(this.imageUrl, false);
   }

   private void load(String url, boolean wasRedirect) {
      HttpURLConnection connection = null;
      logger.debug("Downloading http texture from {} to {}", new Object[]{url, this.cacheFile});

      try {
         connection = (HttpURLConnection)new URL(url).openConnection();
         connection.setDoInput(true);
         connection.setDoOutput(false);
         connection.setInstanceFollowRedirects(false);
         connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
         connection.setRequestProperty("Content-Type", "image/png");
         connection.setRequestProperty("Accept", "image/png");
         connection.setRequestProperty("Expect", "100-continue");
         connection.connect();
         String type = connection.getContentType();
         long size = connection.getContentLengthLong();
         int statusCode = connection.getResponseCode();
         if (!wasRedirect && (statusCode == 302 || statusCode == 301 || statusCode == 303)) {
            String newUrl = connection.getHeaderField("Location");
            if (!NoppesStringUtils.empty(newUrl)) {
               this.load(newUrl, true);
            }

            return;
         }

         if (statusCode / 100 == 2 && type.equals("image/png") && (size <= 2000000L || Minecraft.m_91087_().m_91091_())) {
            FileUtils.copyInputStreamToFile(connection.getInputStream(), this.cacheFile);
            return;
         }
      } catch (Exception var12) {
         logger.error("Couldn't download http texture", var12);
         return;
      } finally {
         if (connection != null) {
            connection.disconnect();
         }
      }
   }

   public NativeImage parseUserSkin(NativeImage image) {
      if (image.m_85084_() != image.m_84982_() && image.m_84982_() / 2 != image.m_85084_()) {
         throw new IllegalArgumentException("Invalid texture size: " + image.m_84982_() + "x" + image.m_85084_());
      } else {
         int scale = image.m_84982_() / 64;
         boolean lvt_2_1_ = image.m_85084_() != image.m_84982_();
         if (lvt_2_1_ && this.fix64) {
            NativeImage nativeImage = new NativeImage(64 * scale, 64 * scale, true);
            nativeImage.m_85054_(image);
            image.close();
            image = nativeImage;
            nativeImage.m_84997_(0, 32 * scale, 64 * scale, 32 * scale, 0);
            nativeImage.m_85025_(4 * scale, 16 * scale, 16 * scale, 32 * scale, 4 * scale, 4 * scale, true, false);
            nativeImage.m_85025_(8 * scale, 16 * scale, 16 * scale, 32 * scale, 4 * scale, 4 * scale, true, false);
            nativeImage.m_85025_(0, 20 * scale, 24 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
            nativeImage.m_85025_(4 * scale, 20 * scale, 16 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
            nativeImage.m_85025_(8 * scale, 20 * scale, 8 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
            nativeImage.m_85025_(12 * scale, 20 * scale, 16 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
            nativeImage.m_85025_(44 * scale, 16 * scale, -8 * scale, 32 * scale, 4 * scale, 4 * scale, true, false);
            nativeImage.m_85025_(48 * scale, 16 * scale, -8 * scale, 32 * scale, 4 * scale, 4 * scale, true, false);
            nativeImage.m_85025_(40 * scale, 20 * scale, 0, 32 * scale, 4 * scale, 12 * scale, true, false);
            nativeImage.m_85025_(44 * scale, 20 * scale, -8 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
            nativeImage.m_85025_(48 * scale, 20 * scale, -16 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
            nativeImage.m_85025_(52 * scale, 20 * scale, -8 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
         }

         if (!MorePlayerModels.AllowFullyInvisibleSkins) {
            setAreaOpaque(image, 0, 0, 32 * scale, 16 * scale);
         }

         if (lvt_2_1_ && this.fix64) {
            setAreaTransparent(image, 32 * scale, 0, 64 * scale, 32 * scale);
         }

         return image;
      }
   }

   private static void setAreaTransparent(NativeImage image, int x, int y, int width, int height) {
      for (int i = x; i < width; i++) {
         for (int j = y; j < height; j++) {
            int k = image.m_84985_(i, j);
            if ((k >> 24 & 0xFF) < 128) {
               return;
            }
         }
      }

      for (int l = x; l < width; l++) {
         for (int i1 = y; i1 < height; i1++) {
            image.m_84988_(l, i1, image.m_84985_(l, i1) & 16777215);
         }
      }
   }

   private static void setAreaOpaque(NativeImage image, int x, int y, int width, int height) {
      for (int i = x; i < width; i++) {
         for (int j = y; j < height; j++) {
            image.m_84988_(i, j, image.m_84985_(i, j) | 0xFF000000);
         }
      }
   }
}
