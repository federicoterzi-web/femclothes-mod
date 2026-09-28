package noppes.mpm.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiNpcLabel {
   public Component label;
   private int x;
   private int y;
   private int color = 4210752;
   public boolean enabled = true;
   public int id;

   public GuiNpcLabel(int id, Component label, int x, int y, int color) {
      this.id = id;
      this.label = label;
      this.x = x;
      this.y = y;
      this.color = color;
   }

   public GuiNpcLabel(int id, String label, int x, int y, int color) {
      this(id, Component.m_237115_(label), x, y, color);
   }

   public GuiNpcLabel(int id, Component label, int x, int y) {
      this(id, label, x, y, 4210752);
   }

   public GuiNpcLabel(int id, String label, int x, int y) {
      this(id, label, x, y, 4210752);
   }

   public void drawLabel(GuiGraphics graphics, Screen gui, Font font) {
      if (this.enabled) {
         graphics.m_280614_(font, this.label, this.x, this.y, this.color, false);
      }
   }

   public void center(int width) {
      int size = Minecraft.m_91087_().f_91062_.m_92852_(this.label);
      this.x += (width - size) / 2;
   }
}
