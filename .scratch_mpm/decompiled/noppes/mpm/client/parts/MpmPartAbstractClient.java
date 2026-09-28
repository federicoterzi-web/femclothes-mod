package noppes.mpm.client.parts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import noppes.mpm.shared.util.NopVector3f;

public abstract class MpmPartAbstractClient extends MpmPart {
   public NopVector3f pos = NopVector3f.ZERO;
   public NopVector3f rot = NopVector3f.ZERO;
   protected Map<String, ModelPartWrapper> defaultPose = new HashMap<>();

   public void render(MpmPartData data, PoseStack mStack, MultiBufferSource typeBuffer, int lightmapUV, AbstractClientPlayer player) {
      VertexConsumer c = typeBuffer.m_6299_(RenderType.m_110473_(data.usePlayerSkin ? player.m_108560_() : data.getTexture()));
      this.render(data, mStack, c, lightmapUV, player);
   }

   public void render(MpmPartData data, PoseStack mStack, VertexConsumer c, int lightmapUV, AbstractClientPlayer player) {
   }

   @Override
   public final ModelPartWrapper getPart(String name) {
      return this.defaultPose.get(name);
   }
}
