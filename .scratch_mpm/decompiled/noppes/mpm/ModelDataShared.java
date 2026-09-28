package noppes.mpm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.mpm.client.parts.MpmPart;
import noppes.mpm.client.parts.MpmPartData;
import noppes.mpm.constants.BodyPart;
import noppes.mpm.constants.EnumParts;

public abstract class ModelDataShared {
   public ModelPartConfig arm1 = new ModelPartConfig();
   public ModelPartConfig arm2 = new ModelPartConfig();
   public ModelPartConfig body = new ModelPartConfig();
   public ModelPartConfig leg1 = new ModelPartConfig();
   public ModelPartConfig leg2 = new ModelPartConfig();
   public ModelPartConfig head = new ModelPartConfig();
   protected ResourceLocation entityName = null;
   protected LivingEntity entity;
   public CompoundTag extra = new CompoundTag();
   public ListTag oldPartData = new ListTag();
   public List<MpmPartData> mpmParts = new ArrayList<>();
   public List<BodyPart> hiddenParts = new ArrayList<>();
   public int wingMode = 0;
   public String url = "";
   public String displayName = "";

   public CompoundTag writeToNBT() {
      CompoundTag compound = new CompoundTag();
      if (this.entityName != null) {
         compound.m_128359_("EntityName", this.entityName.toString());
      }

      compound.m_128365_("ArmsConfig", this.arm1.writeToNBT());
      compound.m_128365_("Arms2Config", this.arm2.writeToNBT());
      compound.m_128365_("BodyConfig", this.body.writeToNBT());
      compound.m_128365_("LegsConfig", this.leg1.writeToNBT());
      compound.m_128365_("Legs2Config", this.leg2.writeToNBT());
      compound.m_128365_("HeadConfig", this.head.writeToNBT());
      compound.m_128365_("ExtraData", this.extra);
      compound.m_128405_("WingMode", this.wingMode);
      compound.m_128359_("CustomSkinUrl", this.url);
      compound.m_128359_("DisplayName", this.displayName);
      compound.m_128365_("Parts", this.oldPartData);
      ListTag list = new ListTag();

      for (MpmPartData e : this.mpmParts) {
         list.add(e.getNbt());
      }

      compound.m_128365_("NewParts", list);
      return compound;
   }

   public void readFromNBT(CompoundTag compound) {
      String rl = compound.m_128461_("EntityName");
      this.setEntity(rl.isEmpty() ? null : new ResourceLocation(rl));
      this.arm1.readFromNBT(compound.m_128469_("ArmsConfig"));
      this.arm2.readFromNBT(compound.m_128469_("Arms2Config"));
      this.body.readFromNBT(compound.m_128469_("BodyConfig"));
      this.leg1.readFromNBT(compound.m_128469_("LegsConfig"));
      this.leg2.readFromNBT(compound.m_128469_("Legs2Config"));
      this.head.readFromNBT(compound.m_128469_("HeadConfig"));
      this.extra = compound.m_128469_("ExtraData");
      this.wingMode = compound.m_128451_("WingMode");
      this.url = compound.m_128461_("CustomSkinUrl");
      this.displayName = compound.m_128461_("DisplayName");
      List<MpmPartData> mpmParts = new ArrayList<>();
      ListTag list = compound.m_128437_("NewParts", 10);

      for (int i = 0; i < list.size(); i++) {
         MpmPartData part = new MpmPartData();
         part.setNbt(list.m_128728_(i));
         if (part.partId.equals(ModelEyeData.RESOURCE) || part.partId.equals(ModelEyeData.RESOURCE_RIGHT) || part.partId.equals(ModelEyeData.RESOURCE_LEFT)) {
            part = new ModelEyeData();
            part.setNbt(list.m_128728_(i));
         }

         MorePlayerModels.proxy.createMpmPartData(part);
         mpmParts.add(part);
      }

      this.mpmParts = mpmParts;
      this.oldPartData = compound.m_128437_("Parts", 10);
      if (this.mpmParts.isEmpty()) {
         for (int i = 0; i < list.size(); i++) {
            this.mpmParts.add(EnumParts.convertOldPart(list.m_128728_(i)));
         }
      }

      this.refreshParts();
      this.updateTransate();
   }

   public void updateTransate() {
      for (EnumParts part : EnumParts.values()) {
         ModelPartConfig config = this.getPartConfig(part);
         if (config != null) {
            if (part == EnumParts.HEAD) {
               config.setTranslate(0.0F, this.getBodyY(), 0.0F);
            } else if (part == EnumParts.ARM_LEFT) {
               ModelPartConfig body = this.getPartConfig(EnumParts.BODY);
               float x = (1.0F - body.scaleX) * 0.25F + (1.0F - config.scaleX) * 0.0625F;
               float y = this.getBodyY() + (1.0F - config.scaleY) * -0.125F;
               config.setTranslate(-x, y, 0.0F);
               if (!config.notShared) {
                  ModelPartConfig arm = this.getPartConfig(EnumParts.ARM_RIGHT);
                  arm.copyValues(config);
               }
            } else if (part == EnumParts.ARM_RIGHT) {
               ModelPartConfig body = this.getPartConfig(EnumParts.BODY);
               float x = (1.0F - body.scaleX) * 0.25F + (1.0F - config.scaleX) * 0.0625F;
               float y = this.getBodyY() + (1.0F - config.scaleY) * -0.125F;
               config.setTranslate(x, y, 0.0F);
            } else if (part == EnumParts.LEG_LEFT) {
               config.setTranslate(-(1.0F - config.scaleX) * 0.118F, this.getLegsY(), -(1.0F - config.scaleZ) * 0.00625F);
               if (!config.notShared) {
                  ModelPartConfig leg = this.getPartConfig(EnumParts.LEG_RIGHT);
                  leg.copyValues(config);
               }
            } else if (part == EnumParts.LEG_RIGHT) {
               config.setTranslate((1.0F - config.scaleX) * 0.118F, this.getLegsY(), -(1.0F - config.scaleZ) * 0.00625F);
            } else if (part == EnumParts.BODY) {
               config.setTranslate(0.0F, this.getBodyY(), 0.0F);
            }
         }
      }
   }

   public void setEntity(ResourceLocation resourceLocation) {
      this.entityName = resourceLocation;
      this.clearEntity();
      this.extra = new CompoundTag();
   }

   public ResourceLocation getEntityName() {
      return this.entityName;
   }

   public boolean hasEntity() {
      return this.entityName != null;
   }

   public float offsetY() {
      return this.entity == null ? -this.getBodyY() : this.entity.m_20206_() - 1.8F;
   }

   public void clearEntity() {
      this.entity = null;
   }

   public ModelPartConfig getPartConfig(EnumParts type) {
      if (type == EnumParts.BODY) {
         return this.body;
      } else if (type == EnumParts.ARM_LEFT) {
         return this.arm1;
      } else if (type == EnumParts.ARM_RIGHT) {
         return this.arm2;
      } else if (type == EnumParts.LEG_LEFT) {
         return this.leg1;
      } else {
         return type == EnumParts.LEG_RIGHT ? this.leg2 : this.head;
      }
   }

   public float getBodyY() {
      return this.entity != null ? this.entity.m_20206_() : (1.0F - this.body.scaleY) * 0.75F + this.getLegsY();
   }

   public float getLegsY() {
      ModelPartConfig legs = this.leg1;
      if (this.leg1.notShared && this.leg2.scaleY > this.leg1.scaleY) {
         legs = this.leg2;
      }

      return (1.0F - legs.scaleY) * 0.75F;
   }

   public void refreshParts() {
      this.hiddenParts = this.mpmParts.stream().flatMap(part -> {
         MpmPart p = part.getPart();
         return p != null ? p.hiddenParts.stream() : Stream.empty();
      }).distinct().collect(Collectors.toList());
   }
}
