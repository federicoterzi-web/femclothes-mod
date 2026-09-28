package noppes.mpm.client.gui.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GuiNpcSlider extends AbstractWidget {
   public static final ResourceLocation SLIDER_LOCATION = new ResourceLocation("textures/gui/slider.png");
   private ISliderListener listener;
   public int id;
   public float sliderValue = 1.0F;

   public GuiNpcSlider(Screen parent, int id, int xPos, int yPos, String displayString, float sliderValue) {
      super(xPos, yPos, 150, 20, Component.m_237115_(displayString));
      this.id = id;
      this.sliderValue = sliderValue;
      if (parent instanceof ISliderListener) {
         this.listener = (ISliderListener)parent;
      }
   }

   public GuiNpcSlider(Screen parent, int id, int xPos, int yPos, float sliderValue) {
      this(parent, id, xPos, yPos, "", sliderValue);
      if (this.listener != null) {
         this.listener.mouseDragged(this);
      }
   }

   public GuiNpcSlider(Screen parent, int id, int xPos, int yPos, int width, int height, float sliderValue) {
      this(parent, id, xPos, yPos, "", sliderValue);
      this.f_93618_ = width;
      this.f_93619_ = height;
      if (this.listener != null) {
         this.listener.mouseDragged(this);
      }
   }

   public void setListener(ISliderListener listener) {
      this.listener = listener;
      listener.mouseDragged(this);
   }

   public void m_7435_(SoundManager soundHandler) {
   }

   public void setString(String str) {
      this.m_93666_(Component.m_237113_(str));
   }

   private void setSliderValue(float value) {
      value = Mth.m_14036_(value, 0.0F, 1.0F);
      if (value != this.sliderValue) {
         this.sliderValue = value;
         this.listener.mouseDragged(this);
      }
   }

   public void m_5716_(double x, double y) {
      if (this.f_93624_ && this.f_93623_) {
         this.setSliderValue((float)(x - (this.m_252754_() + 4)) / (this.f_93618_ - 8));
      }
   }

   protected void m_7212_(double x, double y, double dragX, double dragY) {
      this.setSliderValue((float)(x - (this.m_252754_() + 4)) / (this.f_93618_ - 8));
      super.m_7212_(x, y, dragX, dragY);
   }

   public void m_7691_(double x, double y) {
      super.m_7435_(Minecraft.m_91087_().m_91106_());
   }

   public void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float f) {
      if (this.f_93624_) {
         int vOffset = (this.m_198029_() ? 2 : 1) * 20;
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         graphics.m_280027_(SLIDER_LOCATION, this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_(), 20, 4, 200, 20, 0, vOffset);
         graphics.m_280027_(
            SLIDER_LOCATION, this.m_252754_() + (int)((double)this.sliderValue * (this.f_93618_ - 8)), this.m_252907_(), 8, 20, 20, 4, 200, 20, 0, vOffset
         );
         int i = this.f_93623_ ? 16777215 : 10526880;
         this.m_280372_(graphics, Minecraft.m_91087_().f_91062_, 2, i | Mth.m_14167_(this.f_93625_ * 255.0F) << 24);
      }
   }

   protected void m_168797_(NarrationElementOutput p_259858_) {
   }
}
