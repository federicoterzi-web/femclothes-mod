package noppes.mpm.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiNpcTextField extends EditBox {
   public boolean enabled = true;
   public boolean inMenu = true;
   public boolean numbersOnly = false;
   private ITextfieldListener listener;
   public int id;
   public int min = 0;
   public int max = Integer.MAX_VALUE;
   public int def = 0;
   private static GuiNpcTextField activeTextfield = null;
   private final int[] allowedSpecialChars = new int[]{14, 211, 203, 205};

   public GuiNpcTextField(int id, Screen parent, int i, int j, int k, int l, String s) {
      super(Minecraft.m_91087_().f_91062_, i, j, k, l, Component.m_237113_(s));
      this.m_94199_(500);
      this.m_94144_(s);
      this.id = id;
      if (parent instanceof ITextfieldListener) {
         this.listener = (ITextfieldListener)parent;
      }
   }

   public GuiNpcTextField(Screen parent, int id, int i, int j, int k, int l, String s, ITextfieldListener listener) {
      super(Minecraft.m_91087_().f_91062_, i, j, k, l, Component.m_237113_(s));
      this.m_94199_(500);
      this.m_94144_(s);
      this.id = id;
      this.listener = listener;
   }

   public static boolean hasActive() {
      return activeTextfield != null;
   }

   private boolean charAllowed(char c, int i) {
      if (this.numbersOnly && !Character.isDigit(c)) {
         for (int j : this.allowedSpecialChars) {
            if (j == i) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public boolean m_5534_(char c, int i) {
      return !this.charAllowed(c, i) ? false : super.m_5534_(c, i);
   }

   public boolean isEmpty() {
      return this.m_94155_().trim().length() == 0;
   }

   public int getInteger() {
      return Integer.parseInt(this.m_94155_());
   }

   public boolean isInteger() {
      try {
         Integer.parseInt(this.m_94155_());
         return true;
      } catch (NumberFormatException var2) {
         return false;
      }
   }

   public void unFocused() {
      if (this.numbersOnly) {
         if (!this.isEmpty() && this.isInteger()) {
            if (this.getInteger() < this.min) {
               this.m_94144_(this.min + "");
            } else if (this.getInteger() > this.max) {
               this.m_94144_(this.max + "");
            }
         } else {
            this.m_94144_(this.def + "");
         }
      }

      if (this.listener != null) {
         this.listener.unFocused(this);
      }

      if (this == activeTextfield) {
         activeTextfield = null;
      }
   }

   public void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.enabled) {
         super.m_87963_(graphics, mouseX, mouseY, partialTicks);
      }
   }

   public void setMinMaxDefault(int i, int j, int k) {
      this.min = i;
      this.max = j;
      this.def = k;
   }

   public static void unfocus() {
      if (activeTextfield != null) {
         activeTextfield.unFocused();
      }

      activeTextfield = null;
   }

   public static void handleFocus(Screen screen, boolean clickResult) {
      if (screen.m_7222_() instanceof GuiNpcTextField tf) {
         if (!clickResult) {
            screen.m_7522_(null);
            unfocus();
            return;
         }

         if (activeTextfield != null && activeTextfield != tf) {
            activeTextfield.unFocused();
         }

         activeTextfield = tf;
      } else if (activeTextfield != null) {
         unfocus();
      }
   }
}
