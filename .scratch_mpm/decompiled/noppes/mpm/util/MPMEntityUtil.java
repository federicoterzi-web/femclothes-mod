package noppes.mpm.util;

import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.mpm.mixin.LivingEntityMixin;

public class MPMEntityUtil {
   public static void copy(LivingEntity copied, LivingEntity entity) {
      LivingEntityMixin copiedm = (LivingEntityMixin)copied;
      LivingEntityMixin entitym = (LivingEntityMixin)entity;
      entity.f_20919_ = copied.f_20919_;
      entity.f_19787_ = copied.f_19787_;
      entity.f_19867_ = copied.f_19787_;
      entity.f_19788_ = copied.f_19788_;
      entity.f_20902_ = copied.f_20902_;
      entity.f_20900_ = copied.f_20900_;
      entity.m_6853_(copied.m_20096_());
      entity.f_19789_ = copied.f_19789_;
      entity.m_6862_(copiedm.isJumping());
      entity.f_19854_ = copied.f_19854_;
      entity.f_19855_ = copied.f_19855_;
      entity.f_19856_ = copied.f_19856_;
      entity.m_6034_(copied.m_20185_(), copied.m_20186_(), copied.m_20189_());
      entity.f_19790_ = copied.f_19790_;
      entity.f_19791_ = copied.f_19791_;
      entity.f_19792_ = copied.f_19792_;
      entity.m_20256_(copied.m_20184_());
      entity.m_146926_(copied.m_146909_());
      entity.m_146922_(copied.m_146908_());
      entity.f_19860_ = copied.f_19860_;
      entity.f_19859_ = copied.f_19859_;
      entity.f_20885_ = copied.f_20885_;
      entity.f_20886_ = copied.f_20886_;
      entity.f_20883_ = copied.f_20883_;
      entity.f_20884_ = copied.f_20884_;
      entity.f_267362_.m_267771_(copied.f_267362_.m_267731_());
      entitym.setAnimStep(copiedm.getAnimStep());
      entitym.setAnimStepO(copiedm.getAnimStepO());
      entity.f_20921_ = copied.f_20921_;
      entity.f_20920_ = copied.f_20920_;
      entity.f_19797_ = copied.f_19797_;
      entity.m_21153_(Math.min(copied.m_21223_(), entity.m_21233_()));
      entity.getPersistentData().m_128391_(copied.getPersistentData());
      if (entity.m_20202_() != copied.m_20202_()) {
         entitym.setVehicle(copiedm.getVehicle());
      }

      if (entity instanceof Player && copied instanceof Player) {
         Player ePlayer = (Player)entity;
         Player cPlayer = (Player)copied;
         ePlayer.f_36100_ = cPlayer.f_36100_;
         ePlayer.f_36099_ = cPlayer.f_36099_;
         ePlayer.f_36102_ = cPlayer.f_36102_;
         ePlayer.f_36103_ = cPlayer.f_36103_;
         ePlayer.f_36104_ = cPlayer.f_36104_;
         ePlayer.f_36105_ = cPlayer.f_36105_;
         ePlayer.f_36106_ = cPlayer.f_36106_;
         ePlayer.f_36075_ = cPlayer.f_36075_;
      }

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         entity.m_8061_(slot, copied.m_6844_(slot));
      }

      if (entity instanceof EnderDragon) {
         entity.m_146926_(entity.m_146909_() + 180.0F);
      }
   }

   public static HashMap<String, ResourceLocation> getAllEntities(Level level) {
      HashMap<String, ResourceLocation> data = new HashMap<>();

      for (EntityType<? extends Entity> ent : ForgeRegistries.ENTITY_TYPES.getValues()) {
         try {
            Entity e = ent.m_20615_(level);
            if (e != null) {
               if (LivingEntity.class.isAssignableFrom(e.getClass())) {
                  data.put(ent.m_20675_(), ForgeRegistries.ENTITY_TYPES.getKey(ent));
               }

               e.m_142687_(RemovalReason.DISCARDED);
            }
         } catch (Exception var5) {
         }
      }

      return data;
   }
}
