package noppes.mpm.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import noppes.mpm.client.gui.util.GuiCustomScroll;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.ICustomScrollListener;
import noppes.mpm.util.MPMEntityUtil;

public class GuiCreationEntities extends GuiCreationScreenInterface implements ICustomScrollListener {
   public HashMap<String, ResourceLocation> data = new HashMap<>();
   private List<String> list;
   private GuiCustomScroll scroll;
   private boolean resetToSelected = true;

   public GuiCreationEntities() {
      this.data = MPMEntityUtil.getAllEntities(Minecraft.m_91087_().f_91073_);
      this.list = new ArrayList<>(this.data.keySet());
      this.list.add(I18n.m_118938_("gui.player", new Object[0]));
      Collections.sort(this.list, String.CASE_INSENSITIVE_ORDER);
      this.active = 1;
      this.xOffset = 60;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.addButton(new GuiNpcButton(this, 10, this.guiLeft, this.guiTop + 46, 120, 20, "gui.resettoplayer"));
      if (this.scroll == null) {
         this.scroll = new GuiCustomScroll(this, 0);
         this.scroll.setUnsortedList(this.list);
      }

      this.scroll.guiLeft = this.guiLeft;
      this.scroll.guiTop = this.guiTop + 68;
      this.scroll.setSize(100, this.ySize - 70);
      String selected = I18n.m_118938_("gui.player", new Object[0]);
      if (this.playerdata.hasEntity()) {
         for (Entry<String, ResourceLocation> en : this.data.entrySet()) {
            if (en.getValue().equals(this.playerdata.getEntityName())) {
               selected = en.getKey();
            }
         }
      }

      this.scroll.setSelected(selected);
      if (this.resetToSelected) {
         this.scroll.scrollTo(this.scroll.getSelected());
         this.resetToSelected = false;
      }

      this.addScroll(this.scroll);
   }

   @Override
   public void buttonEvent(GuiNpcButton btn) {
      if (btn.id == 10) {
         this.playerdata.setEntity(null);
         this.resetToSelected = true;
         this.m_7856_();
      }
   }

   @Override
   public void scrollClicked(double i, double j, int k, GuiCustomScroll scroll) {
      ResourceLocation resource = this.data.get(scroll.getSelected());
      this.playerdata.setEntity(resource);
      this.m_7856_();
   }

   @Override
   public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
   }
}
