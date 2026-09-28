package noppes.mpm.client;

import java.util.ArrayList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityFakeLiving extends LivingEntity {
   public EntityFakeLiving(Level level) {
      super(EntityType.f_20520_, level);
   }

   public Iterable<ItemStack> m_6168_() {
      return new ArrayList<>();
   }

   public ItemStack m_6844_(EquipmentSlot slotIn) {
      return ItemStack.f_41583_;
   }

   public void m_8061_(EquipmentSlot equipmentSlotType, ItemStack itemStack) {
   }

   public HumanoidArm m_5737_() {
      return HumanoidArm.LEFT;
   }
}
