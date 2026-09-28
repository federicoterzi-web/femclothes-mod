package noppes.mpm.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class EntityRainbowFX extends Particle {
   private float quadSize;
   public static float[][] colorTable = new float[][]{
      {1.0F, 0.0F, 0.0F}, {1.0F, 0.5F, 0.0F}, {1.0F, 1.0F, 0.0F}, {0.0F, 1.0F, 0.0F}, {0.0F, 0.0F, 1.0F}, {0.0F, 4375.0F, 0.0F, 1.0F}, {0.5625F, 0.0F, 1.0F}
   };
   float reddustParticleScale;

   public EntityRainbowFX(ClientLevel world, double d, double d1, double d2, double f, double f1, double f2) {
      this(world, d, d1, d2, 1.0F, f, f1, f2);
      this.quadSize = 0.1F * (this.f_107223_.m_188501_() * 0.2F + 0.5F);
   }

   public EntityRainbowFX(ClientLevel world, double d, double d1, double d2, float f, double f1, double f2, double f3) {
      super(world, d, d1, d2, 0.0, 0.0, 0.0);
      this.f_107215_ *= 0.1F;
      this.f_107216_ *= 0.1F;
      this.f_107217_ *= 0.1F;
      if (f1 == 0.0) {
         f1 = 1.0;
      }

      int i = world.f_46441_.m_188503_(colorTable.length);
      this.f_107227_ = colorTable[i][0];
      this.f_107228_ = colorTable[i][1];
      this.f_107229_ = colorTable[i][2];
      this.quadSize *= 0.75F;
      this.quadSize *= f;
      this.reddustParticleScale = this.quadSize;
      this.f_107225_ = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      this.f_107225_ = (int)(this.f_107225_ * f);
   }

   public void m_5744_(VertexConsumer renderer, Camera info, float partialTicks) {
      float f6 = (this.f_107224_ + partialTicks) / this.f_107225_ * 32.0F;
      if (f6 < 0.0F) {
         f6 = 0.0F;
      } else if (f6 > 1.0F) {
         f6 = 1.0F;
      }

      this.quadSize = this.reddustParticleScale * f6;
   }

   public ParticleRenderType m_7556_() {
      return ParticleRenderType.f_107430_;
   }

   public void m_5989_() {
      this.f_107209_ = this.f_107212_;
      this.f_107210_ = this.f_107213_;
      this.f_107211_ = this.f_107214_;
      if (this.f_107224_++ >= this.f_107225_) {
         this.m_107274_();
      }

      this.m_6257_(this.f_107215_, this.f_107216_, this.f_107217_);
      if (this.f_107213_ == this.f_107210_) {
         this.f_107215_ *= 1.1;
         this.f_107217_ *= 1.1;
      }

      this.f_107215_ *= 0.96F;
      this.f_107216_ *= 0.96F;
      this.f_107217_ *= 0.96F;
      if (this.f_107218_) {
         this.f_107215_ *= 0.7F;
         this.f_107217_ *= 0.7F;
      }
   }
}
