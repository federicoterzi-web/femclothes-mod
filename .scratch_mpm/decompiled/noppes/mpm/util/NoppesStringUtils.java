package noppes.mpm.util;

import net.minecraft.resources.ResourceLocation;

public class NoppesStringUtils {
   public static boolean areEqual(String s1, String s2) {
      if (s1 == s2) {
         return true;
      } else {
         return s1 != null && s2 != null ? s1.equalsIgnoreCase(s2) : false;
      }
   }

   public static boolean areEqual(ResourceLocation s1, ResourceLocation s2) {
      if (s1 == s2) {
         return true;
      } else {
         return s1 != null && s2 != null ? s1.toString().equalsIgnoreCase(s2.toString()) : false;
      }
   }

   public static boolean empty(String s) {
      return s == null || s.trim().isEmpty();
   }
}
