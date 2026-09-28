package noppes.mpm.client.gui;

import noppes.mpm.MorePlayerModels;
import noppes.mpm.client.ClientProxy;
import noppes.mpm.client.SkinUtil;
import noppes.mpm.client.gui.util.GuiNPCInterface;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.GuiNpcLabel;
import noppes.mpm.client.gui.util.GuiNpcTextField;

public class GuiConfig extends GuiNPCInterface {
   public GuiConfig() {
      this.xSize = 366;
      this.ySize = 226;
      this.closeOnEsc = true;
      this.setBackground("menubg.png");
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      int y = this.guiTop + 20;
      int rowWidth = 180;
      this.addButton(new GuiNpcButton(this, 47, this.guiLeft + 104, y + 22, 60, 20, new String[]{"gui.no", "gui.yes"}, MorePlayerModels.EnablePOV ? 1 : 0));
      this.addLabel(new GuiNpcLabel(47, "config.pov", this.guiLeft + 4, y + 27));
      int var10005 = this.guiLeft + 104 + rowWidth;
      y += 22;
      this.addButton(new GuiNpcButton(this, 48, var10005, y, 60, 20, new String[]{"gui.no", "gui.yes"}, MorePlayerModels.EnableChatBubbles ? 1 : 0));
      this.addLabel(new GuiNpcLabel(48, "config.chatbubbles", this.guiLeft + rowWidth, y + 5));
      this.addButton(new GuiNpcButton(this, 49, this.guiLeft + 104, y + 22, 60, 20, new String[]{"gui.no", "gui.yes"}, MorePlayerModels.EnableBackItem ? 1 : 0));
      this.addLabel(new GuiNpcLabel(49, "config.backitem", this.guiLeft + 4, y + 27));
      var10005 = this.guiLeft + 104 + rowWidth;
      y += 22;
      this.addButton(new GuiNpcButton(this, 50, var10005, y, 60, 20, new String[]{"gui.no", "1", "2", "3", "4"}, MorePlayerModels.Tooltips));
      this.addLabel(new GuiNpcLabel(50, "config.tooltip", this.guiLeft + rowWidth, y + 5));
      this.addButton(
         new GuiNpcButton(this, 57, this.guiLeft + 104 + rowWidth, y + 22, 60, 20, new String[]{"gui.yes", "gui.no"}, MorePlayerModels.HidePlayerNames ? 1 : 0)
      );
      this.addLabel(new GuiNpcLabel(57, "config.names", this.guiLeft + rowWidth, y + 27));
      var10005 = this.guiLeft + 104;
      y += 22;
      this.addButton(new GuiNpcButton(this, 53, var10005, y, 60, 20, new String[]{"gui.no", "gui.yes"}, MorePlayerModels.EnableParticles ? 1 : 0));
      this.addLabel(new GuiNpcLabel(53, "config.particles", this.guiLeft + 4, y + 5));
      this.addButton(
         new GuiNpcButton(this, 56, this.guiLeft + 104 + rowWidth, y + 22, 60, 20, new String[]{"gui.yes", "gui.no"}, MorePlayerModels.HideSelectionBox ? 1 : 0)
      );
      this.addLabel(new GuiNpcLabel(56, "config.blockhighlight", this.guiLeft + rowWidth, y + 27));
      var10005 = this.guiLeft + 104;
      y += 22;
      this.addButton(new GuiNpcButton(this, 54, var10005, y, 60, 20, new String[]{"gui.no", "gui.yes"}, MorePlayerModels.HeadWearType));
      this.addLabel(new GuiNpcLabel(54, "config.solidheadlayer", this.guiLeft + 4, y + 5));
      this.addButton(
         new GuiNpcButton(this, 55, this.guiLeft + 104 + rowWidth, y + 22, 60, 20, new String[]{"gui.no", "gui.yes"}, MorePlayerModels.Compatibility ? 1 : 0)
      );
      this.addLabel(new GuiNpcLabel(55, "config.compatibility", this.guiLeft + rowWidth, y + 27));
      var10005 = this.guiLeft + 104;
      y += 22;
      this.addButton(new GuiNpcButton(this, 58, var10005, y, 60, 20, new String[]{"gui.no", "gui.yes"}, MorePlayerModels.AllowFullyInvisibleSkins ? 1 : 0));
      this.addLabel(new GuiNpcLabel(58, "config.allowinvisibleskins", this.guiLeft + 4, y + 5));
      this.addTextField(new GuiNpcTextField(this, 60, this.guiLeft + 104 + rowWidth, y + 22, 60, 20, MorePlayerModels.ScaleSizeMax + "", field -> {
         try {
            MorePlayerModels.ScaleSizeMax = Float.parseFloat(field.m_94155_());
            MorePlayerModels.instance.configLoader.updateConfig();
         } catch (NumberFormatException var2x) {
            field.m_94144_(MorePlayerModels.ScaleSizeMax + "");
         }
      }));
      this.addLabel(new GuiNpcLabel(60, "config.scaleSizeMax", this.guiLeft + rowWidth, y + 27));
      var10005 = this.guiLeft + 104;
      y += 22;
      this.addTextField(new GuiNpcTextField(this, 59, var10005, y, 60, 20, MorePlayerModels.ScaleSizeMin + "", field -> {
         try {
            MorePlayerModels.ScaleSizeMin = Float.parseFloat(field.m_94155_());
            MorePlayerModels.instance.configLoader.updateConfig();
         } catch (NumberFormatException var2x) {
            field.m_94144_(MorePlayerModels.ScaleSizeMin + "");
         }
      }));
      this.addLabel(new GuiNpcLabel(59, "config.scaleSizeMin", this.guiLeft + 4, y + 5));
      this.addButton(new GuiNpcButton(this, 66, this.guiLeft + this.xSize - 24, this.guiTop + 4, 20, 20, "X"));
   }

   @Override
   public void buttonEvent(GuiNpcButton button) {
      if (button.id == 47) {
         MorePlayerModels.EnablePOV = button.getValue() == 1;
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 48) {
         MorePlayerModels.EnableChatBubbles = button.getValue() == 1;
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 49) {
         MorePlayerModels.EnableBackItem = button.getValue() == 1;
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 50) {
         MorePlayerModels.Tooltips = button.getValue();
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 53) {
         MorePlayerModels.EnableParticles = button.getValue() == 1;
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 54) {
         MorePlayerModels.HeadWearType = button.getValue();
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 55) {
         MorePlayerModels.Compatibility = button.getValue() == 1;
         MorePlayerModels.instance.configLoader.updateConfig();
         ClientProxy.fixModels();
      }

      if (button.id == 56) {
         MorePlayerModels.HideSelectionBox = button.getValue() == 1;
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 57) {
         MorePlayerModels.HidePlayerNames = button.getValue() == 1;
         MorePlayerModels.instance.configLoader.updateConfig();
      }

      if (button.id == 58) {
         MorePlayerModels.AllowFullyInvisibleSkins = button.getValue() == 1;
         MorePlayerModels.instance.configLoader.updateConfig();
         SkinUtil.reloadSkins();
      }

      if (button.id == 66) {
         this.close();
      }
   }

   @Override
   public void save() {
   }
}
