package noppes.mpm.client.parts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import noppes.mpm.mixin.ModelPartMixin;
import noppes.mpm.shared.client.model.Model2DRenderer;
import noppes.mpm.shared.util.NopVector2i;
import noppes.mpm.shared.util.NopVector3f;

public class MpmPartBedrock extends MpmPartAbstractClient {
   public final Map<ResourceLocation, Model2DRenderer> playerModels = new HashMap<>();
   private ModelPart model;
   private ModelPartMixin modelMixin;
   public NopVector2i textureSize = NopVector2i.ZERO;

   @Override
   public void render(MpmPartData data, PoseStack mStack, VertexConsumer c, int lightmapUV, AbstractClientPlayer player) {
      mStack.m_85836_();
      if (this.model != null) {
         Map<String, ModelPart> children = this.modelMixin.getChildren();
         this.model.m_104299_(mStack);
         float f = 0.0625F;
         mStack.m_252880_(-this.rotatePoint.x * f, -this.rotatePoint.y * f, -this.rotatePoint.z * f);
         mStack.m_85841_(this.scale.x, this.scale.y, this.scale.z);

         for (ModelPart modelpart : children.values()) {
            modelpart.m_104306_(mStack, c, lightmapUV, OverlayTexture.f_118083_, data.color.x, data.color.y, data.color.z, 1.0F);
         }
      }

      mStack.m_85849_();
   }

   @Override
   public void load(JsonObject renderData) {
      if (renderData != null && renderData.size() > 0) {
         MeshDefinition meshdefinition = new MeshDefinition();
         PartDefinition root = meshdefinition.m_171576_();
         JsonObject ob = renderData.get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject();
         JsonObject description = ob.get("description").getAsJsonObject();
         this.textureSize = new NopVector2i(description.get("texture_width").getAsInt(), description.get("texture_height").getAsInt());
         JsonArray bones = ob.get("bones").getAsJsonArray();
         Map<String, PartDefinition> namedParts = new HashMap<>();
         Map<String, NopVector3f> parentPivots = new HashMap<>();
         Map<String, ModelPartWrapper> defaultPose = new HashMap<>();

         for (int i = 0; i < bones.size(); i++) {
            JsonObject bone = bones.get(i).getAsJsonObject();
            String name = bone.get("name").getAsString();
            String pName = bone.has("parent") ? bone.get("parent").getAsString() : null;
            PartDefinition parent = pName != null && namedParts.containsKey(pName) ? namedParts.get(pName) : root;
            NopVector3f ppivot = parentPivots.containsKey(pName) ? parentPivots.get(pName) : NopVector3f.ZERO;
            NopVector3f pivot = MpmPartReader.jsonVector3f(bone.get("pivot"));
            parentPivots.put(name, pivot);
            NopVector3f rotation = MpmPartReader.jsonVector3f(bone.get("rotation")).mul((float) (Math.PI / 180.0));
            PartPose pose = PartPose.m_171423_(pivot.x - ppivot.x, ppivot.y - pivot.y, pivot.z - ppivot.z, rotation.x, rotation.y, rotation.z);
            PartDefinition partDef = parent.m_171599_(name, CubeListBuilder.m_171558_(), pose);
            defaultPose.put(name, new ModelPartWrapper(name, (ModelPart)null, new NopVector3f(pose.f_171405_, pose.f_171406_, pose.f_171407_), rotation));
            if (bone.has("cubes")) {
               JsonArray cubes = bone.get("cubes").getAsJsonArray();

               for (int j = 0; j < cubes.size(); j++) {
                  CubeListBuilder builder = CubeListBuilder.m_171558_();
                  JsonObject cube = cubes.get(j).getAsJsonObject();
                  NopVector2i uv = MpmPartReader.jsonVector2i(cube.get("uv"));
                  boolean mirror = cube.has("mirror") && cube.get("mirror").getAsBoolean();
                  NopVector3f cpivot = MpmPartReader.jsonVector3f(cube.get("pivot"));
                  rotation = MpmPartReader.jsonVector3f(cube.get("rotation")).mul((float) (Math.PI / 180.0));
                  NopVector3f origin = MpmPartReader.jsonVector3f(cube.get("origin"));
                  NopVector3f size = MpmPartReader.jsonVector3f(cube.get("size"));
                  CubeDeformation deform = cube.has("inflate") ? new CubeDeformation(cube.get("inflate").getAsFloat()) : CubeDeformation.f_171458_;
                  builder.m_171514_(uv.x, uv.y)
                     .m_171555_(mirror)
                     .m_171488_(origin.x - cpivot.x, cpivot.y - size.y - origin.y, origin.z - cpivot.z, size.x, size.y, size.z, deform);
                  partDef.m_171599_(
                     "cube_" + name + j,
                     builder,
                     PartPose.m_171423_(cpivot.x - pivot.x, pivot.y - cpivot.y, cpivot.z - pivot.z, rotation.x, rotation.y, rotation.z)
                  );
               }
            }

            namedParts.put(name, partDef);
         }

         this.model = LayerDefinition.m_171565_(meshdefinition, description.get("texture_width").getAsInt(), description.get("texture_height").getAsInt())
            .m_171564_();
         this.model.m_104227_(this.translate.x, this.translate.y, this.translate.z);

         for (Entry<String, ModelPartWrapper> entry : defaultPose.entrySet()) {
            entry.getValue().mcPart = this.getChild(this.model, entry.getKey());
         }

         defaultPose.put(null, new ModelPartWrapper(null, this.model, this.translate, this.rotate));
         this.defaultPose = defaultPose;
         NopVector3f rotation = this.rotate.mul((float) (Math.PI / 180.0));
         this.model.m_171327_(rotation.x, rotation.y, rotation.z);
         this.modelMixin = (ModelPartMixin)this.model;
      }
   }

   private ModelPart getChild(ModelPart root, String name) {
      Map<String, ModelPart> children = ((ModelPartMixin)root).getChildren();
      if (children.containsKey(name)) {
         return children.get(name);
      } else {
         for (ModelPart child : children.values()) {
            ModelPart p = this.getChild(child, name);
            if (p != null) {
               return p;
            }
         }

         return null;
      }
   }
}
