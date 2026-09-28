package noppes.mpm.shared.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import noppes.mpm.shared.client.ImageDownloadAlt;
import noppes.mpm.shared.client.ResourceDownloader;
import noppes.mpm.shared.client.model.util.CustomRenderStates;
import noppes.mpm.shared.client.model.util.Polygon;
import noppes.mpm.shared.client.model.util.Vertex;
import noppes.mpm.shared.util.NopVector2i;
import noppes.mpm.shared.util.NopVector3f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Model2DRenderer extends NopModelPart {
   private final float x1;
   private final float x2;
   private final float y1;
   private final float y2;
   private final int width;
   private final int height;
   private final NopVector2i texPos;
   private float rotationOffsetX;
   private float rotationOffsetY;
   private float rotationOffsetZ;
   public static ResourceLocation textureOverride = null;
   private ResourceLocation location;
   private float scaleX = 1.0F;
   private float scaleY = 1.0F;
   private float thickness = 1.0F;
   private final Map<ResourceLocation, Polygon[]> compiled = new HashMap<>();
   VertexBuffer cache;

   public Model2DRenderer(int texWidth, int texHeight, int x, int y, int width, int height, ResourceLocation location) {
      super(texWidth, texHeight, 0, 0);
      this.width = width;
      this.height = height;
      this.texPos = new NopVector2i(x, y);
      this.setTexSize(texWidth, texHeight);
      this.location = location;
      this.x1 = (float)x / texWidth;
      this.y1 = (float)y / texHeight;
      this.x2 = ((float)x + width) / texWidth;
      this.y2 = ((float)y + height) / texHeight;
      this.init(location);
   }

   public Polygon[] init(ResourceLocation location) {
      Polygon[] polygons = this.compiled.get(location);
      if (polygons == null && location != null && !location.toString().isEmpty()) {
         if (ResourceDownloader.contains(location)) {
            return null;
         } else {
            BufferedImage image = null;

            try {
               Resource resource = (Resource)Minecraft.m_91087_().m_91098_().m_213713_(location).get();
               image = ImageIO.read(resource.m_215507_());
            } catch (Exception var24) {
               AbstractTexture text = Minecraft.m_91087_().m_91097_().m_174786_(location, null);
               if (text != null && text instanceof ImageDownloadAlt) {
                  try (FileInputStream input = new FileInputStream(((ImageDownloadAlt)text).cacheFile)) {
                     image = ImageIO.read(input);
                  } catch (Exception var21) {
                  }
               }
            }

            int scaleW = 1;
            int scaleH = 1;
            if (image != null) {
               scaleW = Math.max(1, (int)(image.getWidth() / this.xTexSize));
               scaleH = Math.max(1, (int)(image.getHeight() / this.yTexSize));
            }

            int width = this.width * scaleW;
            int height = this.height * scaleH;
            NopVector2i texPos = this.texPos.mul(scaleW, scaleH);
            polygons = new Polygon[]{
               new Polygon(
                  new Vector3f(0.0F, 0.0F, 1.0F),
                  new Vertex(0.0F, 0.0F, 0.0F, this.x1, this.y2),
                  new Vertex(1.0F, 0.0F, 0.0F, this.x2, this.y2),
                  new Vertex(1.0F, 1.0F, 0.0F, this.x2, this.y1),
                  new Vertex(0.0F, 1.0F, 0.0F, this.x1, this.y1)
               ),
               new Polygon(
                  new Vector3f(0.0F, 0.0F, -1.0F),
                  new Vertex(0.0F, 1.0F, -0.0625F, this.x1, this.y1),
                  new Vertex(1.0F, 1.0F, -0.0625F, this.x2, this.y1),
                  new Vertex(1.0F, 0.0F, -0.0625F, this.x2, this.y2),
                  new Vertex(0.0F, 0.0F, -0.0625F, this.x1, this.y2)
               ),
               null,
               null,
               null,
               null
            };
            List<Vertex> list = new ArrayList<>();
            List<Vertex> list2 = new ArrayList<>();

            for (int k = 0; k < width; k++) {
               float f7 = (float)k / width;
               float f8 = this.x1 + (this.x2 - this.x1) * f7 - 0.5F * (this.x1 - this.x2) / width;
               float f9 = f7 + 1.0F / width;
               boolean left = false;
               boolean right = false;
               if (image == null) {
                  left = true;
                  right = true;
               } else {
                  try {
                     for (int n = 0; n < height; n++) {
                        if ((image.getRGB(texPos.x + k, texPos.y + n) >> 24 & 0xFF) >= 128) {
                           if (k + 1 < width && (image.getRGB(texPos.x + k + 1, texPos.y + n) >> 24 & 0xFF) < 128) {
                              right = true;
                           } else if (k + 1 == width) {
                              right = true;
                           }

                           if (k > 0 && (image.getRGB(texPos.x + k - 1, texPos.y + n) >> 24 & 0xFF) < 128) {
                              left = true;
                           } else if (k == 0) {
                              left = true;
                           }
                        }
                     }
                  } catch (Exception var23) {
                  }
               }

               if (left) {
                  list.add(new Vertex(f7, 0.0F, -0.0625F, f8, this.y2));
                  list.add(new Vertex(f7, 0.0F, 0.0F, f8, this.y2));
                  list.add(new Vertex(f7, 1.0F, 0.0F, f8, this.y1));
                  list.add(new Vertex(f7, 1.0F, -0.0625F, f8, this.y1));
               }

               if (right) {
                  list2.add(new Vertex(f9, 1.0F, -0.0625F, f8, this.y1));
                  list2.add(new Vertex(f9, 1.0F, 0.0F, f8, this.y1));
                  list2.add(new Vertex(f9, 0.0F, 0.0F, f8, this.y2));
                  list2.add(new Vertex(f9, 0.0F, -0.0625F, f8, this.y2));
               }
            }

            polygons[2] = new Polygon(new Vector3f(-1.0F, 0.0F, 0.0F), list.toArray(new Vertex[0]));
            polygons[3] = new Polygon(new Vector3f(1.0F, 0.0F, 0.0F), list2.toArray(new Vertex[0]));
            list = new ArrayList<>();
            list2 = new ArrayList<>();

            for (int k = 0; k < height; k++) {
               float f7x = (float)k / height;
               float f8x = this.y2 + (this.y1 - this.y2) * f7x - 0.5F * (this.y2 - this.y1) / height;
               float f9x = f7x + 1.0F / height;
               boolean top = false;
               boolean bottom = false;
               if (image == null) {
                  top = true;
                  bottom = true;
               } else {
                  try {
                     for (int nx = 0; nx < width; nx++) {
                        int m = height - k - 1;
                        if ((image.getRGB(texPos.x + nx, texPos.y + m) >> 24 & 0xFF) >= 128) {
                           if (m > 0 && (image.getRGB(texPos.x + nx, texPos.y + m - 1) >> 24 & 0xFF) < 128) {
                              top = true;
                           } else if (m == 0) {
                              top = true;
                           }

                           if (m + 1 < height && (image.getRGB(texPos.x + nx, texPos.y + m + 1) >> 24 & 0xFF) < 128) {
                              bottom = true;
                           } else if (m + 1 == height) {
                              bottom = true;
                           }
                        }
                     }
                  } catch (Exception var22) {
                  }
               }

               if (bottom) {
                  list2.add(new Vertex(1.0F, f7x, 0.0F, this.x2, f8x));
                  list2.add(new Vertex(0.0F, f7x, 0.0F, this.x1, f8x));
                  list2.add(new Vertex(0.0F, f7x, -0.0625F, this.x1, f8x));
                  list2.add(new Vertex(1.0F, f7x, -0.0625F, this.x2, f8x));
               }

               if (top) {
                  list.add(new Vertex(0.0F, f9x, 0.0F, this.x1, f8x));
                  list.add(new Vertex(1.0F, f9x, 0.0F, this.x2, f8x));
                  list.add(new Vertex(1.0F, f9x, -0.0625F, this.x2, f8x));
                  list.add(new Vertex(0.0F, f9x, -0.0625F, this.x1, f8x));
               }
            }

            polygons[4] = new Polygon(new Vector3f(0.0F, 1.0F, 0.0F), list.toArray(new Vertex[0]));
            polygons[5] = new Polygon(new Vector3f(0.0F, -1.0F, 0.0F), list2.toArray(new Vertex[0]));
            this.compiled.put(location, polygons);
            return polygons;
         }
      } else {
         return polygons;
      }
   }

   @Override
   public void render(PoseStack mstack, VertexConsumer builder, int light, int overlay, float red, float green, float blue, float alpha) {
      this.render(textureOverride != null ? textureOverride : this.location, mstack, builder, light, overlay, red, green, blue, alpha);
   }

   public void render(
      ResourceLocation location, PoseStack mstack, VertexConsumer builder, int light, int overlay, float red, float green, float blue, float alpha
   ) {
      if (this.visible && location != null && !location.toString().isEmpty()) {
         mstack.m_85836_();
         this.translateAndRotate(mstack);
         float f = 0.0625F;
         mstack.m_252880_(this.rotationOffsetX * f, this.rotationOffsetY * f, this.rotationOffsetZ * f);
         mstack.m_85841_(this.scaleX * this.width / this.height, this.scaleY, this.thickness);
         mstack.m_252781_(Axis.f_252529_.m_252977_(180.0F));
         if (this.mirror) {
            mstack.m_252880_(0.0F, 0.0F, -1.0F * f);
            mstack.m_252781_(Axis.f_252436_.m_252977_(180.0F));
         }

         this.renderModel(location, mstack.m_85850_().m_252943_(), mstack.m_85850_().m_252922_(), builder, light, overlay, red, green, blue, alpha);
         mstack.m_85849_();
      }
   }

   public void render(ResourceLocation resource, PoseStack mstack, int light, int overlay, float red, float green, float blue, float alpha) {
      if (this.visible && resource != null) {
         Minecraft.m_91087_().m_91097_().m_174784_(resource);
         RenderType rType = CustomRenderStates.entityCutout(resource);
         RenderSystem.setShader(() -> CustomRenderStates.posTexNormalShader);
         RenderSystem.setShaderTexture(0, resource);
         RenderSystem.setTextureMatrix(new Matrix4f().translation(this.texPos.x, this.texPos.y, 0.0F));
         if (this.cache == null) {
            this.cache = new VertexBuffer(Usage.STATIC);
            PoseStack mmstack = new PoseStack();
            mmstack.m_85836_();
            Tesselator t = Tesselator.m_85913_();
            BufferBuilder bufferbuilder = t.m_85915_();
            bufferbuilder.m_166779_(Mode.TRIANGLES, CustomRenderStates.POS_TEX_NORMAL);
            this.renderModel(resource, mmstack.m_85850_().m_252943_(), mmstack.m_85850_().m_252922_(), bufferbuilder, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            bufferbuilder.m_231175_();
            this.cache.m_231221_(bufferbuilder.m_231175_());
            mmstack.m_85849_();
         }

         mstack.m_85836_();
         this.translateAndRotate(mstack);
         float f = 0.0625F;
         mstack.m_252880_(this.rotationOffsetX * f, this.rotationOffsetY * f, this.rotationOffsetZ * f);
         mstack.m_85841_(this.scaleX * this.width / this.height, this.scaleY, this.thickness);
         mstack.m_252781_(Axis.f_252529_.m_252977_(180.0F));
         if (this.mirror) {
            mstack.m_252880_(0.0F, 0.0F, -1.0F * f);
            mstack.m_252781_(Axis.f_252436_.m_252977_(180.0F));
         }

         Pose entry = mstack.m_85850_();
         Matrix4f matrix = entry.m_252922_();
         this.cache.m_253207_(matrix, new Matrix4f(), RenderSystem.getShader());
         mstack.m_85849_();
      }
   }

   public void renderModel(
      ResourceLocation resource,
      Matrix3f matrix3f,
      Matrix4f matrix4f,
      VertexConsumer builder,
      int light,
      int overlay,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      Polygon[] polygons = this.init(resource);
      if (polygons != null) {
         for (int i = 0; i < polygons.length; i++) {
            Polygon p = polygons[i];
            Vector3f vector3f = matrix3f.transform(new Vector3f(p.normal));
            float nX = vector3f.x();
            float nY = vector3f.y();
            float nZ = vector3f.z();

            for (int j = 0; j < p.vertexes.length; j++) {
               Vertex vec = p.vertexes[j];
               Vector4f vector4f = matrix4f.transform(new Vector4f(vec.pos, 1.0F));
               builder.m_5483_(vector4f.x(), vector4f.y(), vector4f.z());
               builder.m_85950_(red, green, blue, alpha);
               builder.m_7421_(vec.texCoords.x, vec.texCoords.y);
               builder.m_86008_(overlay);
               builder.m_85969_(light);
               builder.m_5601_(nX, nY, nZ);
               builder.m_5752_();
            }
         }
      }
   }

   private void addVertex(
      VertexConsumer builder,
      Matrix4f matrix,
      float x,
      float y,
      float z,
      float red,
      float green,
      float blue,
      float alpha,
      float texU,
      float texV,
      int overlayUV,
      int lightmapUV,
      float normalX,
      float normalY,
      float normalZ
   ) {
      Vector4f v = matrix.transform(new Vector4f(x, y, z, 1.0F));
      builder.m_5483_(v.x(), v.y(), v.z());
      builder.m_85950_(red, green, blue, alpha);
      builder.m_7421_(texU, texV);
      builder.m_86008_(overlayUV);
      builder.m_85969_(lightmapUV);
      builder.m_5601_(normalX, normalY, normalZ);
      builder.m_5752_();
   }

   public Model2DRenderer setRotationOffset(float x, float y, float z) {
      this.rotationOffsetX = x;
      this.rotationOffsetY = y;
      this.rotationOffsetZ = z;
      return this;
   }

   public Model2DRenderer setRotationOffset(NopVector3f scale) {
      this.rotationOffsetX = scale.x;
      this.rotationOffsetY = scale.y;
      this.rotationOffsetZ = scale.z;
      return this;
   }

   public void setScale(float scale) {
      this.scaleX = scale;
      this.scaleY = scale;
   }

   public void setScale(float x, float y) {
      this.scaleX = x;
      this.scaleY = y;
   }

   public Model2DRenderer setScale(NopVector3f scale) {
      this.scaleX = scale.x;
      this.scaleY = scale.y;
      this.thickness = scale.z;
      return this;
   }

   public void setThickness(float thickness) {
      this.thickness = thickness;
   }
}
