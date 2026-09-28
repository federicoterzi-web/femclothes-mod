package noppes.mpm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.mpm.CommonProxy;
import noppes.mpm.ModelData;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.client.fx.EntityEnderFX;
import noppes.mpm.client.gui.select.GuiTextureSelection;
import noppes.mpm.client.layer.LayerBackItem;
import noppes.mpm.client.layer.LayerCapeMPM;
import noppes.mpm.client.layer.LayerElytraAlt;
import noppes.mpm.client.layer.LayerHeadwear;
import noppes.mpm.client.layer.LayerInterface;
import noppes.mpm.client.layer.LayerParts;
import noppes.mpm.client.model.animation.AnimationHandler;
import noppes.mpm.client.parts.MpmPartData;
import noppes.mpm.client.parts.MpmPartDataClient;
import noppes.mpm.client.parts.MpmPartReader;
import noppes.mpm.mixin.ArmorLayerMixin;
import noppes.mpm.mixin.LivingRenderer2Mixin;
import noppes.mpm.mixin.ParticleManagerMixin;
import noppes.mpm.shared.client.model.util.CustomRenderStates;

public class ClientProxy extends CommonProxy {
   public static ModelData data;
   public static PlayerModel playerModel;
   public static ArmorLayerMixin armorLayer;
   public static ArmorLayerMixin armorLayerSlim;

   @Override
   public void load() {
      this.createFolders();
      MinecraftForge.EVENT_BUS.register(new RenderEvent());
      MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
      new PresetController(MorePlayerModels.dir);
      AnimationHandler.initAnimations();
      if (MorePlayerModels.EnableUpdateChecker) {
         VersionChecker checker = new VersionChecker();
         checker.start();
      }

      ResourceManagerReloadListener listener = manager -> {
         EntityEnderFX.portalSprite = ((ParticleManagerMixin)Minecraft.m_91087_().f_91061_)
            .getPacks()
            .get(ForgeRegistries.PARTICLE_TYPES.getKey(ParticleTypes.f_123760_));
         MpmPartReader.reload();
         SkinUtil.reloadSkins();
         GuiTextureSelection.clear();
         RenderSystem.recordRenderCall(() -> {
            try {
               CustomRenderStates.posTexNormalShader = new ShaderInstance(manager, "moreplayermodels:position_tex_normal", CustomRenderStates.POS_TEX_NORMAL);
            } catch (IOException var2x) {
               var2x.printStackTrace();
            }
         });
      };
      ((ReloadableResourceManager)Minecraft.m_91087_().m_91098_()).m_7217_(listener);
      listener.m_6213_(Minecraft.m_91087_().m_91098_());
   }

   public static void fixModels() {
      Minecraft mc = Minecraft.m_91087_();
      EntityRenderDispatcher manager = mc.m_91290_();
      Map<String, EntityRenderer<? extends Player>> map = manager.getSkinMap();

      for (String type : map.keySet()) {
         EntityRenderer<? extends Player> render = map.get(type);
         addLayers((PlayerRenderer)render, type.equals("slim"));
      }
   }

   private static void addLayers(PlayerRenderer playerRender, boolean slim) {
      boolean hasMPMLayers = false;
      List<RenderLayer> list = ((LivingRenderer2Mixin)playerRender).getLayers();
      synchronized (list) {
         for (RenderLayer layer : new ArrayList<>(list)) {
            if (layer instanceof LayerInterface) {
               ((LayerInterface)layer).setBase((PlayerModel)playerRender.m_7200_());
               hasMPMLayers = true;
            }
         }

         if (slim) {
            armorLayerSlim = (ArmorLayerMixin)list.stream().filter(t -> t instanceof HumanoidArmorLayer).findAny().get();
         } else {
            armorLayer = (ArmorLayerMixin)list.stream().filter(t -> t instanceof HumanoidArmorLayer).findAny().get();
         }

         if (!hasMPMLayers) {
            list.removeIf(layerx -> layerx instanceof CapeLayer);
            list.removeIf(layerx -> layerx instanceof ElytraLayer);
            list.add(1, new LayerHeadwear(playerRender));
            list.add(new LayerCapeMPM(playerRender));
            list.add(new LayerBackItem(playerRender));
            list.add(new LayerElytraAlt(playerRender, Minecraft.m_91087_().m_167973_()));
            list.add(new LayerParts(playerRender));
         }
      }
   }

   public static void bindTexture2(ResourceLocation location) {
      if (location != null) {
         TextureManager manager = Minecraft.m_91087_().m_91097_();
         AbstractTexture textureObject = manager.m_118506_(location);
         if (textureObject == null) {
            textureObject = new SimpleTexture(location);
            manager.m_118495_(location, textureObject);
         }

         textureObject.m_117966_();
      }
   }

   @Override
   public void executor(Player player, Runnable runnable) {
      Minecraft.m_91087_().execute(runnable);
   }

   private void createFolders() {
      File file = new File(MorePlayerModels.dir, "assets/moreplayermodels");
      if (!file.exists()) {
         file.mkdirs();
      }

      File check = new File(file, "parts");
      if (!check.exists()) {
         check.mkdir();
      }

      check = new File(file, "textures");
      if (!check.exists()) {
         check.mkdir();
      }

      check = new File(file, "sounds");
      if (!check.exists()) {
         check.mkdir();
      }

      File json = new File(file, "sounds.json");
      if (!json.exists()) {
         try {
            json.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(json));
            writer.write("{\n\n}");
            writer.close();
         } catch (IOException var7) {
         }
      }

      File meta = new File(MorePlayerModels.dir, "pack.mcmeta");
      if (!meta.exists()) {
         try {
            meta.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(meta));
            writer.write("{\n    \"pack\": {\n        \"description\": \"moreplayermodels map resource pack\",\n        \"pack_format\": 6\n    }\n}");
            writer.close();
         } catch (IOException var6) {
            var6.printStackTrace();
         }
      }
   }

   @Override
   public void createMpmPartData(MpmPartData data) {
      data.clientData = new MpmPartDataClient();
   }
}
