package noppes.mpm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import noppes.mpm.ModelData;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.client.PacketAnimationStart;

public class CommandAnimations {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      animationCommand(dispatcher, "bow", EnumAnimation.BOW);
      animationCommand(dispatcher, "crawl", EnumAnimation.CRAWL);
      animationCommand(dispatcher, "cry", EnumAnimation.CRY);
      animationCommand(dispatcher, "dance", EnumAnimation.DANCE);
      animationCommand(dispatcher, "death", EnumAnimation.DEATH);
      animationCommand(dispatcher, "hug", EnumAnimation.HUG);
      animationCommand(dispatcher, "no", EnumAnimation.NO);
      animationCommand(dispatcher, "point", EnumAnimation.POINT);
      animationCommand(dispatcher, "sit", EnumAnimation.SIT);
      animationCommand(dispatcher, "sleep", EnumAnimation.SLEEP);
      animationCommand(dispatcher, "wag", EnumAnimation.WAG);
      animationCommand(dispatcher, "wave", EnumAnimation.WAVE);
      animationCommand(dispatcher, "yes", EnumAnimation.YES);
   }

   private static void animationCommand(CommandDispatcher<CommandSourceStack> dispatcher, String command, EnumAnimation animation) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(command).requires(source -> source.m_6761_(0))).executes(context -> {
            ServerPlayer player = ((CommandSourceStack)context.getSource()).m_81375_();
            ModelData data = ModelData.get(player);
            EnumAnimation ani = animation;
            if (data.animation == animation) {
               ani = EnumAnimation.NONE;
            } else if (data.moveAnimation == animation) {
               ani = EnumAnimation.IDLE;
            }

            data.setAnimation(ani);
            Packets.sendNearby(player, new PacketAnimationStart(player.m_20148_(), ani));
            return 1;
         })
      );
   }
}
