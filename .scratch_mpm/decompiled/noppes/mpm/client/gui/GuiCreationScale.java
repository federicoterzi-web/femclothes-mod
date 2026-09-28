package noppes.mpm.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.language.I18n;
import noppes.mpm.ModelPartConfig;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.client.gui.util.GuiCustomScroll;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.GuiNpcLabel;
import noppes.mpm.client.gui.util.GuiNpcSlider;
import noppes.mpm.client.gui.util.ICustomScrollListener;
import noppes.mpm.client.gui.util.ISliderListener;
import noppes.mpm.constants.EnumParts;

public class GuiCreationScale extends GuiCreationScreenInterface implements ISliderListener, ICustomScrollListener {
   private GuiCustomScroll scroll;
   private List<EnumParts> data = new ArrayList<>();
   private static EnumParts selected = EnumParts.HEAD;

   public GuiCreationScale() {
      this.active = 3;
      this.xOffset = 140;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      if (this.scroll == null) {
         this.scroll = new GuiCustomScroll(this, 0);
      }

      ArrayList list = new ArrayList();
      EnumParts[] parts = new EnumParts[]{EnumParts.HEAD, EnumParts.BODY, EnumParts.ARM_LEFT, EnumParts.ARM_RIGHT, EnumParts.LEG_LEFT, EnumParts.LEG_RIGHT};
      this.data.clear();

      for (EnumParts part : parts) {
         if (part == EnumParts.ARM_RIGHT) {
            ModelPartConfig config = this.playerdata.getPartConfig(EnumParts.ARM_LEFT);
            if (!config.notShared) {
               continue;
            }
         }

         if (part == EnumParts.LEG_RIGHT) {
            ModelPartConfig config = this.playerdata.getPartConfig(EnumParts.LEG_LEFT);
            if (!config.notShared) {
               continue;
            }
         }

         this.data.add(part);
         list.add(I18n.m_118938_("part." + part.name, new Object[0]));
      }

      this.scroll.setUnsortedList(list);
      this.scroll.setSelected(I18n.m_118938_("part." + selected.name, new Object[0]));
      this.scroll.guiLeft = this.guiLeft;
      this.scroll.guiTop = this.guiTop + 46;
      this.scroll.setSize(100, this.ySize - 74);
      this.addScroll(this.scroll);
      ModelPartConfig config = this.playerdata.getPartConfig(selected);
      int y = this.guiTop + 65;
      this.addLabel(new GuiNpcLabel(10, "scale.width", this.guiLeft + 102, y + 5, 16777215));
      this.addSlider(
         new GuiNpcSlider(
            this,
            10,
            this.guiLeft + 150,
            y,
            100,
            20,
            (config.scaleX - MorePlayerModels.ScaleSizeMin) / (MorePlayerModels.ScaleSizeMax - MorePlayerModels.ScaleSizeMin)
         )
      );
      y += 22;
      this.addLabel(new GuiNpcLabel(11, "scale.height", this.guiLeft + 102, y + 5, 16777215));
      this.addSlider(
         new GuiNpcSlider(
            this,
            11,
            this.guiLeft + 150,
            y,
            100,
            20,
            (config.scaleY - MorePlayerModels.ScaleSizeMin) / (MorePlayerModels.ScaleSizeMax - MorePlayerModels.ScaleSizeMin)
         )
      );
      y += 22;
      this.addLabel(new GuiNpcLabel(12, "scale.depth", this.guiLeft + 102, y + 5, 16777215));
      this.addSlider(
         new GuiNpcSlider(
            this,
            12,
            this.guiLeft + 150,
            y,
            100,
            20,
            (config.scaleZ - MorePlayerModels.ScaleSizeMin) / (MorePlayerModels.ScaleSizeMax - MorePlayerModels.ScaleSizeMin)
         )
      );
      if (selected == EnumParts.ARM_LEFT || selected == EnumParts.LEG_LEFT) {
         y += 22;
         this.addLabel(new GuiNpcLabel(13, "scale.shared", this.guiLeft + 102, y + 5, 16777215));
         this.addButton(new GuiNpcButton(this, 13, this.guiLeft + 150, y, 50, 20, new String[]{"gui.no", "gui.yes"}, config.notShared ? 0 : 1));
      }
   }

   @Override
   public void buttonEvent(GuiNpcButton btn) {
      if (btn.id == 13) {
         boolean bo = btn.getValue() == 0;
         this.playerdata.getPartConfig(selected).notShared = bo;
         this.m_7856_();
      }
   }

   @Override
   public void mouseDragged(GuiNpcSlider slider) {
      super.mouseDragged(slider);
      if (slider.id >= 10 && slider.id <= 12) {
         int percent = (int)(
            MorePlayerModels.ScaleSizeMin * 100.0F + slider.sliderValue * (MorePlayerModels.ScaleSizeMax - MorePlayerModels.ScaleSizeMin) * 100.0F
         );
         slider.setString(percent + "%");
         ModelPartConfig config = this.playerdata.getPartConfig(selected);
         if (slider.id == 10) {
            config.scaleX = MorePlayerModels.ScaleSizeMin + slider.sliderValue * (MorePlayerModels.ScaleSizeMax - MorePlayerModels.ScaleSizeMin);
         }

         if (slider.id == 11) {
            config.scaleY = MorePlayerModels.ScaleSizeMin + slider.sliderValue * (MorePlayerModels.ScaleSizeMax - MorePlayerModels.ScaleSizeMin);
         }

         if (slider.id == 12) {
            config.scaleZ = MorePlayerModels.ScaleSizeMin + slider.sliderValue * (MorePlayerModels.ScaleSizeMax - MorePlayerModels.ScaleSizeMin);
         }

         this.playerdata.updateTransate();
      }
   }

   @Override
   public void scrollClicked(double i, double j, int k, GuiCustomScroll scroll) {
      if (scroll.getSelectedIndex() >= 0) {
         selected = this.data.get(scroll.getSelectedIndex());
         this.m_7856_();
      }
   }

   @Override
   public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
   }
}
