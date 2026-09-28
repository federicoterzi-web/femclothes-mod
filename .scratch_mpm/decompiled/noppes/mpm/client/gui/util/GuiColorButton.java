package noppes.mpm.client.gui.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button.OnPress;

public class GuiColorButton extends GuiNpcButton {
   public int color;

   public GuiColorButton(int id, int x, int y, int color, OnPress clicked) {
      super(id, x, y, 50, 20, "", clicked);
      this.color = color;
   }

   @Override
   public void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.f_93624_) {
         graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + 50, this.m_252907_() + 20, -16777216 + this.color);
      }
   }
}
