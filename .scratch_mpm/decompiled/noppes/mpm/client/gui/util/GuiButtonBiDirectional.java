package noppes.mpm.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.resources.ResourceLocation;

public class GuiButtonBiDirectional extends GuiNpcButton {
   public static final ResourceLocation resource = new ResourceLocation("moreplayermodels:textures/gui/arrowbuttons.png");

   public GuiButtonBiDirectional(GuiNPCInterface gui, int id, int x, int y, int width, int height, String[] arr, int current) {
      super(gui, id, x, y, width, height, arr, current);
   }

   public GuiButtonBiDirectional(int id, int x, int y, int width, int height, String[] arr, int current, OnPress clicked) {
      super(id, x, y, width, height, arr, current, clicked);
   }

   @Override
   public void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.f_93624_) {
         Minecraft mc = Minecraft.m_91087_();
         boolean disabled = !this.f_93623_ || this.display.length <= 1;
         boolean hover = !disabled
            && mouseX >= this.m_252754_()
            && mouseY >= this.m_252907_()
            && mouseX < this.m_252754_() + this.f_93618_
            && mouseY < this.m_252907_() + this.f_93619_;
         boolean hoverL = !disabled
            && mouseX >= this.m_252754_()
            && mouseY >= this.m_252907_()
            && mouseX < this.m_252754_() + 11
            && mouseY < this.m_252907_() + this.f_93619_;
         boolean hoverR = !disabled
            && !hoverL
            && mouseX >= this.m_252754_() + this.f_93618_ - 11
            && mouseY >= this.m_252907_()
            && mouseX < this.m_252754_() + this.f_93618_
            && mouseY < this.m_252907_() + this.f_93619_;
         graphics.m_280218_(resource, this.m_252754_(), this.m_252907_(), 0, disabled ? 20 : (hoverL ? 40 : 0), 11, 20);
         graphics.m_280218_(resource, this.m_252754_() + this.f_93618_ - 11, this.m_252907_(), 11, disabled ? 20 : (hoverR ? 40 : 0), 11, 20);
         int l = 16777215;
         if (this.packedFGColor != 0) {
            l = this.packedFGColor;
         } else if (!this.f_93623_ || disabled) {
            l = 10526880;
         } else if (hover) {
            l = 16777120;
         }

         String text = "";
         float maxWidth = this.f_93618_ - 36;
         String displayString = this.m_6035_().getString();
         if (mc.f_91062_.m_92895_(displayString) > maxWidth) {
            for (int h = 0; h < displayString.length(); h++) {
               char c = displayString.charAt(h);
               text = text + c;
               if (mc.f_91062_.m_92895_(text) > maxWidth) {
                  break;
               }
            }

            text = text + "...";
         } else {
            text = displayString;
         }

         if (hover) {
            text = "§n" + text;
         }

         graphics.m_280137_(mc.f_91062_, text, this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + (this.f_93619_ - 8) / 2, l);
      }
   }

   @Override
   public void m_5716_(double x, double y) {
      if (this.display != null && this.display.length != 0) {
         int value = this.getValue();
         boolean hoverL = x >= this.m_252754_() && y >= this.m_252907_() && x < this.m_252754_() + 11 && y < this.m_252907_() + this.f_93619_;
         boolean hoverR = !hoverL
            && x >= this.m_252754_() + 11
            && y >= this.m_252907_()
            && x < this.m_252754_() + this.f_93618_
            && y < this.m_252907_() + this.f_93619_;
         if (hoverR) {
            value = (value + 1) % this.display.length;
         }

         if (hoverL) {
            if (value <= 0) {
               value = this.display.length;
            }

            value--;
         }

         this.setDisplay(value);
      }

      this.f_93717_.m_93750_(this);
   }
}
