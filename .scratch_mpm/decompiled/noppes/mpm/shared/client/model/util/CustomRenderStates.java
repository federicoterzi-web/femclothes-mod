package noppes.mpm.shared.client.model.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4f;

public class CustomRenderStates extends RenderStateShard {
   public static final Vector4f WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   public static VertexFormat POS_COL_TEX_LIGHT_FADE_NORMAL;
   public static VertexFormat POS_COL_TEX_NORMAL;
   public static final VertexFormat POS_TEX_NORMAL = new VertexFormat(
      ImmutableMap.of(
         "Position",
         DefaultVertexFormat.f_85804_,
         "UV0",
         DefaultVertexFormat.f_85806_,
         "Normal",
         DefaultVertexFormat.f_85809_,
         "Padding",
         DefaultVertexFormat.f_85810_
      )
   );
   protected static final TransparencyStateShard ADDITIVE_TRANSPARENCY = new TransparencyStateShard("lm_additive_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   protected static final TransparencyStateShard SUBTRACTIVE_TRANSPARENCY = new TransparencyStateShard("lm_subtractive_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.DST_COLOR, DestFactor.ONE_MINUS_SRC_ALPHA);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   private static final RenderType[] OBJ_RENDER_TYPES = new RenderType[CustomRenderStates.BLEND.values().length * 2];
   public static final RenderType OBJ_OUTLINE_RENDER_TYPE;
   protected static final ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_SHADER;
   public static ShaderInstance posTexNormalShader;
   private static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT;

   public CustomRenderStates(String p_i225973_1_, Runnable p_i225973_2_, Runnable p_i225973_3_) {
      super(p_i225973_1_, p_i225973_2_, p_i225973_3_);
   }

   public static RenderType getObjVBORenderType(int blending, boolean glow) {
      return OBJ_RENDER_TYPES[blending << 1 | (glow ? 1 : 0)];
   }

   public static RenderType entityCutout(ResourceLocation p_110444_) {
      return ENTITY_CUTOUT.apply(p_110444_);
   }

   public static RenderType getObjRenderType(ResourceLocation texture, int blending, boolean glow) {
      if (POS_COL_TEX_LIGHT_FADE_NORMAL == null) {
         Map<String, VertexFormatElement> vertexFormatValues = new HashMap<>();
         vertexFormatValues.put("Position", DefaultVertexFormat.f_85804_);
         vertexFormatValues.put("Color", DefaultVertexFormat.f_85805_);
         vertexFormatValues.put("UV0", DefaultVertexFormat.f_85806_);
         vertexFormatValues.put("UV1", DefaultVertexFormat.f_85807_);
         vertexFormatValues.put("UV2", DefaultVertexFormat.f_85808_);
         vertexFormatValues.put("Normal", DefaultVertexFormat.f_85809_);
         vertexFormatValues.put("Padding", DefaultVertexFormat.f_85810_);
         POS_COL_TEX_LIGHT_FADE_NORMAL = new VertexFormat(ImmutableMap.copyOf(vertexFormatValues));
      }

      TransparencyStateShard TransparencyStateShard = f_110139_;
      if (blending == CustomRenderStates.BLEND.ADD.getValue()) {
         TransparencyStateShard = ADDITIVE_TRANSPARENCY;
      } else if (blending == CustomRenderStates.BLEND.SUB.getValue()) {
         TransparencyStateShard = SUBTRACTIVE_TRANSPARENCY;
      }

      CompositeState renderTypeState = CompositeState.m_110628_()
         .m_173290_(new TextureStateShard(texture, false, false))
         .m_110685_(TransparencyStateShard)
         .m_110661_(f_110110_)
         .m_110671_(f_110152_)
         .m_110677_(f_110154_)
         .m_110691_(true);
      return RenderType.m_173215_("lm_obj_translucent_no_cull", POS_COL_TEX_LIGHT_FADE_NORMAL, Mode.TRIANGLES, 256, true, false, renderTypeState);
   }

   public static RenderType getObjColorOnlyRenderType(ResourceLocation texture, int blending, boolean glow) {
      if (POS_COL_TEX_LIGHT_FADE_NORMAL == null) {
         Map<String, VertexFormatElement> vertexFormatValues = new HashMap<>();
         vertexFormatValues.put("Position", DefaultVertexFormat.f_85804_);
         vertexFormatValues.put("Color", DefaultVertexFormat.f_85805_);
         vertexFormatValues.put("Normal", DefaultVertexFormat.f_85809_);
         vertexFormatValues.put("Padding", DefaultVertexFormat.f_85810_);
         POS_COL_TEX_LIGHT_FADE_NORMAL = new VertexFormat(ImmutableMap.copyOf(vertexFormatValues));
      }

      TransparencyStateShard TransparencyStateShard = f_110139_;
      if (blending == CustomRenderStates.BLEND.ADD.getValue()) {
         TransparencyStateShard = ADDITIVE_TRANSPARENCY;
      } else if (blending == CustomRenderStates.BLEND.SUB.getValue()) {
         TransparencyStateShard = SUBTRACTIVE_TRANSPARENCY;
      }

      CompositeState renderTypeState = CompositeState.m_110628_()
         .m_173290_(new TextureStateShard(texture, false, false))
         .m_110685_(TransparencyStateShard)
         .m_110661_(f_110110_)
         .m_110671_(f_110152_)
         .m_110677_(f_110154_)
         .m_110691_(true);
      return RenderType.m_173215_("lm_obj_translucent_no_cull", POS_COL_TEX_LIGHT_FADE_NORMAL, Mode.TRIANGLES, 256, true, false, renderTypeState);
   }

   public static RenderType getObjOutlineRenderType(ResourceLocation texture) {
      if (POS_COL_TEX_LIGHT_FADE_NORMAL == null) {
         Map<String, VertexFormatElement> vertexFormatValues = new HashMap<>();
         vertexFormatValues.put("Position", DefaultVertexFormat.f_85804_);
         vertexFormatValues.put("Color", DefaultVertexFormat.f_85805_);
         vertexFormatValues.put("UV0", DefaultVertexFormat.f_85806_);
         vertexFormatValues.put("UV1", DefaultVertexFormat.f_85807_);
         vertexFormatValues.put("UV2", DefaultVertexFormat.f_85808_);
         vertexFormatValues.put("Normal", DefaultVertexFormat.f_85809_);
         vertexFormatValues.put("Padding", DefaultVertexFormat.f_85810_);
         POS_COL_TEX_LIGHT_FADE_NORMAL = new VertexFormat(ImmutableMap.copyOf(vertexFormatValues));
      }

      CompositeState renderTypeState = CompositeState.m_110628_()
         .m_173290_(new TextureStateShard(texture, false, false))
         .m_110661_(f_110110_)
         .m_110663_(f_110111_)
         .m_110675_(f_110124_)
         .m_110691_(false);
      return RenderType.m_173215_("lm_obj_outline_no_cull", POS_COL_TEX_LIGHT_FADE_NORMAL, Mode.TRIANGLES, 256, true, false, renderTypeState);
   }

   public static RenderType getSpriteRenderType(ResourceLocation texture) {
      if (POS_COL_TEX_NORMAL == null) {
         Map<String, VertexFormatElement> vertexFormatValues = new HashMap<>();
         vertexFormatValues.put("Position", DefaultVertexFormat.f_85804_);
         vertexFormatValues.put("Color", DefaultVertexFormat.f_85805_);
         vertexFormatValues.put("UV0", DefaultVertexFormat.f_85806_);
         vertexFormatValues.put("Normal", DefaultVertexFormat.f_85809_);
         vertexFormatValues.put("Padding", DefaultVertexFormat.f_85810_);
         POS_COL_TEX_NORMAL = new VertexFormat(ImmutableMap.copyOf(vertexFormatValues));
      }

      CompositeState renderTypeState = CompositeState.m_110628_().m_173290_(new TextureStateShard(texture, false, false)).m_110691_(true);
      return RenderType.m_173215_("lm_sprite", POS_COL_TEX_NORMAL, Mode.QUADS, 256, true, false, renderTypeState);
   }

   static {
      for (CustomRenderStates.BLEND blend : CustomRenderStates.BLEND.values()) {
         for (int glow = 0; glow < 2; glow++) {
            OBJ_RENDER_TYPES[blend.id * 2 + glow] = RenderType.m_173215_(
               "lm_obj_" + blend.toString() + (glow == 1 ? "_glow" : ""),
               POS_TEX_NORMAL,
               Mode.TRIANGLES,
               256,
               true,
               false,
               CompositeState.m_110628_()
                  .m_110685_(
                     blend == CustomRenderStates.BLEND.ADD
                        ? ADDITIVE_TRANSPARENCY
                        : (blend == CustomRenderStates.BLEND.SUB ? SUBTRACTIVE_TRANSPARENCY : f_110139_)
                  )
                  .m_110661_(f_110110_)
                  .m_110671_(f_110152_)
                  .m_110677_(f_110154_)
                  .m_110691_(false)
            );
         }
      }

      OBJ_OUTLINE_RENDER_TYPE = RenderType.m_173215_(
         "lm_obj_outline_no_cull",
         POS_TEX_NORMAL,
         Mode.TRIANGLES,
         256,
         true,
         false,
         CompositeState.m_110628_().m_110663_(f_110111_).m_110661_(f_110110_).m_110675_(f_110124_).m_110691_(false)
      );
      RENDERTYPE_ENTITY_CUTOUT_SHADER = new ShaderStateShard(GameRenderer::m_172664_);
      posTexNormalShader = null;
      ENTITY_CUTOUT = Util.m_143827_(
         p_173202_ -> {
            CompositeState rendertype$compositestate = CompositeState.m_110628_()
               .m_173292_(new ShaderStateShard(() -> posTexNormalShader))
               .m_173290_(new TextureStateShard(p_173202_, false, false))
               .m_110685_(f_110134_)
               .m_110671_(f_110152_)
               .m_110677_(f_110154_)
               .m_110691_(true);
            return RenderType.m_173215_("nop_entity_cutout", POS_TEX_NORMAL, Mode.TRIANGLES, 256, true, false, rendertype$compositestate);
         }
      );
   }

   public static enum BLEND {
      NORMAL(0),
      ADD(1),
      SUB(2);

      public final int id;

      private BLEND(int value) {
         this.id = value;
      }

      public int getValue() {
         return this.id;
      }
   }
}
