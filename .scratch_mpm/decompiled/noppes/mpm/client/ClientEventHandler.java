package noppes.mpm.client;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import net.minecraftforge.event.PlayLevelSoundEvent.AtEntity;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import noppes.mpm.ModelData;
import noppes.mpm.ModelEyeData;
import noppes.mpm.ModelPartData;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.ServerTickHandler;
import noppes.mpm.client.fx.EntityEnderFX;
import noppes.mpm.client.gui.GuiCreationScreenInterface;
import noppes.mpm.client.gui.GuiMPM;
import noppes.mpm.client.parts.MpmPartData;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.mixin.LivingEntityMixin;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.server.PacketAnimationUpdate;
import noppes.mpm.packets.server.PacketPing;
import noppes.mpm.sync.WebApi;
import noppes.mpm.util.MPMEntityUtil;

public class ClientEventHandler {
   public static float partialTick = 0.0F;
   private long lastAltClick = 0L;
   private boolean altIsPressed = false;
   private Level prevWorld;
   private static final Predicate<Player> playerSelector = Predicates.and(new Predicate[]{new Predicate<Player>() {
      final double range = 6400.0;

      public boolean apply(Player entity) {
         return entity != Minecraft.m_91087_().f_91074_ && entity.m_20280_(Minecraft.m_91087_().f_91074_) <= 6400.0;
      }
   }});
   public static List<Player> playerList;
   private static final ResourceLocation female_death = new ResourceLocation("moreplayermodels:human.female.death");
   private static final ResourceLocation female_hurt = new ResourceLocation("moreplayermodels:human.female.hurt");
   private static final ResourceLocation female_attack = new ResourceLocation("moreplayermodels:human.female.attack");
   private static final ResourceLocation male_death = new ResourceLocation("moreplayermodels:human.male.death");
   private static final ResourceLocation male_hurt = new ResourceLocation("moreplayermodels:human.male.hurt");
   private static final ResourceLocation male_attack = new ResourceLocation("moreplayermodels:human.male.attack");
   private static final ResourceLocation goblin_death = new ResourceLocation("moreplayermodels:goblin.male.death");
   private static final ResourceLocation goblin_hurt = new ResourceLocation("moreplayermodels:goblin.male.hurt");
   private static final ResourceLocation goblin_attack = new ResourceLocation("moreplayermodels:goblin.male.attack");
   public static MpmCamera camera = new MpmCamera();

   @SubscribeEvent
   public void onPlaySoundAtEntity(AtEntity event) {
      if (event.getEntity() instanceof LocalPlayer player && event.getSound() != null) {
         ModelData data = ModelData.get(player);
         if (data != null && data.soundType != 0) {
            ResourceLocation sound = null;
            if (event.getSound() == SoundEvents.f_12323_ && !player.m_21224_() && !((LivingEntityMixin)player).isDead()) {
               if (data.soundType == 1) {
                  sound = female_hurt;
               } else if (data.soundType == 2) {
                  sound = male_hurt;
               } else if (data.soundType == 3) {
                  sound = goblin_hurt;
               }
            }

            if (event.getSound() == SoundEvents.f_12322_) {
               if (data.soundType == 1) {
                  sound = female_death;
               } else if (data.soundType == 2) {
                  sound = male_death;
               } else if (data.soundType == 3) {
                  sound = goblin_death;
               }
            }

            if (sound != null) {
               event.setSound(BuiltInRegistries.f_256894_.m_263177_(SoundEvent.m_262824_(sound)));
            }
         }
      }
   }

   @SubscribeEvent
   public void onAttack(LivingAttackEvent event) {
      if (!(event.getAmount() < 1.0F)
         && event.getSource().m_269415_().f_268677_().equals("player")
         && event.getSource().m_7639_() instanceof LocalPlayer player) {
         if (!(event.getEntity().m_21223_() < 0.0F) && !(player.m_21216_() > 20.0F / 2.0F)) {
            ModelData data = ModelData.get(player);
            if (data != null && data.soundType != 0) {
               ResourceLocation sound = null;
               if (data.soundType == 1) {
                  sound = female_attack;
               } else if (data.soundType == 2) {
                  sound = male_attack;
               } else if (data.soundType == 3) {
                  sound = goblin_attack;
               }

               if (sound != null) {
                  float pitch = (player.m_217043_().m_188501_() - player.m_217043_().m_188501_()) * 0.2F + 1.0F;
                  player.m_9236_().m_5594_(player, player.m_20183_(), SoundEvent.m_262824_(sound), SoundSource.PLAYERS, 0.9876543F, pitch);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onKey(Key event) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc != null && mc.f_91074_ != null) {
         if (MpmKeys.Screen.m_90857_()) {
            ModelData data = ModelData.get(mc.f_91074_);
            data.setAnimation(EnumAnimation.NONE);
            if (mc.f_91080_ == null) {
               mc.m_91152_(new GuiMPM());
            }
         }

         if (mc.f_91080_ == null) {
         }

         if (mc.m_91302_()) {
            if (MpmKeys.MPM1.m_90857_()) {
               processAnimation(MorePlayerModels.button1);
            }

            if (MpmKeys.MPM2.m_90857_()) {
               processAnimation(MorePlayerModels.button2);
            }

            if (MpmKeys.MPM3.m_90857_()) {
               processAnimation(MorePlayerModels.button3);
            }

            if (MpmKeys.MPM4.m_90857_()) {
               processAnimation(MorePlayerModels.button4);
            }

            if (MpmKeys.MPM5.m_90857_()) {
               processAnimation(MorePlayerModels.button5);
            }

            com.mojang.blaze3d.platform.InputConstants.Key i = Type.KEYSYM.m_84895_(event.getKey());
            if (MpmKeys.Camera.m_90857_() && mc.f_91066_.m_92176_() == CameraType.THIRD_PERSON_FRONT) {
               long time = System.currentTimeMillis();
               if (!this.altIsPressed) {
                  if (time - this.lastAltClick < 400L) {
                     camera.reset();
                  } else {
                     camera.enable();
                     this.lastAltClick = time;
                  }
               }

               this.altIsPressed = true;
            } else if (this.altIsPressed) {
               this.altIsPressed = false;
            }
         }
      }
   }

   @SubscribeEvent
   public void onMouse(MouseScrollingEvent event) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.m_91302_() && mc.f_91063_.m_109153_().m_90594_() && camera.enabled && this.altIsPressed) {
         camera.cameraDistance = (float)(camera.cameraDistance - event.getScrollDelta());
         if (camera.cameraDistance > 14.0F) {
            camera.cameraDistance = 14.0F;
         } else if (camera.cameraDistance < 1.0F) {
            camera.cameraDistance = 1.0F;
         }

         event.setCanceled(true);
      }
   }

   public static void processAnimation(int type) {
      if (type >= 0) {
         PacketAnimationUpdate.setAnimation(Minecraft.m_91087_().f_91074_, EnumAnimation.values()[type]);
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent event) {
      if (event.phase == Phase.START) {
         partialTick = event.renderTickTime;
      }

      camera.update(event.phase == Phase.START);
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.side != LogicalSide.SERVER && event.phase != Phase.START) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91073_ == null) {
            if (this.prevWorld != null) {
               this.prevWorld = null;
               SkinUtil.lastSkinTick = -20L;
            }
         } else {
            if (this.prevWorld != mc.f_91073_) {
               GuiCreationScreenInterface.Message = "message.noserver";
               ModelData data = ModelData.get(mc.f_91074_);
               MorePlayerModels.HasServerSide = true;
               Packets.sendServer(new PacketPing(MorePlayerModels.Version, data.writeToNBT()));
               MorePlayerModels.HasServerSide = false;
               this.prevWorld = mc.f_91073_;
            }

            SkinUtil.lastSkinTick++;
            if (mc.f_91073_.m_6106_().m_6793_() % 20L == 0L) {
               playerList = mc.f_91073_.m_6907_().stream().filter(playerSelector).collect(Collectors.toList());
               WebApi.instance.run();
            }
         }
      }
   }

   @SubscribeEvent
   public void onPlayerTick(PlayerTickEvent event) {
      if (event.side != LogicalSide.SERVER && event.phase != Phase.START) {
         ClientProxy.data = null;
         Player player = event.player;
         ModelData data = ModelData.get(player);
         LivingEntity entity = data.getEntity(player);
         Minecraft mc = Minecraft.m_91087_();
         if (entity != null) {
            MPMEntityUtil.copy(player, entity);
         }

         if (!MorePlayerModels.HasServerSide && entity == null) {
            for (MpmPartData pd : data.mpmParts) {
               if (pd instanceof ModelEyeData) {
                  ((ModelEyeData)pd).update(player);
               }
            }
         }

         if (data.inLove > 0) {
            data.inLove--;
            if (player.m_217043_().m_188499_()) {
               double d0 = player.m_217043_().m_188583_() * 0.02;
               double d1 = player.m_217043_().m_188583_() * 0.02;
               double d2 = player.m_217043_().m_188583_() * 0.02;
               player.m_9236_()
                  .m_7106_(
                     ParticleTypes.f_123750_,
                     player.m_20185_() + player.m_217043_().m_188501_() * player.m_20205_() * 2.0F - player.m_20205_(),
                     player.m_20186_() + 0.5 + player.m_217043_().m_188501_() * player.m_20206_(),
                     player.m_20189_() + player.m_217043_().m_188501_() * player.m_20205_() * 2.0F - player.m_20205_(),
                     d0,
                     d1,
                     d2
                  );
            }
         }

         if (data.animation == EnumAnimation.CRY) {
            float f1 = player.m_146909_() * (float) Math.PI / 180.0F;
            float dx = -Mth.m_14031_(f1);
            float dz = Mth.m_14089_(f1);
            float width = entity == null ? player.m_20205_() : entity.m_20205_();

            for (int i = 0; i < 10.0F; i++) {
               float f2 = (player.m_217043_().m_188501_() - 0.5F) * width * 0.5F + dx * 0.15F;
               float f3 = (player.m_217043_().m_188501_() - 0.5F) * width * 0.5F + dz * 0.15F;
               player.m_9236_()
                  .m_7106_(
                     ParticleTypes.f_123769_,
                     player.m_20185_() + f2,
                     player.m_20186_() - data.getBodyY() + 1.1F - player.m_6049_(),
                     player.m_20189_() + f3,
                     1.0E-25F,
                     0.0,
                     1.0E-25F
                  );
            }
         }

         ServerTickHandler.checkMovementAnimation(player, data);
         if (data.animation != EnumAnimation.NONE) {
            ServerTickHandler.checkAnimation(player, data);
         }

         if (data.animation == EnumAnimation.DEATH) {
            if (player.f_20919_ == 0) {
               player.m_5496_(SoundEvents.f_11915_, 1.0F, 1.0F);
            }

            if (player.f_20919_ < 19) {
               player.f_20919_++;
            }
         }

         if (data.prevAnimation != data.animation && data.prevAnimation == EnumAnimation.DEATH && !player.m_21224_()) {
            player.f_20919_ = 0;
         }

         data.prevMoveAnimation = data.moveAnimation;
         data.prevAnimation = data.animation;
         data.prevPosX = player.m_20185_();
         data.prevPosY = player.m_20186_();
         data.prevPosZ = player.m_20189_();
         ModelPartData particles = null;
         if (particles != null) {
            this.spawnParticles(player, data, particles);
         }
      }
   }

   private void spawnParticles(Player player, ModelData data, ModelPartData particles) {
      if (MorePlayerModels.EnableParticles) {
         Minecraft minecraft = Minecraft.m_91087_();
         double height = player.m_6049_() + data.getBodyY();
         RandomSource rand = player.m_217043_();

         for (int i = 0; i < 2; i++) {
            EntityEnderFX fx = new EntityEnderFX(
               (AbstractClientPlayer)player,
               player.m_20208_(0.5),
               player.m_20187_() - height - 0.25,
               player.m_20262_(0.5),
               (rand.m_188500_() - 0.5) * 2.0,
               -rand.m_188500_(),
               (rand.m_188500_() - 0.5) * 2.0,
               particles
            );
            minecraft.f_91061_.m_107344_(fx);
         }
      }
   }
}
