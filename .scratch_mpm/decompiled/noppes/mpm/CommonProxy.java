package noppes.mpm;

import net.minecraft.world.entity.player.Player;
import noppes.mpm.client.parts.MpmPartData;

public class CommonProxy {
   public void load() {
   }

   public void postLoad() {
   }

   public void executor(Player player, Runnable runnable) {
      player.m_20194_().execute(runnable);
   }

   public void createMpmPartData(MpmPartData data) {
   }
}
