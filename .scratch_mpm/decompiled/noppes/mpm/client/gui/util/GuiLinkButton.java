package noppes.mpm.client.gui.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

public class GuiLinkButton extends Button {
   private int color = 238;

   public GuiLinkButton(int x, int y, Component title, OnPress action) {
      super(x, y, Minecraft.m_91087_().f_91062_.m_92852_(title), 9, title, action, Button.f_252438_);
   }

   public GuiLinkButton(int x, int y, int color, Component title, OnPress action) {
      super(x, y, Minecraft.m_91087_().f_91062_.m_92852_(title), 9, title, action, Button.f_252438_);
      this.color = color;
   }

   public void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      Minecraft minecraft = Minecraft.m_91087_();
      MutableComponent text = this.m_6035_().m_6879_().m_130944_(new ChatFormatting[0]);
      if (this.m_198029_()) {
         text.m_130944_(new ChatFormatting[]{ChatFormatting.UNDERLINE});
      }

      FormattedCharSequence ireorderingprocessor = text.m_7532_();
      graphics.m_280648_(minecraft.f_91062_, ireorderingprocessor, this.m_252754_(), this.m_252907_(), this.color);
   }
}
