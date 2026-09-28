package noppes.mpm.client.gui.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

public abstract class GuiNPCInterface extends Screen {
   public LocalPlayer player;
   public boolean drawDefaultBackground = true;
   private final HashMap<Integer, GuiNpcButton> npcbuttons = new HashMap<>();
   private final HashMap<Integer, GuiNpcTextField> textfields = new HashMap<>();
   private final HashMap<Integer, GuiNpcLabel> labels = new HashMap<>();
   private final HashMap<Integer, GuiCustomScroll> scrolls = new HashMap<>();
   private final HashMap<Integer, GuiNpcSlider> sliders = new HashMap<>();
   private final HashMap<Integer, Screen> extra = new HashMap<>();
   protected ResourceLocation background = null;
   public boolean closeOnEsc = false;
   public int guiLeft;
   public int guiTop;
   public int xSize;
   public int ySize;
   private GuiNPCInterface subgui;
   public GuiNPCInterface parent;
   public int mouseX;
   public int mouseY;

   public GuiNPCInterface() {
      super(Component.m_237119_());
      this.player = Minecraft.m_91087_().f_91074_;
      this.xSize = 200;
      this.ySize = 222;
   }

   public void setBackground(String texture) {
      this.background = new ResourceLocation("moreplayermodels", "textures/gui/" + texture);
   }

   public ResourceLocation getResource(String texture) {
      return new ResourceLocation("moreplayermodels", "textures/gui/" + texture);
   }

   public void m_7856_() {
      super.m_7856_();
      GuiNpcTextField.unfocus();
      if (this.subgui != null) {
         this.subgui.m_6575_(this.f_96541_, this.f_96543_, this.f_96544_);
         this.subgui.m_7856_();
      }

      this.guiLeft = (this.f_96543_ - this.xSize) / 2;
      this.guiTop = (this.f_96544_ - this.ySize) / 2;
      this.f_169369_.clear();
      this.labels.clear();
      this.m_6702_().clear();
      this.textfields.clear();
      this.npcbuttons.clear();
      this.scrolls.clear();
      this.sliders.clear();
   }

   public void m_86600_() {
      if (this.subgui != null) {
         this.subgui.m_86600_();
      } else {
         super.m_86600_();
      }
   }

   public boolean m_6375_(double i, double j, int k) {
      if (this.subgui != null) {
         return this.subgui.m_6375_(i, j, k);
      } else {
         if (k == 0) {
            for (GuiCustomScroll scroll : new ArrayList<>(this.scrolls.values())) {
               scroll.m_6375_(i, j, k);
            }
         }

         this.mouseEvent(i, j, k);
         boolean bo = super.m_6375_(i, j, k);
         GuiNpcTextField.handleFocus(this, bo);
         return bo;
      }
   }

   public void mouseEvent(double x, double y, int k) {
   }

   public void buttonEvent(GuiNpcButton button) {
   }

   public boolean m_5534_(char c, int i) {
      if (this.subgui != null) {
         return this.subgui.m_5534_(c, i);
      } else {
         for (GuiCustomScroll s : this.scrolls.values()) {
            s.m_5534_(c, i);
         }

         return super.m_5534_(c, i);
      }
   }

   public boolean m_6913_() {
      return false;
   }

   public boolean m_7933_(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
      if (this.subgui != null) {
         return this.subgui.m_7933_(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
      } else if (!this.closeOnEsc || p_keyPressed_1_ != 256 && (GuiNpcTextField.hasActive() || !this.isInventoryKey(p_keyPressed_1_))) {
         for (GuiCustomScroll s : this.scrolls.values()) {
            s.m_7933_(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
         }

         return super.m_7933_(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
      } else {
         this.close();
         return true;
      }
   }

   public void m_7379_() {
      super.m_7379_();
      GuiNpcTextField.unfocus();
   }

   public void close() {
      if (this.parent != null) {
         this.parent.closeSubGui(this);
      } else {
         this.setScreen(null);
      }

      this.save();
   }

   public void addButton(GuiNpcButton button) {
      this.npcbuttons.put(button.id, button);
      super.m_142416_(button);
   }

   public GuiNpcButton getButton(int i) {
      return this.npcbuttons.get(i);
   }

   public void addTextField(GuiNpcTextField tf) {
      this.textfields.put(tf.id, tf);
      super.m_142416_(tf);
   }

   public GuiNpcTextField getTextField(int i) {
      return this.textfields.get(i);
   }

   public void addLabel(GuiNpcLabel label) {
      this.labels.put(label.id, label);
   }

   public GuiNpcLabel getLabel(int i) {
      return this.labels.get(i);
   }

   public void addSlider(GuiNpcSlider slider) {
      this.sliders.put(slider.id, slider);
      this.m_142416_(slider);
   }

   public GuiNpcSlider getSlider(int i) {
      return this.sliders.get(i);
   }

   public void addScroll(GuiCustomScroll scroll) {
      scroll.m_6575_(this.f_96541_, scroll.f_96543_, scroll.f_96544_);
      this.scrolls.put(scroll.id, scroll);
   }

   public GuiCustomScroll getScroll(int id) {
      return this.scrolls.get(id);
   }

   public abstract void save();

   public void m_88315_(GuiGraphics graphics, int x, int y, float f) {
      this.mouseX = x;
      this.mouseY = y;
      if (this.subgui == null || this.subgui.drawSubGuiBackground()) {
         if (this.drawDefaultBackground) {
            this.m_280273_(graphics);
         }

         if (this.background != null && this.f_96541_.m_91097_() != null) {
            if (this.xSize > 256) {
               graphics.m_280218_(this.background, this.guiLeft, this.guiTop, 0, 0, 250, this.ySize);
               graphics.m_280218_(this.background, this.guiLeft + 250, this.guiTop, 256 - (this.xSize - 250), 0, this.xSize - 250, this.ySize);
            } else {
               graphics.m_280218_(this.background, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
            }
         }

         for (GuiNpcLabel label : this.labels.values()) {
            label.drawLabel(graphics, this, this.f_96547_);
         }

         for (GuiCustomScroll scroll : this.scrolls.values()) {
            scroll.m_88315_(graphics, x, y, f);
         }

         for (Screen gui : this.extra.values()) {
            gui.m_88315_(graphics, x, y, f);
         }

         for (Renderable renderable : this.f_169369_) {
            renderable.m_88315_(graphics, x, y, f);
         }
      }

      if (this.subgui != null) {
         graphics.m_280168_().m_252880_(0.0F, 0.0F, 260.0F);
         this.subgui.m_88315_(graphics, x, y, f);
         graphics.m_280168_().m_252880_(0.0F, 0.0F, -260.0F);
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double scrolledY) {
      if (this.subgui != null) {
         this.subgui.m_6050_(mouseX, mouseY, scrolledY);
      } else {
         for (GuiCustomScroll scroll : this.scrolls.values()) {
            scroll.m_6050_(mouseX, mouseY, scrolledY);
         }
      }

      return true;
   }

   public boolean m_7979_(double x, double y, int button, double dx, double dy) {
      return this.subgui != null ? this.subgui.m_7979_(x, y, button, dx, dy) : super.m_7979_(x, y, button, dx, dy);
   }

   public boolean m_6348_(double x, double y, int button) {
      return this.subgui != null ? this.subgui.m_6348_(x, y, button) : super.m_6348_(x, y, button);
   }

   public boolean drawSubGuiBackground() {
      return true;
   }

   public Font getFont() {
      return this.f_96547_;
   }

   public void elementClicked() {
      if (this.subgui != null) {
         this.subgui.elementClicked();
      }
   }

   public boolean m_7043_() {
      return false;
   }

   public void doubleClicked() {
   }

   public boolean isInventoryKey(int i) {
      return this.f_96541_.f_91066_.f_92092_.isActiveAndMatches(InputConstants.m_84827_(i, 0));
   }

   public void setScreen(Screen gui) {
      this.f_96541_.m_91152_(gui);
   }

   public void setSubGui(GuiNPCInterface gui) {
      this.subgui = gui;
      this.subgui.parent = this;
      this.subgui.m_6575_(this.f_96541_, this.f_96543_, this.f_96544_);
      this.m_7856_();
   }

   public void closeSubGui(GuiNPCInterface gui) {
      this.subgui = null;
      if (this instanceof ISubGuiListener) {
         ((ISubGuiListener)this).subGuiClosed(gui);
      }

      this.m_7856_();
   }

   public boolean hasSubGui() {
      return this.subgui != null;
   }

   public GuiNPCInterface getSubGui() {
      return this.hasSubGui() && this.subgui.hasSubGui() ? this.subgui.getSubGui() : this.subgui;
   }

   public void drawEntity(GuiGraphics graphics, LivingEntity entity, int x, int y, float zoomed, int rotation, int guiLeft, int guiTop) {
      float f2 = entity.f_20883_;
      float f3 = entity.m_146908_();
      float f4 = entity.m_146909_();
      float f5 = entity.f_20886_;
      float f6 = entity.f_20885_;
      float scale = 1.0F;
      if (entity.m_20206_() > 2.4) {
         scale = 2.0F / entity.m_20206_();
      }

      float f7 = (float)(guiLeft + x) - this.mouseX;
      float f8 = guiTop + y - 50.0F * scale * zoomed - this.mouseY;
      entity.f_20883_ = 0.0F;
      entity.m_146922_((float)Math.atan(f7 / 80.0F) * 40.0F + rotation);
      entity.m_146926_(-((float)Math.atan(f8 / 40.0F)) * 20.0F);
      entity.f_20885_ = 0.0F;
      entity.f_20886_ = 0.0F;
      PoseStack poseStack = graphics.m_280168_();
      poseStack.m_85836_();
      poseStack.m_85837_(guiLeft + x, guiTop + y, 1050.0);
      poseStack.m_252931_(new Matrix4f().scaling(1.0F, 1.0F, -1.0F));
      poseStack.m_85837_(0.0, 0.0, 1000.0);
      poseStack.m_85841_(30.0F * scale * zoomed, 30.0F * scale * zoomed, 1.0F);
      poseStack.m_252781_(Axis.f_252436_.m_252977_(180.0F));
      poseStack.m_252781_(Axis.f_252403_.m_252977_(180.0F));
      poseStack.m_252781_(Axis.f_252392_.m_252977_(rotation));
      Lighting.m_166384_();
      EntityRenderDispatcher lvt_16_1_ = Minecraft.m_91087_().m_91290_();
      lvt_16_1_.m_252923_(Axis.f_252392_.m_252977_(180.0F));
      lvt_16_1_.m_114468_(false);
      RenderSystem.runAsFancy(() -> lvt_16_1_.m_114384_(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, poseStack, graphics.m_280091_(), 15728880));
      graphics.m_280262_();
      lvt_16_1_.m_114468_(true);
      poseStack.m_85849_();
      Lighting.m_84931_();
      entity.f_20883_ = f2;
      entity.m_146922_(f3);
      entity.m_146926_(f4);
      entity.f_20886_ = f5;
      entity.f_20885_ = f6;
   }

   public void openLink(String link) {
      try {
         Class oclass = Class.forName("java.awt.Desktop");
         Object object = oclass.getMethod("getDesktop").invoke(null);
         oclass.getMethod("browse", URI.class).invoke(object, new URI(link));
      } catch (Throwable var4) {
      }
   }
}
