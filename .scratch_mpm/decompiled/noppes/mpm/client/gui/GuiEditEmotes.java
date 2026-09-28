package noppes.mpm.client.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.client.gui.util.GuiButtonBiDirectional;
import noppes.mpm.client.gui.util.GuiNPCInterface;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.GuiNpcLabel;

public class GuiEditEmotes extends GuiNPCInterface {
   private final String[] animations = new String[]{
      "gui.none",
      "animation.sleep",
      "animation.crawl",
      "animation.hug",
      "animation.sit",
      "animation.dance",
      "animation.wave",
      "animation.wag",
      "animation.bow",
      "animation.cry",
      "animation.yes",
      "animation.no",
      "animation.point",
      "animation.death"
   };

   public GuiEditEmotes() {
      this.xSize = 366;
      this.ySize = 226;
      this.setBackground("menubg.png");
      this.closeOnEsc = true;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      int y = this.guiTop + 4;
      this.addLabel(new GuiNpcLabel(0, "message.animationmessage1", this.guiLeft + 4, y));
      this.addLabel(new GuiNpcLabel(6, "message.animationmessage2", this.guiLeft + 4, y + 11));
      y += 32;
      this.addButton(1, y, "MPM 1", MorePlayerModels.button1);
      y += 22;
      this.addButton(2, y, "MPM 2", MorePlayerModels.button2);
      y += 22;
      this.addButton(3, y, "MPM 3", MorePlayerModels.button3);
      y += 22;
      this.addButton(4, y, "MPM 4", MorePlayerModels.button4);
      y += 22;
      this.addButton(5, y, "MPM 5", MorePlayerModels.button5);
      this.addButton(new GuiNpcButton(this, 66, this.guiLeft + this.xSize - 24, this.guiTop + 4, 20, 20, "X"));
      y = this.guiTop + 32;
      this.addLabel(new GuiNpcLabel(10, Component.m_237115_("gui.commands").m_130946_(":"), this.guiLeft + 240, y));
      int var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(11, "/bow", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(12, "/crawl", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(13, "/cry", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(14, "/dance", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(15, "/death", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(16, "/hug", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(17, "/point", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(18, "/sit", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(19, "/sleep", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(20, "/wag", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(21, "/wave", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(22, "/yes", var10005, y));
      var10005 = this.guiLeft + 250;
      y += 11;
      this.addLabel(new GuiNpcLabel(23, "/no", var10005, y));
   }

   private void addButton(int id, int y, String title, int value) {
      Component comp = Component.m_237119_();

      for (KeyMapping key : Minecraft.m_91087_().f_91066_.f_92059_) {
         if (key.m_90860_().equals(title)) {
            comp = Component.m_237113_(" (").m_7220_(key.getKey().m_84875_()).m_130946_(")");
            break;
         }
      }

      this.addButton(new GuiButtonBiDirectional(this, id, this.guiLeft + 80, y, 100, 20, this.animations, value));
      this.addLabel(new GuiNpcLabel(id, Component.m_237113_(title).m_7220_(comp), this.guiLeft + 4, y + 5));
   }

   @Override
   public void buttonEvent(GuiNpcButton button) {
      if (button.id == 1) {
         MorePlayerModels.button1 = button.getValue();
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 2) {
         MorePlayerModels.button2 = button.getValue();
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 3) {
         MorePlayerModels.button3 = button.getValue();
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 4) {
         MorePlayerModels.button4 = button.getValue();
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 5) {
         MorePlayerModels.button5 = button.getValue();
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 66) {
         this.close();
      }
   }

   @Override
   public void save() {
   }
}
