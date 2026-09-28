package noppes.mpm.client.gui.select;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.mpm.ModelData;
import noppes.mpm.client.RenderEvent;
import noppes.mpm.client.gui.util.GuiCustomScroll;
import noppes.mpm.client.gui.util.GuiNPCInterface;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.ICustomScrollListener;
import noppes.mpm.shared.client.AssetsFinder;
import noppes.mpm.shared.util.NopCallback;
import noppes.mpm.util.MPMEntityUtil;

public class GuiTextureSelection extends GuiNPCInterface implements ICustomScrollListener {
   private String up = "..<" + I18n.m_118938_("gui.up", new Object[0]) + ">..";
   private GuiCustomScroll scrollCategories;
   private GuiCustomScroll scrollQuests;
   public String title = "";
   private String location = "";
   private String selectedDomain;
   private ModelData playerdata;
   public ResourceLocation prevResource;
   public ResourceLocation selectedResource;
   private static final HashMap<String, List<ResourceLocation>> domains = new HashMap<>();
   private static final HashMap<String, ResourceLocation> textures = new HashMap<>();
   private NopCallback<ResourceLocation> callback;

   public GuiTextureSelection(ModelData playerdata, ResourceLocation resource, NopCallback<ResourceLocation> callback) {
      this.callback = callback;
      this.playerdata = playerdata;
      this.prevResource = resource;
      this.selectedResource = resource;
      this.drawDefaultBackground = false;
      this.setBackground("menubg.png");
      this.xSize = 366;
      this.ySize = 226;
      if (domains.isEmpty()) {
         for (ResourceLocation loc : AssetsFinder.find("textures", ".png")) {
            domains.computeIfAbsent(loc.m_135827_(), k -> new ArrayList<>()).add(loc);
         }
      }

      if (resource != null && !resource.m_135815_().isEmpty()) {
         this.selectedDomain = this.selectedResource.m_135827_();
         if (!domains.containsKey(this.selectedDomain)) {
            this.selectedDomain = null;
         }

         int i = this.selectedResource.m_135815_().lastIndexOf(47);
         this.location = this.selectedResource.m_135815_().substring(0, i + 1);
      }
   }

   public static void clear() {
      domains.clear();
      textures.clear();
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      if (this.selectedDomain != null) {
         this.title = this.selectedDomain + ":" + this.location;
      } else {
         this.title = "";
      }

      this.addButton(new GuiNpcButton(this, 2, this.guiLeft + 264, this.guiTop + 170, 90, 20, "gui.done"));
      this.addButton(new GuiNpcButton(this, 1, this.guiLeft + 264, this.guiTop + 190, 90, 20, "gui.cancel"));
      if (this.scrollCategories == null) {
         this.scrollCategories = new GuiCustomScroll(this, 0);
         this.scrollCategories.setSize(120, 200);
      }

      if (this.selectedDomain == null) {
         this.scrollCategories.setList(Lists.newArrayList(domains.keySet()));
         if (this.selectedDomain != null) {
            this.scrollCategories.setSelected(this.selectedDomain);
         }
      } else {
         List<String> list = new ArrayList<>();
         list.add(this.up);

         for (ResourceLocation td : domains.get(this.selectedDomain)) {
            String fullPath = td.m_135815_();
            if (fullPath.indexOf(47) >= 0) {
               fullPath = fullPath.substring(0, fullPath.lastIndexOf(47) + 1);
            }

            if (this.location.isEmpty() || fullPath.startsWith(this.location) && !fullPath.equals(this.location)) {
               String path = fullPath.substring(this.location.length());
               int i = path.indexOf(47);
               if (i >= 0) {
                  path = path.substring(0, i);
                  if (!path.isEmpty() && !list.contains(path)) {
                     list.add(path);
                  }
               }
            }
         }

         this.scrollCategories.setList(list);
      }

      this.scrollCategories.guiLeft = this.guiLeft + 4;
      this.scrollCategories.guiTop = this.guiTop + 14;
      this.addScroll(this.scrollCategories);
      if (this.scrollQuests == null) {
         this.scrollQuests = new GuiCustomScroll(this, 1);
         this.scrollQuests.setSize(130, 200);
      }

      if (this.selectedDomain != null) {
         textures.clear();
         List<ResourceLocation> data = domains.get(this.selectedDomain);
         List<String> list = new ArrayList<>();
         String loc = this.location;
         if (this.scrollCategories.hasSelected() && !this.scrollCategories.getSelected().equals(this.up)) {
            loc = loc + this.scrollCategories.getSelected() + "/";
         }

         for (ResourceLocation td : data) {
            String name = td.m_135815_();
            String path = td.m_135815_();
            if (name.indexOf(47) >= 0) {
               name = name.substring(name.lastIndexOf(47) + 1);
               path = path.substring(0, path.lastIndexOf(47) + 1);
            }

            if (path.equals(loc) && !list.contains(name)) {
               list.add(name);
               textures.put(name, td);
            }
         }

         this.scrollQuests.setList(list);
      }

      if (this.selectedResource != null) {
         this.scrollQuests.setSelected(this.selectedResource.m_135815_());
      }

      this.scrollQuests.guiLeft = this.guiLeft + 125;
      this.scrollQuests.guiTop = this.guiTop + 14;
      this.addScroll(this.scrollQuests);
   }

   @Override
   public void buttonEvent(GuiNpcButton guibutton) {
      if (guibutton.id == 1) {
         this.callback.sumbit(this.prevResource);
      }

      if (guibutton.id == 2 && this.selectedResource != null) {
         this.callback.sumbit(this.selectedResource);
      }

      this.close();
      this.parent.m_7856_();
   }

   @Override
   public void m_88315_(GuiGraphics graphics, int x, int y, float f) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, x, y, f);
      graphics.m_280137_(this.f_96547_, this.title, this.f_96543_ / 2, 8, 16777215);
      LivingEntity entity = this.playerdata.getEntity(this.f_96541_.f_91074_);
      if (entity == null) {
         entity = this.player;
      } else {
         MPMEntityUtil.copy(this.f_96541_.f_91074_, this.player);
      }

      RenderEvent.entityResource = this.playerdata.resourceLocation;
      InventoryScreen.m_274545_(graphics, this.guiLeft + 310, this.guiTop + 140, 60, this.guiLeft + 310 - x, this.guiTop + 50 - y, entity);
   }

   @Override
   public void scrollClicked(double i, double j, int k, GuiCustomScroll scroll) {
      if (scroll == this.scrollQuests) {
         if (scroll.id == 1) {
            this.selectedResource = textures.get(scroll.getSelected());
            this.callback.sumbit(this.selectedResource);
         }
      } else {
         this.m_7856_();
      }
   }

   @Override
   public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
      if (scroll == this.scrollCategories) {
         if (this.selectedDomain == null) {
            this.selectedDomain = selection;
         } else if (selection.equals(this.up)) {
            int i = this.location.lastIndexOf(47, this.location.length() - 2);
            if (i < 0) {
               if (this.location.isEmpty()) {
                  this.selectedDomain = null;
               }

               this.location = "";
            } else {
               this.location = this.location.substring(0, i + 1);
            }
         } else {
            this.location = this.location + selection + "/";
         }

         this.scrollCategories.setSelectedIndex(-1);
         this.scrollQuests.setSelectedIndex(-1);
         this.m_7856_();
      } else {
         this.close();
         this.parent.m_7856_();
      }
   }

   @Override
   public void save() {
   }
}
