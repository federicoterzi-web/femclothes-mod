package noppes.mpm.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class VersionChecker extends Thread {
   @Override
   public void run() {
      try {
         Player player = Minecraft.m_91087_().f_91074_;
      } catch (NoSuchMethodError var5) {
         return;
      }

      LocalPlayer var6;
      while ((var6 = Minecraft.m_91087_().f_91074_) == null) {
         try {
            Thread.sleep(2000L);
         } catch (InterruptedException var4) {
            var4.printStackTrace();
         }
      }

      Component s = MpmKeys.Screen.getKey().m_84875_().m_6879_().m_130940_(ChatFormatting.RED);
      Component message = Component.m_237113_("§2MorePlayerModels§f ").m_7220_(Component.m_237110_("message.startup", new Object[]{s}));
      var6.m_213846_(message);
   }
}
