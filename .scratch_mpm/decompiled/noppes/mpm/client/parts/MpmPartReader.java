package noppes.mpm.client.parts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import noppes.mpm.LogWriter;
import noppes.mpm.ModelEyeData;
import noppes.mpm.constants.BodyPart;
import noppes.mpm.constants.PartBehaviorType;
import noppes.mpm.constants.PartRenderType;
import noppes.mpm.shared.client.AssetsFinder;
import noppes.mpm.shared.util.NopVector2i;
import noppes.mpm.shared.util.NopVector3f;
import noppes.mpm.shared.util.NopVector3i;
import noppes.mpm.util.MpmException;

public class MpmPartReader {
   public static Map<ResourceLocation, MpmPart> PARTS = new HashMap<>();
   public static Map<String, List<AnimationContainer>> ANIMATIONS = new HashMap<>();

   public static void reload() {
      Map<String, List<AnimationContainer>> mapA = new HashMap<>();

      for (ResourceLocation loc : AssetsFinder.find("animations", ".json")) {
         try {
            Resource r = (Resource)Minecraft.m_91087_().m_91098_().m_213713_(loc).get();
            JsonObject root = JsonParser.parseReader(r.m_215508_()).getAsJsonObject();
            mapA.put(loc.m_135815_().substring(11, loc.m_135815_().length() - 5), MpmPart.loadAnimations(root));
         } catch (Throwable var6) {
            LogWriter.error("Error in " + loc.toString(), var6);
         }
      }

      ANIMATIONS = mapA;
      Map<ResourceLocation, MpmPart> map = new HashMap<>();

      for (ResourceLocation loc : AssetsFinder.find("parts", ".json")) {
         MpmPart part = loadPart(loc);
         if (part != null) {
            map.put(loc, part);
         }
      }

      PARTS = map;
      PARTS.put(ModelEyeData.RESOURCE, new MpmPartEyes(0, ModelEyeData.RESOURCE));
      PARTS.put(ModelEyeData.RESOURCE_RIGHT, new MpmPartEyes(1, ModelEyeData.RESOURCE_RIGHT));
      PARTS.put(ModelEyeData.RESOURCE_LEFT, new MpmPartEyes(2, ModelEyeData.RESOURCE_LEFT));

      for (Entry<ResourceLocation, MpmPart> entry : PARTS.entrySet()) {
         if (entry.getValue().parentId != null && !PARTS.containsKey(entry.getValue().parentId)) {
            LogWriter.error("Error in " + entry.getKey().toString() + " - Unable to find parent " + entry.getValue().parentId);
            Notify(Component.m_237113_("Error in " + entry.getKey().toString() + " - Unable to find parent " + entry.getValue().parentId));
         }
      }
   }

   private static MpmPart loadPart(ResourceLocation location) {
      try {
         Resource r = (Resource)Minecraft.m_91087_().m_91098_().m_213713_(location).get();
         JsonObject root = JsonParser.parseReader(r.m_215508_()).getAsJsonObject();
         PartRenderType renderType = PartRenderType.valueOf(getRequiredString(root, "render_type").toUpperCase());
         MpmPart part = new MpmPart();
         if (renderType == PartRenderType.BEDROCK) {
            part = new MpmPartBedrock();
         }

         if (renderType == PartRenderType.SIMPLE) {
            part = new MpmPartSimple();
         }

         part.isEnabled = !root.has("enabled") || root.get("enabled").getAsBoolean();
         part.id = location;
         part.name = getRequiredString(root, "name");
         part.texture = root.has("texture") ? new ResourceLocation(root.get("texture").getAsString()) : null;
         part.menu = getRequiredString(root, "menu");
         part.author = getRequiredString(root, "author");
         part.translate = jsonVector3f(root.get("translate"));
         part.scale = jsonVector3fOrOne(root.get("scale"));
         part.rotatePoint = jsonVector3f(root.get("rotate_offset"));
         part.rotate = jsonVector3f(root.get("rotate"));
         part.previewRotation = root.get("preview_rotation").getAsInt();
         part.hiddenParts = jsonEnumList(BodyPart.class, root.get("hidden_parts"));
         part.disableCustomTextures = root.has("disable_custom_textures") && root.get("disable_custom_textures").getAsBoolean();
         part.defaultUsePlayerSkins = root.has("default_use_player_skins") && root.get("default_use_player_skins").getAsBoolean();
         part.renderType = renderType;
         part.bodyPart = BodyPart.valueOf(getRequiredString(root, "body_part").toUpperCase());
         part.load(root.has("render_data") ? root.get("render_data").getAsJsonObject() : null);
         if (root.has("parent")) {
            part.parentId = new ResourceLocation(root.get("parent").getAsString());
         }

         part.animationType = root.has("animation_type")
            ? PartBehaviorType.valueOf(root.get("animation_type").getAsString().toUpperCase())
            : PartBehaviorType.NONE;
         if (root.has("animation_inherit")) {
            String inpart = root.get("animation_inherit").getAsString().toLowerCase();
            if (!ANIMATIONS.containsKey(inpart)) {
               throw new MpmException("Unknown animation inherit: " + inpart);
            }

            part.load(ANIMATIONS.get(inpart), part);
         }

         if (root.has("animation_data")) {
            part.load(MpmPart.loadAnimations(root.get("animation_data").getAsJsonObject()), part);
         }

         return part;
      } catch (Throwable var6) {
         LogWriter.error("Error in " + location.toString(), var6);
         Notify(Component.m_237113_("Error in " + location + " - " + var6.getMessage()));
         return null;
      }
   }

   public static String getRequiredString(JsonObject root, String part) {
      if (!root.has(part)) {
         throw new MpmException("Can't fine " + part);
      } else {
         return root.get(part).getAsString();
      }
   }

   public static <T extends Enum> List<T> jsonEnumList(Class<T> type, JsonElement el) {
      List<T> list = new ArrayList<>();
      if (el != null && el.isJsonArray()) {
         JsonArray arr = el.getAsJsonArray();

         for (int i = 0; i < arr.size(); i++) {
            list.add(Enum.valueOf(type, arr.get(i).getAsString().toUpperCase()));
         }

         return list;
      } else {
         return list;
      }
   }

   public static NopVector2i jsonVector2i(JsonElement el) {
      if (el != null && el.isJsonArray()) {
         JsonArray arr = el.getAsJsonArray();
         int[] r = new int[arr.size()];

         for (int i = 0; i < arr.size(); i++) {
            r[i] = arr.get(i).getAsInt();
         }

         return new NopVector2i(r);
      } else {
         return NopVector2i.ZERO;
      }
   }

   public static NopVector3i jsonVector3i(JsonElement el) {
      if (el != null && el.isJsonArray()) {
         JsonArray arr = el.getAsJsonArray();
         int[] r = new int[arr.size()];

         for (int i = 0; i < arr.size(); i++) {
            r[i] = arr.get(i).getAsInt();
         }

         return new NopVector3i(r);
      } else {
         return NopVector3i.ZERO;
      }
   }

   public static NopVector3f jsonVector3f(JsonElement el) {
      if (el == null) {
         return NopVector3f.ZERO;
      } else {
         JsonArray arr = el.getAsJsonArray();
         float[] r = new float[arr.size()];

         for (int i = 0; i < arr.size(); i++) {
            r[i] = arr.get(i).getAsFloat();
         }

         return new NopVector3f(r);
      }
   }

   public static NopVector3f jsonVector3fOrOne(JsonElement el) {
      if (el == null) {
         return NopVector3f.ONE;
      } else {
         JsonArray arr = el.getAsJsonArray();
         float[] r = new float[arr.size()];

         for (int i = 0; i < arr.size(); i++) {
            r[i] = arr.get(i).getAsFloat();
         }

         return new NopVector3f(r);
      }
   }

   public static void Notify(MutableComponent message) {
      Component chatcomponenttranslation = message.m_130944_(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
      if (Minecraft.m_91087_().f_91074_ != null && Minecraft.m_91087_().f_91074_.m_6102_()) {
         Minecraft.m_91087_().f_91074_.m_213846_(chatcomponenttranslation);
      }
   }
}
