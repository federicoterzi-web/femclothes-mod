package noppes.mpm.client.parts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import noppes.mpm.constants.BodyPart;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.constants.PartBehaviorType;
import noppes.mpm.constants.PartRenderType;
import noppes.mpm.shared.util.NopVector3f;
import noppes.mpm.util.MpmException;

public class MpmPart {
   public boolean isEnabled;
   public ResourceLocation id;
   public ResourceLocation parentId;
   public String name;
   public ResourceLocation texture;
   public String menu;
   public PartRenderType renderType;
   public PartBehaviorType animationType;
   public BodyPart bodyPart;
   public List<BodyPart> hiddenParts;
   public NopVector3f translate = NopVector3f.ZERO;
   public NopVector3f scale = NopVector3f.ZERO;
   public NopVector3f rotatePoint = NopVector3f.ZERO;
   public NopVector3f rotate = NopVector3f.ZERO;
   public int previewRotation = 45;
   public boolean disableCustomTextures;
   public boolean defaultUsePlayerSkins;
   public String author;
   public Map<EnumAnimation, ModelPartWrapper[]> animations = new HashMap<>();

   public void load(List<AnimationContainer> animationsList, MpmPart part) {
      if (animationsList != null && animationsList.size() != 0) {
         animationsList.forEach(container -> {
            ModelPartWrapper[] list = this.animations.computeIfAbsent(container.animation, k -> new ModelPartWrapper[0]);
            ModelPartWrapper model = part.getPart(container.part);
            if (model != null) {
               if (container.additional) {
                  container = container.copy();

                  for (int i = 0; i < container.actualLength; i++) {
                     if (i < container.length) {
                        if (container.hasTranslate) {
                           container.translates[i] = container.translates[i].add(model.oriPos);
                        }

                        if (container.hasRotation) {
                           container.rotations[i] = container.rotations[i].add(model.oriRot);
                        }
                     } else {
                        if (container.hasTranslate) {
                           container.translates[i] = container.translates[container.length - i % container.length - 2];
                        }

                        if (container.hasRotation) {
                           container.rotations[i] = container.rotations[container.length - i % container.length - 2];
                        }
                     }
                  }
               }

               model.animations.put(container.animation, container);
               list = Arrays.copyOf(list, list.length + 1);
               list[list.length - 1] = model;
               this.animations.put(container.animation, list);
            }
         });
      }
   }

   public static List<AnimationContainer> loadAnimations(JsonObject json) {
      List<AnimationContainer> list = new ArrayList<>();
      if (json != null && json.size() != 0) {
         for (Entry<String, JsonElement> entry : json.entrySet()) {
            try {
               EnumAnimation animation = EnumAnimation.valueOf(entry.getKey().toUpperCase());
               JsonObject animationData = entry.getValue().getAsJsonObject();
               int length = animationData.get("animation_length").getAsInt();
               float speed = animationData.get("animation_speed").getAsFloat();
               boolean loop = animationData.has("loop") && animationData.get("loop").getAsBoolean();
               boolean additional = animationData.has("additional") && animationData.get("additional").getAsBoolean();

               for (Entry<String, JsonElement> bone : animationData.get("bones").getAsJsonObject().entrySet()) {
                  list.removeIf(c -> c.animation == animation && c.part.equals(bone.getKey()));
                  AnimationContainer con = new AnimationContainer(animation, bone.getKey(), length, speed, additional, loop);
                  list.add(con);
                  JsonObject boneAnimation = bone.getValue().getAsJsonObject();
                  if (boneAnimation.has("rotation")) {
                     con.hasRotation = true;
                     JsonArray rotArray = boneAnimation.get("rotation").getAsJsonArray();

                     for (int i = 0; i < con.actualLength; i++) {
                        if (i < length) {
                           con.rotations[i] = MpmPartReader.jsonVector3f(rotArray.get(i)).mul((float) (Math.PI / 180.0));
                        } else {
                           con.rotations[i] = con.rotations[length - i % length - 2];
                        }
                     }
                  }

                  if (boneAnimation.has("translate")) {
                     con.hasTranslate = true;
                     JsonArray tranArray = boneAnimation.get("translate").getAsJsonArray();

                     for (int ix = 0; ix < con.actualLength; ix++) {
                        if (ix < length) {
                           con.translates[ix] = MpmPartReader.jsonVector3f(tranArray.get(ix));
                        } else {
                           con.translates[ix] = con.translates[length - ix % length - 2];
                        }
                     }
                  } else {
                     for (int ixx = 0; ixx < con.actualLength; ixx++) {
                        con.translates[ixx] = NopVector3f.ZERO;
                     }
                  }
               }
            } catch (Exception var16) {
               throw new MpmException(var16, "Error in animation: " + entry.getKey());
            }
         }

         return list;
      } else {
         return list;
      }
   }

   public void load(JsonObject renderData) {
   }

   public ModelPartWrapper getPart(String name) {
      return null;
   }
}
