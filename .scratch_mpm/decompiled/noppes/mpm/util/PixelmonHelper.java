package noppes.mpm.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import noppes.mpm.LogWriter;
import org.apache.logging.log4j.LogManager;

public class PixelmonHelper {
   public static boolean Enabled = false;
   private static Method getPixelmonModel = null;
   private static Class modelSetupClass;
   private static Method modelSetupMethod;

   public static void load() {
      Enabled = ModList.get().isLoaded("pixelmon");
      if (Enabled) {
         try {
            Class c = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.Entity2Client");
            getPixelmonModel = c.getMethod("getModel");
            modelSetupClass = Class.forName("com.pixelmonmod.pixelmon.client.models.PixelmonModelSmd");
            modelSetupMethod = modelSetupClass.getMethod("setupForRender", c);
         } catch (Exception var1) {
            LogWriter.except(var1);
            Enabled = false;
         }
      }
   }

   public static List<String> getPixelmonList() {
      List<String> list = new ArrayList<>();
      if (!Enabled) {
         return list;
      } else {
         try {
            Class c = Class.forName("com.pixelmonmod.pixelmon.enums.EnumPokemonModel");
            Object[] array = c.getEnumConstants();

            for (Object ob : array) {
               list.add(ob.toString());
            }
         } catch (Exception var7) {
            LogManager.getLogger().error("getPixelmonList", var7);
         }

         return list;
      }
   }

   public static boolean isPixelmon(Entity entity) {
      return !Enabled ? false : entity.m_6095_().m_20675_().toLowerCase().contains("pixelmon");
   }

   public static Object getModel(LivingEntity entity) {
      try {
         return getPixelmonModel.invoke(entity);
      } catch (Exception var2) {
         LogManager.getLogger().error("getModel", var2);
         return null;
      }
   }

   public static void setupModel(LivingEntity entity, Object model) {
      try {
         if (modelSetupClass.isAssignableFrom(model.getClass())) {
            modelSetupMethod.invoke(model, entity);
         }
      } catch (Exception var3) {
         LogManager.getLogger().error("setupModel", var3);
      }
   }

   public static String getName(LivingEntity entity) {
      if (Enabled && isPixelmon(entity)) {
         try {
            Method m = entity.getClass().getMethod("getName");
            return m.invoke(entity).toString();
         } catch (Exception var2) {
            LogManager.getLogger().error("getName", var2);
            return "";
         }
      } else {
         return "";
      }
   }

   public static void debug(LivingEntity entity) {
      if (Enabled && isPixelmon(entity)) {
         try {
            Method m = entity.getClass().getMethod("getModel");
            LocalPlayer player = Minecraft.m_91087_().f_91074_;
            player.m_213846_(Component.m_237113_((String)m.invoke(entity)));
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }
}
