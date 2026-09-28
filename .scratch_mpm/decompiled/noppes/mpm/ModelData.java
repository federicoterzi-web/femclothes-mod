package noppes.mpm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.util.PixelmonHelper;

public class ModelData extends ModelDataShared implements ICapabilityProvider {
   public static ExecutorService saveExecutor = Executors.newFixedThreadPool(1);
   public static Capability<ModelData> MODELDATA_CAPABILITY = CapabilityManager.get(new CapabilityToken<ModelData>() {});
   private LazyOptional<ModelData> instance = LazyOptional.of(() -> this);
   public boolean resourceInit = false;
   public boolean resourceLoaded = false;
   public ResourceLocation resourceLocation = null;
   public ResourceLocation resourceLoading = null;
   public boolean webapiActive = false;
   public boolean webapiInit = false;
   public Object textureObject = null;
   public ItemStack backItem = ItemStack.f_41583_;
   public int inLove = 0;
   public int animationTime = -1;
   public int modelType = 0;
   public EnumAnimation moveAnimation = EnumAnimation.IDLE;
   public EnumAnimation prevMoveAnimation = EnumAnimation.IDLE;
   public boolean startMoveAnimation = false;
   public EnumAnimation animation = EnumAnimation.NONE;
   public EnumAnimation prevAnimation = EnumAnimation.NONE;
   public boolean startAnimation = false;
   public int animationStart = 0;
   public float sleepRotation;
   public short soundType = 0;
   public double prevPosX;
   public double prevPosY;
   public double prevPosZ;
   public Player player = null;
   public long lastEdited = System.currentTimeMillis();
   public UUID analyticsUUID = UUID.randomUUID();
   public String presetName = "Default";
   private static ModelData backup = new ModelData();

   @Override
   public synchronized CompoundTag writeToNBT() {
      CompoundTag compound = super.writeToNBT();
      compound.m_128376_("SoundType", this.soundType);
      compound.m_128405_("Animation", this.animation.ordinal());
      compound.m_128405_("MoveAnimation", this.moveAnimation.ordinal());
      compound.m_128356_("LastEdited", this.lastEdited);
      compound.m_128356_("ModelType", this.modelType);
      compound.m_128359_("PresetName", this.presetName);
      return compound;
   }

   @Override
   public synchronized void readFromNBT(CompoundTag compound) {
      String prevUrl = this.url;
      String prevName = this.displayName;
      super.readFromNBT(compound);
      this.soundType = compound.m_128448_("SoundType");
      this.lastEdited = compound.m_128454_("LastEdited");
      this.modelType = compound.m_128451_("ModelType");
      this.presetName = compound.m_128461_("PresetName");
      if (this.player != null) {
         if (!this.hasEntity()) {
            this.player.getPersistentData().m_128473_("MPMModel");
         } else {
            this.player.getPersistentData().m_128359_("MPMModel", this.getEntityName().toString());
         }
      }

      this.setAnimation(compound.m_128451_("Animation"));
      this.setMoveAnimation(compound.m_128451_("MoveAnimation"));
      if (!prevUrl.equals(this.url)) {
         this.resourceInit = false;
         this.resourceLoaded = false;
         this.resourceLocation = null;
      }

      if (!prevName.equals(this.displayName) && this.player != null) {
         this.player.refreshDisplayName();
      }
   }

   public void setMoveAnimation(int i) {
      if (i < EnumAnimation.values().length) {
         this.setMoveAnimation(EnumAnimation.values()[i]);
      } else {
         this.setMoveAnimation(EnumAnimation.IDLE);
      }
   }

   public void setMoveAnimation(EnumAnimation ani) {
      this.startMoveAnimation = this.moveAnimation != ani;
      this.moveAnimation = ani;
   }

   public EnumAnimation getMoveAnimtion(AbstractClientPlayer player) {
      if (player.m_20159_()) {
         return EnumAnimation.SIT;
      } else if (player.m_5803_()) {
         return EnumAnimation.SLEEP;
      } else {
         return this.moveAnimation == EnumAnimation.IDLE && player.m_6047_() ? EnumAnimation.CROUCH : this.moveAnimation;
      }
   }

   public void setAnimation(int i) {
      if (i < EnumAnimation.values().length) {
         this.setAnimation(EnumAnimation.values()[i]);
      } else {
         this.setAnimation(EnumAnimation.NONE);
      }
   }

   public boolean isMovementAnimation(EnumAnimation ani) {
      return ani == EnumAnimation.SLEEP
         || ani == EnumAnimation.CRAWL
         || ani == EnumAnimation.CROUCH
         || ani == EnumAnimation.SIT
         || ani == EnumAnimation.DEATH
         || ani == EnumAnimation.WALK
         || ani == EnumAnimation.IDLE
         || ani == EnumAnimation.FLY_IDLE
         || ani == EnumAnimation.FLY;
   }

   public void setAnimation(EnumAnimation ani) {
      if (this.isMovementAnimation(ani)) {
         this.setMoveAnimation(ani);
      } else {
         this.animationTime = -1;
         this.animation = ani;
         this.lastEdited = System.currentTimeMillis();
         this.startAnimation = this.animation != ani;
         if (this.animation == EnumAnimation.WAVE) {
            this.animationTime = 80;
         }

         if (this.animation == EnumAnimation.YES || this.animation == EnumAnimation.NO) {
            this.animationTime = 60;
         }

         if (this.player != null && ani != EnumAnimation.NONE) {
            this.animationStart = this.player.f_19797_;
         } else {
            this.animationStart = -1;
         }
      }
   }

   public LivingEntity getEntity(Player player) {
      if (!this.hasEntity()) {
         return null;
      } else {
         if (this.entity == null) {
            try {
               this.entity = (LivingEntity)((EntityType)ForgeRegistries.ENTITY_TYPES.getValue(this.getEntityName())).m_20615_(player.m_9236_());
               CompoundTag comp = new CompoundTag();
               this.entity.m_7380_(comp);
               if (PixelmonHelper.isPixelmon(this.entity) && player.m_9236_().f_46443_ && !this.extra.m_128441_("Name")) {
                  this.extra.m_128359_("Name", "Abra");
               }

               comp = comp.m_128391_(this.extra);
               this.entity.m_7378_(comp);
               this.entity.m_20331_(true);
               this.entity.m_21051_(Attributes.f_22276_).m_22100_(player.m_21233_());
               this.entity.m_8061_(EquipmentSlot.MAINHAND, player.m_21205_());
               this.entity.m_8061_(EquipmentSlot.OFFHAND, player.m_21206_());
               this.entity.m_8061_(EquipmentSlot.HEAD, player.m_150109_().m_8020_(3));
               this.entity.m_8061_(EquipmentSlot.CHEST, player.m_150109_().m_8020_(2));
               this.entity.m_8061_(EquipmentSlot.LEGS, player.m_150109_().m_8020_(1));
               this.entity.m_8061_(EquipmentSlot.FEET, player.m_150109_().m_8020_(0));
               this.resourceInit = false;
            } catch (Exception var3) {
            }
         }

         return this.entity;
      }
   }

   public ModelData copy() {
      ModelData data = new ModelData();
      data.readFromNBT(this.writeToNBT());
      data.resourceLoaded = this.resourceLoaded;
      data.player = this.player;
      return data;
   }

   @Override
   public void clearEntity() {
      super.clearEntity();
      this.resourceInit = false;
   }

   public float getOffsetCamera(Player player) {
      if (!MorePlayerModels.EnablePOV) {
         return 0.0F;
      } else {
         float offset = -this.offsetY();
         if (this.entity == null) {
            if (this.moveAnimation == EnumAnimation.SIT) {
               offset += 0.5F - this.getLegsY();
            }

            if (this.moveAnimation == EnumAnimation.SLEEP) {
               offset = 1.18F;
            }

            if (this.moveAnimation == EnumAnimation.CRAWL) {
               offset = 0.8F;
            }
         }

         if (offset < -0.2F && this.isBlocked(player)) {
            offset = -0.2F;
         }

         return offset;
      }
   }

   private boolean isBlocked(Player player) {
      return !player.m_9236_().m_46859_(new BlockPos(BlockPos.m_274446_(player.m_20182_())).m_6630_(2));
   }

   public void save() {
      if (this.player != null) {
         Player player = this.player;
         saveExecutor.submit(() -> {
            try {
               String filename = player.m_20148_().toString().toLowerCase();
               if (filename.isEmpty()) {
                  filename = "noplayername";
               }

               filename = filename + ".dat";
               File file = new File(MorePlayerModels.dir, filename + "_new");
               File file1 = new File(MorePlayerModels.dir, filename + "_old");
               File file2 = new File(MorePlayerModels.dir, filename);
               NbtIo.m_128947_(this.writeToNBT(), new FileOutputStream(file));
               if (file1.exists()) {
                  file1.delete();
               }

               file2.renameTo(file1);
               if (file2.exists()) {
                  file2.delete();
               }

               file.renameTo(file2);
               if (file.exists()) {
                  file.delete();
               }
            } catch (Exception var6) {
               LogWriter.except(var6);
            }
         });
      }
   }

   public static ModelData get(Player player) {
      ModelData data = (ModelData)player.getCapability(MODELDATA_CAPABILITY, null).orElse(backup);
      if (data.player == null) {
         data.player = player;
         data.backItem = player.m_150109_().m_8020_(0);
         data.loadPlayerData(player.m_20148_());
      }

      return data;
   }

   private void loadPlayerData(UUID id) {
      saveExecutor.submit(() -> {
         try {
            String filename = id.toString();
            if (filename.isEmpty()) {
               filename = "noplayername";
            }

            filename = filename + ".dat";

            try {
               File file = new File(MorePlayerModels.dir, filename);
               if (!file.exists()) {
                  return;
               }

               CompoundTag compound = NbtIo.m_128939_(new FileInputStream(file));
               this.readFromNBT(compound);
               return;
            } catch (Exception var6) {
               LogWriter.except(var6);

               try {
                  File file = new File(MorePlayerModels.dir, filename + "_old");
                  if (file.exists()) {
                     return;
                  }

                  CompoundTag compound = NbtIo.m_128939_(new FileInputStream(file));
                  this.readFromNBT(compound);
               } catch (Exception var5) {
                  LogWriter.except(var5);
               }
            }
         } catch (Exception var7) {
            LogWriter.except(var7);
         }
      });
   }

   public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
      return capability == MODELDATA_CAPABILITY ? this.instance.cast() : LazyOptional.empty();
   }
}
