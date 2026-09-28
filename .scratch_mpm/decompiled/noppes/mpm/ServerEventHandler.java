package noppes.mpm;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.NameFormat;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.mpm.commands.CommandAnimations;
import noppes.mpm.commands.CommandMPM;
import noppes.mpm.commands.CommandParticles;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.client.PacketBackItemUpdate;
import noppes.mpm.packets.client.PacketChatEvent;
import noppes.mpm.packets.client.PacketPlayerDataSend;

public class ServerEventHandler {
   private static final ResourceLocation key = new ResourceLocation("moreplayermodels", "modeldata");

   @SubscribeEvent
   public void chat(ServerChatEvent event) {
      Packets.sendAll(new PacketChatEvent(event.getPlayer().m_20148_(), event.getMessage()));
   }

   @SubscribeEvent
   public void playerTracking(StartTracking event) {
      if (event.getTarget() instanceof Player) {
         Player target = (Player)event.getTarget();
         ServerPlayer player = (ServerPlayer)event.getEntity();
         ModelData data = ModelData.get(target);
         Packets.sendDelayed(player, new PacketPlayerDataSend(target.m_20148_(), data.writeToNBT()), 100);
         Packets.sendDelayed(player, new PacketBackItemUpdate(target.m_20148_(), (ItemStack)player.m_150109_().f_35974_.get(0)), 100);
      }
   }

   @SubscribeEvent
   public void onNameSet(NameFormat event) {
      ModelData data = ModelData.get(event.getEntity());
      if (!data.displayName.isEmpty()) {
         event.setDisplayname(Component.m_237115_(data.displayName.replace('&', Character.toChars(167)[0])));
      }
   }

   @SubscribeEvent
   public void attach(AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof Player) {
         event.addCapability(key, new ModelData());
      }
   }

   @SubscribeEvent
   public void registerCommands(RegisterCommandsEvent e) {
      CommandParticles.register(e.getDispatcher());
      CommandAnimations.register(e.getDispatcher());
      CommandMPM.register(e.getDispatcher(), e.getBuildContext());
   }
}
