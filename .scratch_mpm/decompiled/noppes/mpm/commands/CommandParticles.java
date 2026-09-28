package noppes.mpm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.client.PacketParticleAngry;
import noppes.mpm.packets.client.PacketParticleLove;
import noppes.mpm.packets.client.PacketParticleNote;

public class CommandParticles {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("angry").requires(source -> source.m_6761_(0))).executes(context -> {
            ServerPlayer player = ((CommandSourceStack)context.getSource()).m_81375_();
            Packets.sendNearby(player, new PacketParticleAngry(player.m_20148_()));
            return 1;
         })
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("love").requires(source -> source.m_6761_(0))).executes(context -> {
            ServerPlayer player = ((CommandSourceStack)context.getSource()).m_81375_();
            Packets.sendNearby(player, new PacketParticleLove(player.m_20148_()));
            return 1;
         })
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("sing").requires(source -> source.m_6761_(0)))
               .executes(context -> {
                  ServerPlayer player = ((CommandSourceStack)context.getSource()).m_81375_();
                  playNote(player, player.m_217043_().m_188503_(25));
                  return 1;
               }))
            .then(Commands.m_82129_("note", IntegerArgumentType.integer(1)).executes(context -> {
               ServerPlayer player = ((CommandSourceStack)context.getSource()).m_81375_();
               playNote(player, IntegerArgumentType.getInteger(context, "note"));
               return 1;
            }))
      );
   }

   private static void playNote(ServerPlayer player, int note) {
      float pitch = (float)Math.pow(2.0, (note - 12) / 12.0);
      player.m_9236_()
         .m_6263_(null, player.m_20185_(), player.m_20186_(), player.m_20189_(), (SoundEvent)SoundEvents.f_12214_.m_203334_(), SoundSource.PLAYERS, 3.0F, pitch);
      Packets.sendNearby(player, new PacketParticleNote(player.m_20148_(), note));
   }
}
