package noppes.mpm.client.fx;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import noppes.mpm.ModelPartData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EntityEnderFX extends TextureSheetParticle {
   public static SpriteSet portalSprite;
   private int particleNumber;
   private AbstractClientPlayer player;
   private final ResourceLocation location;
   private boolean move = true;
   private float startX = 0.0F;
   private float startY = 0.0F;
   private float startZ = 0.0F;
   private final double portalPosX;
   private final double portalPosY;
   private final double portalPosZ;

   public EntityEnderFX(AbstractClientPlayer player, double x, double y, double z, double par8, double par10, double par12, ModelPartData data) {
      super(player.f_108545_, x, y, z);
      this.m_108335_(portalSprite);
      this.f_107215_ = par8;
      this.f_107216_ = par10;
      this.f_107217_ = par12;
      this.player = player;
      this.particleNumber = player.m_217043_().m_188503_(2);
      this.f_107663_ = 0.1F * (this.f_107223_.m_188501_() * 0.2F + 0.5F);
      this.f_107227_ = (data.color >> 16 & 0xFF) / 255.0F;
      this.f_107228_ = (data.color >> 8 & 0xFF) / 255.0F;
      this.f_107229_ = (data.color & 0xFF) / 255.0F;
      this.f_107212_ = this.portalPosX = x;
      this.f_107213_ = this.portalPosY = y;
      this.f_107214_ = this.portalPosZ = z;
      if (data.playerTexture) {
         this.location = player.m_108560_();
      } else {
         this.location = data.getResource();
      }

      this.f_107225_ = (int)(Math.random() * 10.0) + 40;
   }

   public float m_5902_(float p_217561_1_) {
      float scale = (this.f_107224_ + p_217561_1_) / this.f_107225_;
      scale = 1.0F - scale;
      scale *= scale;
      scale = 1.0F - scale;
      return this.f_107663_ * scale;
   }

   public int m_6355_(float p_189214_1_) {
      int lvt_2_1_ = super.m_6355_(p_189214_1_);
      float lvt_3_1_ = (float)this.f_107224_ / this.f_107225_;
      lvt_3_1_ *= lvt_3_1_;
      lvt_3_1_ *= lvt_3_1_;
      int lvt_4_1_ = lvt_2_1_ & 0xFF;
      int lvt_5_1_ = lvt_2_1_ >> 16 & 0xFF;
      lvt_5_1_ += (int)(lvt_3_1_ * 15.0F * 16.0F);
      if (lvt_5_1_ > 240) {
         lvt_5_1_ = 240;
      }

      return lvt_4_1_ | lvt_5_1_ << 16;
   }

   public void m_5989_() {
      this.f_107209_ = this.f_107212_;
      this.f_107210_ = this.f_107213_;
      this.f_107211_ = this.f_107214_;
      if (this.f_107224_++ >= this.f_107225_) {
         this.m_107274_();
      } else {
         float lvt_1_1_ = (float)this.f_107224_ / this.f_107225_;
         float var3 = -lvt_1_1_ + lvt_1_1_ * lvt_1_1_ * 2.0F;
         float var4 = 1.0F - var3;
         this.f_107212_ = this.portalPosX + this.f_107215_ * var4;
         this.f_107213_ = this.portalPosY + this.f_107216_ * var4 + (1.0F - lvt_1_1_);
         this.f_107214_ = this.portalPosZ + this.f_107217_ * var4;
      }
   }

   public void m_5744_(VertexConsumer renderer, Camera info, float partialTicks) {
      BufferBuilder buffer = (BufferBuilder)renderer;
      if (this.move) {
         this.startX = (float)(this.player.f_19854_ + (this.player.m_20185_() - this.player.f_19854_) * partialTicks);
         this.startY = (float)(this.player.f_19855_ + (this.player.m_20186_() - this.player.f_19855_) * partialTicks);
         this.startZ = (float)(this.player.f_19856_ + (this.player.m_20189_() - this.player.f_19856_) * partialTicks);
      }

      Tesselator tessellator = Tesselator.m_85913_();
      tessellator.m_85914_();
      Minecraft.m_91087_().f_90987_.m_174784_(this.location);
      buffer.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85813_);
      Vec3 vector3d = info.m_90583_();
      float f = (float)(Mth.m_14139_(partialTicks, this.f_107209_, this.f_107212_) - vector3d.m_7096_());
      float f1 = (float)(Mth.m_14139_(partialTicks, this.f_107210_, this.f_107213_) - vector3d.m_7098_());
      float f2 = (float)(Mth.m_14139_(partialTicks, this.f_107211_, this.f_107214_) - vector3d.m_7094_());
      Quaternionf quaternion;
      if (this.f_107231_ == 0.0F) {
         quaternion = info.m_253121_();
      } else {
         quaternion = new Quaternionf(info.m_253121_());
         float f3 = Mth.m_14179_(partialTicks, this.f_107204_, this.f_107231_);
         quaternion.mul(Axis.f_252403_.m_252961_(f3));
      }

      Vector3f vector3f1 = quaternion.transform(new Vector3f(-1.0F, -1.0F, 0.0F));
      Vector3f[] avector3f = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
      };
      float f4 = this.m_5902_(partialTicks);

      for (int i = 0; i < 4; i++) {
         Vector3f vector3f = quaternion.transform(avector3f[i]);
         vector3f.mul(f4);
         vector3f.add(f, f1, f2);
      }

      float f7 = 0.875F;
      float f8 = f7 + 0.125F;
      float f5 = 0.75F - this.particleNumber * 0.25F;
      float f6 = f5 + 0.25F;
      int j = this.m_6355_(partialTicks);
      buffer.m_5483_(avector3f[0].x(), avector3f[0].y(), avector3f[0].z())
         .m_7421_(f8, f6)
         .m_85950_(this.f_107227_, this.f_107228_, this.f_107229_, this.f_107230_)
         .m_85969_(j)
         .m_5752_();
      buffer.m_5483_(avector3f[1].x(), avector3f[1].y(), avector3f[1].z())
         .m_7421_(f8, f5)
         .m_85950_(this.f_107227_, this.f_107228_, this.f_107229_, this.f_107230_)
         .m_85969_(j)
         .m_5752_();
      buffer.m_5483_(avector3f[2].x(), avector3f[2].y(), avector3f[2].z())
         .m_7421_(f7, f5)
         .m_85950_(this.f_107227_, this.f_107228_, this.f_107229_, this.f_107230_)
         .m_85969_(j)
         .m_5752_();
      buffer.m_5483_(avector3f[3].x(), avector3f[3].y(), avector3f[3].z())
         .m_7421_(f7, f6)
         .m_85950_(this.f_107227_, this.f_107228_, this.f_107229_, this.f_107230_)
         .m_85969_(j)
         .m_5752_();
      tessellator.m_85914_();
      Minecraft.m_91087_().f_90987_.m_174784_(TextureAtlas.f_118260_);
      buffer.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85813_);
   }

   public void m_6257_(double p_187110_1_, double p_187110_3_, double p_187110_5_) {
      this.m_107259_(this.m_107277_().m_82386_(p_187110_1_, p_187110_3_, p_187110_5_));
      this.m_107275_();
   }

   public ParticleRenderType m_7556_() {
      return ParticleRenderType.f_107430_;
   }
}
