package noppes.mpm.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import noppes.mpm.ModelData;
import noppes.mpm.ModelEyeData;
import noppes.mpm.client.gui.select.GuiTextureSelection;
import noppes.mpm.client.gui.util.GuiButtonBiDirectional;
import noppes.mpm.client.gui.util.GuiColorButton;
import noppes.mpm.client.gui.util.GuiCustomScroll;
import noppes.mpm.client.gui.util.GuiNPCInterface;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.GuiNpcButtonYesNo;
import noppes.mpm.client.gui.util.GuiNpcLabel;
import noppes.mpm.client.gui.util.GuiNpcSlider;
import noppes.mpm.client.gui.util.GuiNpcTextField;
import noppes.mpm.client.gui.util.ICustomScrollListener;
import noppes.mpm.client.gui.util.ITextfieldListener;
import noppes.mpm.client.layer.LayerParts;
import noppes.mpm.client.parts.ModelPartWrapper;
import noppes.mpm.client.parts.MpmPart;
import noppes.mpm.client.parts.MpmPartAbstractClient;
import noppes.mpm.client.parts.MpmPartData;
import noppes.mpm.client.parts.MpmPartDataClient;
import noppes.mpm.client.parts.MpmPartEyes;
import noppes.mpm.client.parts.MpmPartReader;
import noppes.mpm.constants.BodyPart;
import noppes.mpm.constants.PartBehaviorType;
import noppes.mpm.constants.PartRenderType;
import noppes.mpm.shared.util.ColorUtil;
import noppes.mpm.shared.util.NopVector2i;
import noppes.mpm.shared.util.NopVector3f;
import org.joml.Matrix4f;

public class GuiCreationNewParts extends GuiNPCInterface implements ITextfieldListener, ICustomScrollListener {
   private GuiCustomScroll scroll;
   private ModelData data;
   private ModelData renderData = new ModelData();
   private List<MpmPart> list = new ArrayList<>();
   private List<String> menus = new ArrayList<>();
   private static String active = "";
   private static PlayerModel biped;
   private List<GuiCreationNewParts.GuiMpmPart> guiParts = new ArrayList<>();
   private static final ResourceLocation blankSkin = new ResourceLocation("moreplayermodels", "textures/entity/gray.png");
   private static final ResourceLocation colorWheel = new ResourceLocation("moreplayermodels", "textures/gui/colorwheel.png");
   private static float rotation = 0.5F;

   public GuiCreationNewParts(ModelData data) {
      this.data = data;
      this.menus = MpmPartReader.PARTS.values().stream().map(p -> p.menu).distinct().collect(Collectors.toList());
      if (active.isEmpty()) {
         active = this.menus.get(0);
      }

      this.closeOnEsc = true;
      this.xSize = 420;
      this.ySize = 240;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      biped = new PlayerModel(this.f_96541_.m_167973_().m_171103_(ModelLayers.f_171162_), true);
      this.list = new ArrayList<>(
         MpmPartReader.PARTS
            .values()
            .stream()
            .sorted(Comparator.comparing(t -> t.id))
            .filter(t -> t.menu.equals(active) && t.parentId == null)
            .collect(Collectors.toList())
      );
      if (this.scroll == null) {
         this.scroll = new GuiCustomScroll(this, 0);
         this.scroll.setList(this.menus);
         this.scroll.disabledSearch();
      }

      this.scroll.guiLeft = this.guiLeft + 4;
      this.scroll.guiTop = this.guiTop + 4;
      this.scroll.setSize(100, 110);
      this.scroll.setSelected(active);
      this.addScroll(this.scroll);
      GuiNpcSlider slider = new GuiNpcSlider(this, 500, this.guiLeft + 4, this.guiTop + 216, 100, 20, rotation);
      slider.setListener(t -> {
         rotation = t.sliderValue;
         t.setString((int)(rotation * 360.0F) + "");
      });
      this.addSlider(slider);
      if (this.guiParts.isEmpty()) {
         for (int i = 0; i < this.list.size(); i++) {
            int column = i % 4;
            MpmPart part = this.list.get(i);
            GuiCreationNewParts.GuiMpmPart gui = new GuiCreationNewParts.GuiMpmPart(
               this.guiLeft + column * 70 + 110 + column, this.guiTop + i / 4 * 70 + 4, part
            );
            this.m_142416_(gui);
            this.guiParts.add(gui);
         }
      } else {
         for (int i = 0; i < this.guiParts.size(); i++) {
            int column = i % 4;
            GuiCreationNewParts.GuiMpmPart gui = this.guiParts.get(i);
            this.m_142416_(gui);
            gui.m_252865_(this.guiLeft + column * 70 + 110 + column);
            gui.m_253211_(this.guiTop + i / 4 * 70 + 4);
         }
      }

      this.addButton(new GuiNpcButton(66, this.guiLeft + 396, this.guiTop + 2, 20, 20, "X", b -> this.close()));
   }

   @Override
   public void m_88315_(GuiGraphics graphics, int i, int j, float f) {
      super.m_88315_(graphics, i, j, f);
      this.drawEntity(graphics, this.player, 50, 200, 1.4F, (int)(-rotation * 360.0F) + 180, this.guiLeft, this.guiTop);
      if (!this.hasSubGui()) {
         for (GuiCreationNewParts.GuiMpmPart gui : this.guiParts) {
            gui.renderModel(graphics, i, j, f);
         }

         for (GuiCreationNewParts.GuiMpmPart gui : this.guiParts) {
            gui.renderIcons(graphics, i, j, f);
         }

         for (GuiCreationNewParts.GuiMpmPart gui : this.guiParts) {
            if (gui.infoHovered) {
               List<FormattedCharSequence> text = Arrays.asList(
                  Component.m_237115_(gui.part.name).m_7532_(), Component.m_237110_("message.madeby", new Object[]{gui.part.author}).m_7532_()
               );
               if (!gui.part.isEnabled) {
                  text = Arrays.asList(Component.m_237110_("gui.disabled", new Object[]{gui.part.author}).m_7532_());
               }

               graphics.m_280245_(this.f_96547_, text, i, j);
            }
         }
      }
   }

   @Override
   public void buttonEvent(GuiNpcButton btn) {
   }

   @Override
   public void save() {
   }

   @Override
   public void unFocused(GuiNpcTextField textfield) {
      if (textfield.id == 23) {
      }
   }

   @Override
   public void scrollClicked(double i, double j, int k, GuiCustomScroll scroll) {
      if (scroll.getSelectedIndex() >= 0) {
         active = scroll.getSelected();
         this.guiParts.clear();
         this.m_7856_();
      }
   }

   @Override
   public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
   }

   class EyesPart extends GuiNPCInterface {
      private MpmPartEyes part;
      private ModelEyeData data;

      public EyesPart(ModelEyeData data, MpmPartEyes part) {
         this.data = data;
         this.part = part;
         this.xSize = 310;
         this.ySize = 200;
         this.closeOnEsc = true;
      }

      @Override
      public void m_7856_() {
         super.m_7856_();
         this.guiLeft += 55;
         int y = this.guiTop + 8;
         this.addButton(
            new GuiButtonBiDirectional(
               21, this.guiLeft + 110, y, 110, 20, new String[]{"gui.playerskin", "gui.normal", "gui.texture"}, this.data.skinType, b -> {
                  this.data.skinType = ((GuiButtonBiDirectional)b).getValue();
                  this.m_7856_();
               }
            )
         );
         this.addLabel(new GuiNpcLabel(21, "part.eyes", this.guiLeft + 56, y + 5));
         if (this.data.skinType == 1) {
            this.addButton(
               new GuiColorButton(
                  3,
                  this.guiLeft + 230,
                  y,
                  ColorUtil.rgbToColor(this.data.color),
                  b -> this.setSubGui(
                     new GuiModelColor(GuiCreationNewParts.this, ColorUtil.rgbToColor(this.data.color), color -> this.data.color = ColorUtil.colorToRgb(color))
                  )
               )
            );
         }

         y += 25;
         if (this.data.skinType == 2) {
            this.addTextField(new GuiNpcTextField(this, 2, this.guiLeft + 110, y, 195, 20, this.data.url, tf -> this.data.setUrl(tf.m_94155_())));
            this.addLabel(new GuiNpcLabel(2, "config.skinurl", this.guiLeft + 56, y + 5));
         }

         int var10004 = this.guiLeft + 54;
         y += 25;
         this.addButton(new GuiButtonBiDirectional(22, var10004, y, 100, 20, new String[]{"gui.normal", "gui.big"}, this.data.eyeSize, b -> {
            this.data.eyeSize = ((GuiButtonBiDirectional)b).getValue();
            this.m_7856_();
         }));
         if (this.data.glint || this.data.skinType == 1 || this.data.skinType == 2) {
            this.addButton(
               new GuiButtonBiDirectional(23, this.guiLeft + 156, y, 100, 20, new String[]{"gui.normal", "gui.mirror"}, this.data.mirror ? 1 : 0, b -> {
                  this.data.mirror = ((GuiButtonBiDirectional)b).getValue() == 1;
                  this.m_7856_();
               })
            );
         }

         this.addLabel(new GuiNpcLabel(3, "eye.pupil", this.guiLeft + 4, y + 5));
         var10004 = this.guiLeft + 54;
         y += 25;
         this.addButton(
            new GuiButtonBiDirectional(
               51,
               var10004,
               y,
               100,
               20,
               new String[]{
                  I18n.m_118938_("gui.down", new Object[0]) + "x2", "gui.down", "gui.normal", "gui.up", I18n.m_118938_("gui.up", new Object[0]) + "x2"
               },
               this.data.eyePos.y + 2,
               b -> this.data.eyePos = new NopVector2i(this.data.eyePos.x, ((GuiButtonBiDirectional)b).getValue() - 2)
            )
         );
         this.addButton(
            new GuiButtonBiDirectional(
               50,
               this.guiLeft + 156,
               y,
               100,
               20,
               new String[]{"gui.inward", "gui.normal", "gui.outward"},
               this.data.eyePos.x + 1,
               b -> this.data.eyePos = new NopVector2i(((GuiButtonBiDirectional)b).getValue() - 1, this.data.eyePos.y)
            )
         );
         this.addLabel(new GuiNpcLabel(50, "gui.position", this.guiLeft + 4, y + 5));
         var10004 = this.guiLeft + 54;
         y += 25;
         this.addButton(new GuiNpcButtonYesNo(34, var10004, y, this.data.glint, b -> {
            this.data.glint = ((GuiNpcButtonYesNo)b).getBoolean();
            this.m_7856_();
         }));
         this.addLabel(new GuiNpcLabel(34, "eye.glint", this.guiLeft + 4, y + 5));
         this.addButton(
            new GuiColorButton(
               35,
               this.guiLeft + 162,
               y,
               ColorUtil.rgbToColor(this.data.browColor),
               b -> this.setSubGui(
                  new GuiModelColor(
                     GuiCreationNewParts.this, ColorUtil.rgbToColor(this.data.browColor), color -> this.data.browColor = ColorUtil.colorToRgb(color)
                  )
               )
            )
         );
         this.addButton(
            new GuiButtonBiDirectional(
               36,
               this.guiLeft + 214,
               y,
               70,
               20,
               new String[]{"gui.disabled", "1", "2", "3", "4", "5", "6", "7", "8"},
               (int)(this.data.browThickness.y * 10.0F),
               b -> this.data.browThickness = new NopVector3f(1.0F, ((GuiButtonBiDirectional)b).getValue() / 10.0F, 1.0F)
            )
         );
         this.addLabel(new GuiNpcLabel(35, "eye.lash", this.guiLeft + 112, y + 5));
         var10004 = this.guiLeft + 54;
         y += 25;
         this.addButton(new GuiNpcButtonYesNo(40, var10004, y, !this.data.disableBlink, b -> {
            this.data.disableBlink = !((GuiNpcButtonYesNo)b).getBoolean();
            this.m_7856_();
         }));
         this.addLabel(new GuiNpcLabel(40, "eye.blink", this.guiLeft + 4, y + 5));
         if (!this.data.disableBlink) {
            this.addButton(
               new GuiColorButton(
                  41,
                  this.guiLeft + 162,
                  y,
                  ColorUtil.rgbToColor(this.data.lidColor),
                  b -> this.setSubGui(
                     new GuiModelColor(
                        GuiCreationNewParts.this, ColorUtil.rgbToColor(this.data.lidColor), color -> this.data.lidColor = ColorUtil.colorToRgb(color)
                     )
                  )
               )
            );
            this.addLabel(new GuiNpcLabel(41, "eye.lid", this.guiLeft + 112, y + 5));
         }

         this.addButton(new GuiNpcButton(66, this.guiLeft + 288, this.guiTop + 4, 20, 20, "X", b -> this.close()));
      }

      public void m_280273_(GuiGraphics graphics) {
         super.m_280273_(graphics);
         graphics.m_280509_(this.guiLeft, this.guiTop, this.guiLeft + this.xSize, this.guiTop + this.ySize, -3750202);
         graphics.m_280656_(this.guiLeft, this.guiLeft + this.xSize, this.guiTop, -1);
         graphics.m_280656_(this.guiLeft, this.guiLeft + this.xSize, this.guiTop + this.ySize, -1);
         graphics.m_280315_(this.guiLeft, this.guiTop, this.guiTop + this.ySize, -1);
         graphics.m_280315_(this.guiLeft + this.xSize, this.guiTop, this.guiTop + this.ySize, -1);
         PoseStack posestack = RenderSystem.getModelViewStack();
         posestack.m_85836_();
         posestack.m_85837_(this.guiLeft + 10, this.guiTop + 10, 150.0);
         posestack.m_85841_(1.0F, 1.0F, -1.0F);
         RenderSystem.applyModelViewMatrix();
         PoseStack matrixstack = new PoseStack();
         matrixstack.m_85836_();
         EntityRenderDispatcher entityrenderermanager = this.f_96541_.m_91290_();
         entityrenderermanager.m_114468_(false);
         BufferSource irendertypebuffer$impl = this.f_96541_.m_91269_().m_110104_();
         VertexConsumer ivertex = irendertypebuffer$impl.m_6299_(RenderType.m_110458_(this.player.m_108560_()));
         Lighting.m_166384_();
         RenderSystem.runAsFancy(
            () -> {
               GuiCreationNewParts.biped.f_102810_.f_104207_ = !this.part.hiddenParts.contains(BodyPart.BODY);
               GuiCreationNewParts.biped.f_103378_.f_104207_ = GuiCreationNewParts.biped.f_103378_.f_104207_ && GuiCreationNewParts.biped.f_102810_.f_104207_;
               GuiCreationNewParts.biped.f_102808_.f_104207_ = !this.part.hiddenParts.contains(BodyPart.HEAD);
               GuiCreationNewParts.biped.f_102809_.f_104207_ = GuiCreationNewParts.biped.f_102809_.f_104207_ && GuiCreationNewParts.biped.f_102808_.f_104207_;
               matrixstack.m_252880_(19.0F, 43.0F, 25.0F);
               matrixstack.m_85841_(100.0F, 100.0F, 100.0F);
               GuiCreationNewParts.biped.f_102808_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
               this.part.pos = NopVector3f.ZERO;
               this.part.rot = NopVector3f.ZERO;
               LayerParts.renderPart(
                  this.data,
                  this.part,
                  matrixstack,
                  irendertypebuffer$impl,
                  15728880,
                  this.f_96541_.f_91074_,
                  GuiCreationNewParts.biped,
                  GuiCreationNewParts.this.renderData
               );
            }
         );
         irendertypebuffer$impl.m_109911_();
         matrixstack.m_85849_();
         posestack.m_85849_();
         entityrenderermanager.m_114468_(true);
         RenderSystem.applyModelViewMatrix();
      }

      @Override
      public void m_88315_(GuiGraphics graphics, int i, int j, float f) {
         super.m_88315_(graphics, i, j, f);
      }

      @Override
      public void save() {
      }
   }

   class GuiMpmPart extends AbstractWidget {
      public static final int SIZE = 70;
      public boolean basic = false;
      private List<MpmPart> all = new ArrayList<>();
      private MpmPart part;
      private MpmPartData data;
      private boolean selected = true;
      boolean colorPickerHovered = false;
      boolean infoHovered = false;
      boolean settingsHovered = false;
      boolean hoverL = false;
      boolean hoverR = false;
      int zPos = 0;

      public GuiMpmPart(int x, int y, MpmPart part) {
         super(x, y, 70, 70, Component.m_237119_());
         this.part = part;
         this.all.add(part);

         for (Entry<ResourceLocation, MpmPart> entry : MpmPartReader.PARTS.entrySet()) {
            if (entry.getValue().parentId != null && entry.getValue().parentId.equals(part.id)) {
               this.all.add(entry.getValue());
            }
         }

         for (MpmPart p : this.all) {
            this.data = GuiCreationNewParts.this.data.mpmParts.stream().filter(t -> t.partId.equals(p.id)).findFirst().orElse(null);
            if (this.data != null) {
               this.part = p;
               break;
            }
         }

         this.all = this.all.stream().sorted(Comparator.comparing(t -> t.id)).collect(Collectors.toList());
         if (this.data == null) {
            if (!part.id.equals(ModelEyeData.RESOURCE) && !part.id.equals(ModelEyeData.RESOURCE_RIGHT) && !part.id.equals(ModelEyeData.RESOURCE_LEFT)) {
               this.data = new MpmPartData();
            } else {
               this.data = new ModelEyeData();
            }

            this.data.clientData = new MpmPartDataClient();
            this.data.partId = part.id;
            this.data.usePlayerSkin = part.defaultUsePlayerSkins;
            this.selected = false;
         }
      }

      protected void m_168797_(NarrationElementOutput p_259858_) {
      }

      public void renderModel(GuiGraphics graphics, int xMouse, int yMouse, float tick) {
         int x1 = this.m_252754_();
         int x2 = this.m_252754_() + 70;
         int y1 = this.m_252907_();
         int y2 = this.m_252907_() + 70 - 1;
         graphics.m_280509_(x1, y1, x2, y2, -3750202);
         Minecraft minecraft = Minecraft.m_91087_();
         GuiCreationNewParts.this.renderData.mpmParts = GuiCreationNewParts.this.data.mpmParts;
         PoseStack matrixstack = graphics.m_280168_();
         matrixstack.m_85836_();
         matrixstack.m_85837_(this.m_252754_() + 10, this.m_252907_() + 10, 150.0);
         matrixstack.m_252931_(new Matrix4f().scaling(1.0F, 1.0F, -1.0F));
         EntityRenderDispatcher entityrenderermanager = minecraft.m_91290_();
         entityrenderermanager.m_114468_(false);
         BufferSource irendertypebuffer$impl = graphics.m_280091_();
         VertexConsumer ivertex = irendertypebuffer$impl.m_6299_(RenderType.m_110458_(GuiCreationNewParts.this.player.m_108560_()));
         Lighting.m_166384_();
         RenderSystem.runAsFancy(
            () -> {
               GuiCreationNewParts.biped.f_102814_.f_104207_ = !this.part.hiddenParts.contains(BodyPart.LEFT_LEG)
                  && !this.part.hiddenParts.contains(BodyPart.LEGS);
               GuiCreationNewParts.biped.f_103376_.f_104207_ = GuiCreationNewParts.biped.f_103376_.f_104207_ && GuiCreationNewParts.biped.f_102814_.f_104207_;
               GuiCreationNewParts.biped.f_102813_.f_104207_ = !this.part.hiddenParts.contains(BodyPart.RIGHT_LEG)
                  && !this.part.hiddenParts.contains(BodyPart.LEGS);
               GuiCreationNewParts.biped.f_103377_.f_104207_ = GuiCreationNewParts.biped.f_103377_.f_104207_ && GuiCreationNewParts.biped.f_102813_.f_104207_;
               GuiCreationNewParts.biped.f_102812_.f_104207_ = !this.part.hiddenParts.contains(BodyPart.LEFT_ARM)
                  && !this.part.hiddenParts.contains(BodyPart.ARMS);
               GuiCreationNewParts.biped.f_103374_.f_104207_ = GuiCreationNewParts.biped.f_103374_.f_104207_ && GuiCreationNewParts.biped.f_102812_.f_104207_;
               GuiCreationNewParts.biped.f_102811_.f_104207_ = !this.part.hiddenParts.contains(BodyPart.RIGHT_ARM)
                  && !this.part.hiddenParts.contains(BodyPart.ARMS);
               GuiCreationNewParts.biped.f_103375_.f_104207_ = GuiCreationNewParts.biped.f_103375_.f_104207_ && GuiCreationNewParts.biped.f_102811_.f_104207_;
               GuiCreationNewParts.biped.f_102810_.f_104207_ = !this.part.hiddenParts.contains(BodyPart.BODY);
               GuiCreationNewParts.biped.f_103378_.f_104207_ = GuiCreationNewParts.biped.f_103378_.f_104207_ && GuiCreationNewParts.biped.f_102810_.f_104207_;
               GuiCreationNewParts.biped.f_102808_.f_104207_ = !this.part.hiddenParts.contains(BodyPart.HEAD);
               GuiCreationNewParts.biped.f_102809_.f_104207_ = GuiCreationNewParts.biped.f_102809_.f_104207_ && GuiCreationNewParts.biped.f_102808_.f_104207_;
               if (this.part.bodyPart == BodyPart.HEAD) {
                  matrixstack.m_252880_(24.0F, 34.0F, 25.0F);
                  matrixstack.m_85841_(36.0F, 36.0F, 36.0F);
                  matrixstack.m_252781_(Axis.f_252529_.m_252961_((float) (Math.PI / 8)));
                  matrixstack.m_252781_(Axis.f_252436_.m_252961_(this.part.previewRotation * (float) (Math.PI / 180.0)));
                  GuiCreationNewParts.biped.f_102808_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
               }

               if (this.part.bodyPart == BodyPart.LEGS) {
                  matrixstack.m_252880_(18.0F, 4.0F, 25.0F);
                  matrixstack.m_85841_(36.0F, 36.0F, 36.0F);
                  matrixstack.m_252781_(Axis.f_252529_.m_252961_((float) (Math.PI / 8)));
                  matrixstack.m_252781_(Axis.f_252436_.m_252961_(this.part.previewRotation * (float) (Math.PI / 180.0)));
                  GuiCreationNewParts.biped.f_102810_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
                  if (this.part.animationType == PartBehaviorType.LEGS) {
                     ModelPartWrapper modelPart = this.part.getPart("right_leg");
                     if (modelPart != null) {
                        modelPart.setRot(
                           new NopVector3f(
                              GuiCreationNewParts.biped.f_102813_.f_104203_,
                              GuiCreationNewParts.biped.f_102813_.f_104204_,
                              GuiCreationNewParts.biped.f_102813_.f_104205_
                           )
                        );
                        modelPart.setPos(
                           new NopVector3f(
                              GuiCreationNewParts.biped.f_102813_.f_104200_,
                              GuiCreationNewParts.biped.f_102813_.f_104201_,
                              GuiCreationNewParts.biped.f_102813_.f_104202_
                           )
                        );
                     }

                     modelPart = this.part.getPart("left_leg");
                     if (modelPart != null) {
                        modelPart.setRot(
                           new NopVector3f(
                              GuiCreationNewParts.biped.f_102814_.f_104203_,
                              GuiCreationNewParts.biped.f_102814_.f_104204_,
                              GuiCreationNewParts.biped.f_102814_.f_104205_
                           )
                        );
                        modelPart.setPos(
                           new NopVector3f(
                              GuiCreationNewParts.biped.f_102814_.f_104200_,
                              GuiCreationNewParts.biped.f_102814_.f_104201_,
                              GuiCreationNewParts.biped.f_102814_.f_104202_
                           )
                        );
                     }
                  }

                  GuiCreationNewParts.biped.f_102813_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
                  GuiCreationNewParts.biped.f_102814_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
               }

               if (this.part.bodyPart == BodyPart.ARMS) {
                  matrixstack.m_252880_(18.0F, 12.0F, 25.0F);
                  matrixstack.m_85841_(36.0F, 36.0F, 36.0F);
                  matrixstack.m_252781_(Axis.f_252529_.m_252961_((float) (Math.PI / 8)));
                  matrixstack.m_252781_(Axis.f_252436_.m_252961_(this.part.previewRotation * (float) (Math.PI / 180.0)));
                  GuiCreationNewParts.biped.f_102810_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
                  if (this.part.animationType == PartBehaviorType.ARMS) {
                     ModelPartWrapper modelPartx = this.part.getPart("right_arm");
                     if (modelPartx != null) {
                        modelPartx.setRot(
                           new NopVector3f(
                              GuiCreationNewParts.biped.f_102811_.f_104203_,
                              GuiCreationNewParts.biped.f_102811_.f_104204_,
                              GuiCreationNewParts.biped.f_102811_.f_104205_
                           )
                        );
                        modelPartx.setPos(
                           new NopVector3f(
                              GuiCreationNewParts.biped.f_102811_.f_104200_,
                              GuiCreationNewParts.biped.f_102811_.f_104201_,
                              GuiCreationNewParts.biped.f_102811_.f_104202_
                           )
                        );
                     }

                     modelPartx = this.part.getPart("left_arm");
                     if (modelPartx != null) {
                        modelPartx.setRot(
                           new NopVector3f(
                              GuiCreationNewParts.biped.f_102812_.f_104203_,
                              GuiCreationNewParts.biped.f_102812_.f_104204_,
                              GuiCreationNewParts.biped.f_102812_.f_104205_
                           )
                        );
                        modelPartx.setPos(
                           new NopVector3f(
                              GuiCreationNewParts.biped.f_102812_.f_104200_,
                              GuiCreationNewParts.biped.f_102812_.f_104201_,
                              GuiCreationNewParts.biped.f_102812_.f_104202_
                           )
                        );
                     }
                  }

                  GuiCreationNewParts.biped.f_102812_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
                  GuiCreationNewParts.biped.f_102811_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
               }

               if (this.part.bodyPart == BodyPart.BODY) {
                  matrixstack.m_252880_(18.0F, 18.0F, 25.0F);
                  matrixstack.m_85841_(36.0F, 36.0F, 36.0F);
                  matrixstack.m_252781_(Axis.f_252529_.m_252961_((float) (Math.PI / 8)));
                  matrixstack.m_252781_(Axis.f_252436_.m_252961_(this.part.previewRotation * (float) (Math.PI / 180.0)));
                  GuiCreationNewParts.biped.f_102810_.m_104301_(matrixstack, ivertex, 15728880, OverlayTexture.f_118083_);
               }

               if (this.part.renderType != PartRenderType.NONE) {
                  MpmPartAbstractClient partc = (MpmPartAbstractClient)this.part;
                  partc.pos = NopVector3f.ZERO;
                  partc.rot = NopVector3f.ZERO;
                  LayerParts.renderPart(
                     this.data,
                     partc,
                     matrixstack,
                     irendertypebuffer$impl,
                     15728880,
                     minecraft.f_91074_,
                     GuiCreationNewParts.biped,
                     GuiCreationNewParts.this.renderData
                  );
               }
            }
         );
         graphics.m_280262_();
         matrixstack.m_85849_();
         entityrenderermanager.m_114468_(true);
         Lighting.m_84931_();
      }

      public void m_87963_(GuiGraphics graphics, int xMouse, int yMouse, float tick) {
         if (GuiCreationNewParts.this.hasSubGui()) {
            this.renderModel(graphics, xMouse, yMouse, tick);
            this.renderIcons(graphics, xMouse, yMouse, tick);
         }
      }

      public void renderIcons(GuiGraphics graphics, int xMouse, int yMouse, float tick) {
         int color = -1;
         if (!this.basic) {
            if (this.f_93622_) {
               color = -65536;
            }

            int x1 = this.m_252754_();
            int x2 = this.m_252754_() + 70;
            int y1 = this.m_252907_();
            int y2 = this.m_252907_() + 70 - 1;
            graphics.m_280656_(x1, x2, y1, color);
            graphics.m_280656_(x1, x2, y2, color);
            graphics.m_280315_(x1, y1, y2, color);
            graphics.m_280315_(x2, y1, y2, color);
            x1 = this.m_252754_() + 70 - 16;
            x2 = this.m_252754_() + 70;
            y1 = this.m_252907_() + 1;
            y2 = this.m_252907_() + 70 - 1;
            graphics.m_280509_(x1, y1, x2, y2, -3750202);
            int var13 = -1;
            x1 = this.m_252754_() + 70 - 14;
            x2 = this.m_252754_() + 70 - 2;
            y1 = this.m_252907_() + 2;
            y2 = this.m_252907_() + 14;
            graphics.m_280509_(x1, y1, x2, y2, -16777216);
            graphics.m_280656_(x1, x2, y1, var13);
            graphics.m_280656_(x1, x2, y2, var13);
            graphics.m_280315_(x1, y1, y2, var13);
            graphics.m_280315_(x2, y1, y2, var13);
            if (!this.part.isEnabled) {
               graphics.m_280430_(GuiCreationNewParts.this.f_96547_, Component.m_237113_("X").m_130940_(ChatFormatting.BOLD), x1 + 4, y1 + 3, 16711680);
            } else if (this.selected) {
               char c = (char)Integer.parseInt("2713", 16);
               graphics.m_280430_(GuiCreationNewParts.this.f_96547_, Component.m_237113_(c + "").m_130940_(ChatFormatting.BOLD), x1 + 3, y1 + 2, 65280);
            }
         }

         int guiY = this.m_252907_() + 16;
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         int size = 14;
         int x1 = this.m_252754_() + 70 - 15;
         int x2 = x1 + size;
         int y1 = guiY;
         int y2 = guiY + size;
         this.colorPickerHovered = xMouse >= x1 && yMouse >= guiY && xMouse < x2 && yMouse < y2;
         if (this.colorPickerHovered) {
            x1--;
            y1 = guiY - 1;
            size = 16;
         }

         graphics.m_280398_(GuiCreationNewParts.colorWheel, x1, y1, 0, 0.0F, 0.0F, size, size, size, size);
         guiY += 15;
         if (this.all.size() > 1) {
            x1 = this.m_252754_() + 70 - 17;
            x2 = x1 + 6;
            y2 = guiY + 8;
            this.hoverL = xMouse >= x1 && yMouse >= guiY && xMouse < x2 && yMouse < y2;
            graphics.m_280218_(GuiButtonBiDirectional.resource, x1, guiY, 0, this.hoverL ? 76 : 60, 6, 8);
            String s = this.all.indexOf(this.part) + "";
            graphics.drawString(GuiCreationNewParts.this.f_96547_, s, x1 + 9.5F - GuiCreationNewParts.this.f_96547_.m_92895_(s) / 2.0F, guiY + 0.5F, 0, false);
            x1 = this.m_252754_() + 70 - 5;
            x2 = x1 + 6;
            y2 = guiY + 8;
            this.hoverR = xMouse >= x1 && yMouse >= guiY && xMouse < x2 && yMouse < y2;
            graphics.m_280218_(GuiButtonBiDirectional.resource, x1, guiY, 6, this.hoverR ? 76 : 60, 6, 8);
            guiY += 11;
         }

         if (!this.basic) {
            x1 = this.m_252754_() + 70 - 15;
            x2 = x1 + 14;
            y2 = guiY + 14;
            this.settingsHovered = xMouse >= x1 && yMouse >= guiY && xMouse < x2 && yMouse < y2;
            graphics.m_280218_(GuiButtonBiDirectional.resource, x1, guiY, 0, this.settingsHovered ? 140 : 126, 14, 14);
            int var21 = 8;
            x1 = this.m_252754_() + 70 - 10;
            x2 = x1 + var21;
            y1 = this.m_252907_() + 70 - 12;
            y2 = y1 + var21;
            this.infoHovered = xMouse >= x1 && yMouse >= y1 && xMouse < x2 && yMouse < y2;
            MutableComponent text = Component.m_237113_("i").m_130940_(ChatFormatting.BOLD);
            if (this.infoHovered) {
               text = text.m_130940_(ChatFormatting.UNDERLINE);
            }

            graphics.m_280614_(GuiCreationNewParts.this.f_96547_, text, x1 + 3, y1 + 2, 0, false);
         }
      }

      public void m_5716_(double xMouse, double yMouse) {
         if (this.colorPickerHovered) {
            if (GuiCreationNewParts.this.hasSubGui()) {
               GuiCreationNewParts.this.getSubGui()
                  .setSubGui(new GuiModelColor(GuiCreationNewParts.this, this.data.getColor(), color -> this.data.setColor(color)));
            } else {
               GuiCreationNewParts.this.setSubGui(new GuiModelColor(GuiCreationNewParts.this, this.data.getColor(), color -> this.data.setColor(color)));
            }
         } else if (this.hoverL) {
            int index = (this.all.indexOf(this.part) + this.all.size() - 1) % this.all.size();
            this.part = this.all.get(index);
            this.data.partId = this.part.id;
         } else if (this.hoverR) {
            int index = (this.all.indexOf(this.part) + 1) % this.all.size();
            this.part = this.all.get(index);
            this.data.partId = this.part.id;
         } else if (this.settingsHovered) {
            if (this.data instanceof ModelEyeData) {
               GuiCreationNewParts.this.setSubGui(GuiCreationNewParts.this.new EyesPart((ModelEyeData)this.data, (MpmPartEyes)this.part));
            } else {
               GuiCreationNewParts.this.setSubGui(GuiCreationNewParts.this.new TexturePart(this.data, this.part));
            }
         } else if (this.part.isEnabled && !this.basic) {
            this.selected = !this.selected;
            if (this.selected) {
               GuiCreationNewParts.this.data.mpmParts.add(this.data);
            } else {
               GuiCreationNewParts.this.data.mpmParts.removeIf(t -> t.partId.equals(this.data.partId));
            }
         }

         GuiCreationNewParts.this.data.refreshParts();
      }
   }

   class TexturePart extends GuiNPCInterface {
      private MpmPart part;
      private MpmPartData data;
      private GuiCreationNewParts.GuiMpmPart partGui;

      public TexturePart(MpmPartData data, MpmPart part) {
         this.data = data;
         this.part = part;
         this.xSize = 310;
         this.ySize = 200;
         this.closeOnEsc = true;
      }

      @Override
      public void m_7856_() {
         super.m_7856_();
         this.guiLeft += 55;
         this.partGui = GuiCreationNewParts.this.new GuiMpmPart(this.guiLeft + 2, this.guiTop + 2, this.part);
         this.partGui.zPos = 250;
         this.partGui.basic = true;
         this.m_142416_(this.partGui);
         if (!this.part.disableCustomTextures) {
            this.addLabel(new GuiNpcLabel(21, "gui.playerskin", this.guiLeft + 4, this.guiTop + 110));
            this.addButton(new GuiNpcButtonYesNo(21, this.guiLeft + 76, this.guiTop + 105, this.data.usePlayerSkin, b -> {
               this.data.usePlayerSkin = ((GuiNpcButtonYesNo)b).getBoolean();
               this.m_7856_();
            }));
            if (!this.data.usePlayerSkin) {
               this.addLabel(new GuiNpcLabel(1, "gui.texture", this.guiLeft + 4, this.guiTop + 130));
               ResourceLocation loc = this.data.getDefaultTexture();
               this.addTextField(
                  new GuiNpcTextField(
                     this, 1, this.guiLeft + 4, this.guiTop + 140, 220, 20, loc == null ? "" : loc.toString(), tf -> this.data.setTexture(tf.m_94155_())
                  )
               );
               this.addButton(
                  new GuiNpcButton(
                     1,
                     this.guiLeft + 226,
                     this.guiTop + 140,
                     80,
                     20,
                     "gui.select",
                     b -> this.setSubGui(new GuiTextureSelection(GuiCreationNewParts.this.data, loc, r -> this.data.texture = r))
                  )
               );
               this.addLabel(new GuiNpcLabel(2, "config.skinurl", this.guiLeft + 4, this.guiTop + 168));
               this.addTextField(
                  new GuiNpcTextField(this, 2, this.guiLeft + 4, this.guiTop + 178, 220, 20, this.data.url, tf -> this.data.setUrl(tf.m_94155_()))
               );
            }
         }

         this.addButton(new GuiNpcButton(66, this.guiLeft + 276, this.guiTop + 4, 20, 20, "X", b -> this.close()));
      }

      public void m_280273_(GuiGraphics graphics) {
         super.m_280273_(graphics);
         graphics.m_280509_(this.guiLeft, this.guiTop, this.guiLeft + this.xSize, this.guiTop + this.ySize, -3750202);
         graphics.m_280656_(this.guiLeft, this.guiLeft + this.xSize, this.guiTop, -1);
         graphics.m_280656_(this.guiLeft, this.guiLeft + this.xSize, this.guiTop + this.ySize, -1);
         graphics.m_280315_(this.guiLeft, this.guiTop, this.guiTop + this.ySize, -1);
         graphics.m_280315_(this.guiLeft + this.xSize, this.guiTop, this.guiTop + this.ySize, -1);
      }

      @Override
      public void m_88315_(GuiGraphics graphics, int i, int j, float f) {
         super.m_88315_(graphics, i, j, f);
         if (!this.hasSubGui()) {
            this.partGui.renderModel(graphics, i, j, f);
         }
      }

      @Override
      public void save() {
      }
   }
}
