package noppes.mpm.client.gui.util;

import net.minecraft.client.gui.components.Button.OnPress;

public class GuiNpcButtonYesNo extends GuiNpcButton {
   public GuiNpcButtonYesNo(GuiNPCInterface gui, int id, int x, int y, boolean bo) {
      this(gui, id, x, y, 50, 20, bo);
   }

   public GuiNpcButtonYesNo(int id, int x, int y, boolean bo, OnPress clicked) {
      this(id, x, y, 50, 20, bo, clicked);
   }

   public GuiNpcButtonYesNo(GuiNPCInterface gui, int id, int x, int y, int width, int height, boolean bo) {
      super(gui, id, x, y, width, height, new String[]{"gui.no", "gui.yes"}, bo ? 1 : 0);
   }

   public GuiNpcButtonYesNo(int id, int x, int y, int width, int height, boolean bo, OnPress clicked) {
      super(id, x, y, width, height, new String[]{"gui.no", "gui.yes"}, bo ? 1 : 0, clicked);
   }

   public boolean getBoolean() {
      return this.getValue() == 1;
   }
}
