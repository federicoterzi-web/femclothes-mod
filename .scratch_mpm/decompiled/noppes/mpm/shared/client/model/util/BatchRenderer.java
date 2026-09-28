package noppes.mpm.shared.client.model.util;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import noppes.mpm.shared.util.NopVector2i;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class BatchRenderer {
   private static final FloatBuffer MATRIX_BUFFER = MemoryUtil.memAllocFloat(16);
   public static RenderType lastType = null;
   private static final BatchRenderer instance = new BatchRenderer();
   private final Map<RenderType, List<BatchRenderer.Batch>> queue = new LinkedHashMap<>();

   public static BatchRenderer getInstance() {
      return instance;
   }

   public void add(
      RenderType renderType,
      ResourceLocation resource,
      int id,
      VertexFormat format,
      Matrix4f matrix,
      int vertexCount,
      NopVector2i texPos,
      int light,
      int overlay,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      if (renderType == null) {
         renderType = lastType;
      }

      this.queue
         .computeIfAbsent(renderType, k -> new LinkedList<>())
         .add(new BatchRenderer.Batch(resource, id, format, matrix, vertexCount, texPos, light, overlay, red, green, blue, alpha));
   }

   public void draw() {
      this.queue.forEach((renderType, batches) -> {
         if (!batches.isEmpty()) {
            RenderSystem.assertOnRenderThread();
            renderType.m_110185_();
            RenderSystem.setShader(GameRenderer::m_172838_);
            ShaderInstance shaderinstance = RenderSystem.getShader();

            for (BatchRenderer.Batch b : batches) {
               RenderSystem.setShaderTexture(0, b.resource);
               shaderinstance.f_173312_.m_5941_(new float[]{b.red, b.green, b.blue, b.alpha});
               if (shaderinstance.f_173313_ != null) {
                  shaderinstance.f_173313_.m_142617_(b.light1);
               }

               if (shaderinstance.f_173314_ != null) {
                  shaderinstance.f_173314_.m_142617_(b.light2);
               }

               shaderinstance.f_173308_.m_5679_(b.matrix);
               if (shaderinstance.f_200956_ != null) {
                  shaderinstance.f_200956_.m_200759_(RenderSystem.getInverseViewRotationMatrix());
               }

               if (shaderinstance.f_173315_ != null) {
                  shaderinstance.f_173315_.m_5985_(RenderSystem.getShaderFogStart());
               }

               if (shaderinstance.f_173316_ != null) {
                  shaderinstance.f_173316_.m_5985_(RenderSystem.getShaderFogEnd());
               }

               if (shaderinstance.f_173317_ != null) {
                  shaderinstance.f_173317_.m_5941_(RenderSystem.getShaderFogColor());
               }

               if (shaderinstance.f_202432_ != null) {
                  shaderinstance.f_202432_.m_142617_(RenderSystem.getShaderFogShape().m_202324_());
               }

               if (shaderinstance.f_173310_ != null) {
                  shaderinstance.f_173310_.m_5679_(new Matrix4f().translation(b.texPos.x, b.texPos.y, 0.0F));
               }

               if (shaderinstance.f_173319_ != null) {
                  shaderinstance.f_173319_.m_5985_(RenderSystem.getShaderGameTime());
               }

               if (shaderinstance.f_173311_ != null) {
                  Window window = Minecraft.m_91087_().m_91268_();
                  shaderinstance.f_173311_.m_7971_(window.m_85441_(), window.m_85442_());
               }

               RenderSystem.glBindBuffer(34962, () -> b.id);
               b.format.m_166912_();
               shaderinstance.m_173363_();
               RenderSystem.drawElements(4, 0, b.vertexCount);
               shaderinstance.m_173362_();
               b.format.m_86024_();
               RenderSystem.glBindBuffer(34962, () -> 0);
            }

            renderType.m_110188_();
         }
      });
      this.queue.clear();
   }

   class Batch {
      final Matrix4f matrix;
      final int vertexCount;
      final ResourceLocation resource;
      final int id;
      final VertexFormat format;
      final int light1;
      final int light2;
      final int overlay1;
      final int overlay2;
      final float red;
      final float green;
      final float blue;
      final float alpha;
      final NopVector2i texPos;

      public Batch(
         ResourceLocation resource,
         int id,
         VertexFormat format,
         Matrix4f matrix,
         int vertexCount,
         NopVector2i texPos,
         int light,
         int overlay,
         float red,
         float green,
         float blue,
         float alpha
      ) {
         this.resource = resource;
         this.id = id;
         this.format = format;
         this.matrix = matrix;
         this.vertexCount = vertexCount;
         this.texPos = texPos;
         this.light1 = light & 65535;
         this.light2 = light >> 16 & 65535;
         this.overlay1 = overlay & 65535;
         this.overlay2 = overlay >> 16 & 65535;
         this.red = red;
         this.green = green;
         this.blue = blue;
         this.alpha = alpha;
      }
   }
}
