package noppes.mpm.client.gui.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;

public class GuiNpcButton extends Button {
   public boolean shown = true;
   public GuiNPCInterface gui;
   protected String[] display;
   private int displayValue = 0;
   public int id;
   private static final OnPress clicked = button -> {
      GuiNpcButton b = (GuiNpcButton)button;
      b.gui.buttonEvent(b);
   };

   public GuiNpcButton(GuiNPCInterface gui, int i, int j, int k, String s) {
      super(j, k, 200, 20, Component.m_237115_(s), clicked, Button.f_252438_);
      this.id = i;
      this.gui = gui;
   }

   public GuiNpcButton(GuiNPCInterface gui, int i, int j, int k, String[] display, int val) {
      this(gui, i, j, k, display[val]);
      this.display = display;
      this.displayValue = val;
   }

   public GuiNpcButton(GuiNPCInterface gui, int i, int j, int k, int l, int m, String string) {
      super(j, k, l, m, Component.m_237115_(string), clicked, Button.f_252438_);
      this.id = i;
      this.gui = gui;
   }

   public GuiNpcButton(int i, int j, int k, int l, int m, String string, OnPress clicked) {
      super(j, k, l, m, Component.m_237115_(string), clicked, Button.f_252438_);
      this.id = i;
   }

   public GuiNpcButton(GuiNPCInterface gui, int i, int j, int k, int l, int m, String[] display, int val) {
      this(gui, i, j, k, l, m, display[val % display.length]);
      this.display = display;
      this.displayValue = val % display.length;
   }

   public GuiNpcButton(int i, int j, int k, int l, int m, String[] display, int val, OnPress clicked) {
      this(i, j, k, l, m, display[val % display.length], clicked);
      this.display = display;
      this.displayValue = val % display.length;
   }

   public void setDisplayText(String text) {
      this.m_93666_(Component.m_237115_(text));
   }

   public int getValue() {
      return this.displayValue;
   }

   public void clicked() {
   }

   public void m_87963_(GuiGraphics graphics, int i, int j, float partialTicks) {
      if (this.shown) {
         super.m_87963_(graphics, i, j, partialTicks);
      }
   }

   public void m_5716_(double x, double y) {
      if (this.display != null) {
         this.setDisplay((this.displayValue + 1) % this.display.length);
      }

      super.m_5716_(x, y);
   }

   public void setDisplay(int value) {
      this.displayValue = value;
      this.setDisplayText(this.display[value]);
   }
}
