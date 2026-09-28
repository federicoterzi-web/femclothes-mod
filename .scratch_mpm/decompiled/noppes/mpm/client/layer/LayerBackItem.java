package noppes.mpm.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import noppes.mpm.MorePlayerModels;

public class LayerBackItem extends LayerInterface {
   public LayerBackItem(PlayerRenderer render) {
      super(render);
   }

   @Override
   public void render(
      PoseStack mStack,
      MultiBufferSource typeBuffer,
      int lightmapUV,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float age,
      float netHeadYaw,
      float headPitch
   ) {
      Minecraft minecraft = Minecraft.m_91087_();
      ItemStack itemstack = this.playerdata.backItem;
      if (MorePlayerModels.EnableBackItem && !itemstack.m_41619_() && !ItemStack.m_41656_(itemstack, this.player.m_150109_().m_36056_())) {
         Item item = itemstack.m_41720_();
         if (!(item instanceof BlockItem)) {
            this.base.f_102810_.m_104299_(mStack);
            mStack.m_252880_(0.0F, 0.36F, 0.14F);
            mStack.m_252781_(Axis.f_252529_.m_252977_(180.0F));
            if (item instanceof SwordItem) {
               mStack.m_252781_(Axis.f_252495_.m_252977_(180.0F));
            }

            BakedModel model = minecraft.m_91291_().m_115103_().m_109406_(itemstack);
            ItemTransform p_175034_1_ = model.m_7442_().f_111788_;
            mStack.m_85841_(p_175034_1_.f_111757_.x(), p_175034_1_.f_111757_.y(), p_175034_1_.f_111757_.z());
            minecraft.m_91291_()
               .m_269491_(
                  this.player,
                  itemstack,
                  ItemDisplayContext.NONE,
                  false,
                  mStack,
                  typeBuffer,
                  this.player.m_9236_(),
                  lightmapUV,
                  LivingEntityRenderer.m_115338_(this.player, 0.0F),
                  this.player.m_19879_() + ItemDisplayContext.NONE.ordinal()
               );
         }
      }
   }

   @Override
   public void rotate(float limbSwing, float limbSwingAmount, float partialTicks, float age, float netHeadYaw, float headPitch) {
   }
}
