package noppes.mpm.shared.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class AssetsFinder {
   public static List<ResourceLocation> find(String root, String type) {
      return new ArrayList<>(Minecraft.m_91087_().m_91098_().m_214159_(root, r -> r.m_135815_().endsWith(type)).keySet());
   }
}
