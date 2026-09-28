package noppes.mpm.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import noppes.mpm.ModelData;
import noppes.mpm.client.gui.select.GuiTextureSelection;
import noppes.mpm.client.gui.util.GuiNPCInterface;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.GuiNpcLabel;
import noppes.mpm.client.gui.util.GuiNpcSlider;
import noppes.mpm.client.gui.util.ISliderListener;
import noppes.mpm.client.gui.util.ISubGuiListener;
import noppes.mpm.util.MPMEntityUtil;

public abstract class GuiCreationScreenInterface extends GuiNPCInterface implements ISubGuiListener, ISliderListener {
   public static String Message = "";
   public LivingEntity entity;
   public int active = 0;
   private Player player;
   public int xOffset = 0;
   public ModelData playerdata = ModelData.get(Minecraft.m_91087_().f_91074_);
   public static GuiCreationScreenInterface Gui = new GuiCreationEntities();
   private static float rotation = 0.5F;

   public GuiCreationScreenInterface() {
      this.xSize = 400;
      this.ySize = 240;
      this.xOffset = 140;
      this.player = Minecraft.m_91087_().f_91074_;
      this.closeOnEsc = true;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.entity = this.playerdata.getEntity(this.f_96541_.f_91074_);
      this.addButton(new GuiNpcButton(this, 1, this.guiLeft + 62, this.guiTop, 60, 20, "gui.entity") {
         @Override
         public void m_5716_(double x, double y) {
            GuiCreationScreenInterface.this.openGui(new GuiCreationEntities());
         }
      });
      if (this.entity != null) {
         GuiCreationExtra gui = new GuiCreationExtra();
         gui.playerdata = this.playerdata;
         if (!gui.getData(this.entity).isEmpty()) {
            this.addButton(new GuiNpcButton(this, 2, this.guiLeft, this.guiTop + 23, 60, 20, "gui.extra") {
               @Override
               public void m_5716_(double x, double y) {
                  GuiCreationScreenInterface.this.openGui(new GuiCreationExtra());
               }
            });
         } else if (this.active == 2) {
            this.openGui(new GuiCreationEntities());
            return;
         }
      }

      if (this.entity == null) {
         this.addButton(new GuiNpcButton(this, 3, this.guiLeft + 62, this.guiTop + 23, 60, 20, "gui.scale") {
            @Override
            public void m_5716_(double x, double y) {
               GuiCreationScreenInterface.this.openGui(new GuiCreationScale());
            }
         });
      }

      this.getButton(this.active).f_93623_ = false;
      this.addButton(new GuiNpcButton(this, 66, this.guiLeft + this.xSize - 20, this.guiTop, 20, 20, "X") {
         @Override
         public void m_5716_(double x, double y) {
            GuiCreationScreenInterface.this.close();
         }
      });
      this.addLabel(new GuiNpcLabel(0, Message, this.guiLeft + 120, this.guiTop + this.ySize - 10, 16711680));
      this.getLabel(0).center(this.xSize - 120);
      this.addSlider(new GuiNpcSlider(this, 500, this.guiLeft + this.xOffset + 142, this.guiTop + 210, 120, 20, rotation));
   }

   @Override
   public void m_88315_(GuiGraphics graphics, int x, int y, float f) {
      super.m_88315_(graphics, x, y, f);
      this.entity = this.playerdata.getEntity(this.f_96541_.f_91074_);
      LivingEntity entity = this.entity;
      if (entity == null) {
         LivingEntity var6 = this.player;
      } else {
         MPMEntityUtil.copy(this.f_96541_.f_91074_, this.player);
      }

      if (!(this.getSubGui() instanceof GuiTextureSelection)) {
         this.drawEntity(graphics, this.player, this.xOffset + 200, 200, 2.0F, (int)(-rotation * 360.0F) + 180, this.guiLeft, this.guiTop);
      }
   }

   @Override
   public void save() {
   }

   @Override
   public boolean drawSubGuiBackground() {
      return true;
   }

   public void openGui(GuiNPCInterface gui) {
      this.parent.setSubGui(gui);
      if (gui instanceof GuiCreationScreenInterface) {
         Gui = (GuiCreationScreenInterface)gui;
      }
   }

   @Override
   public void subGuiClosed(GuiNPCInterface subgui) {
      this.m_7856_();
   }

   @Override
   public void mouseDragged(GuiNpcSlider slider) {
      if (slider.id == 500) {
         rotation = slider.sliderValue;
         slider.setString((int)(rotation * 360.0F) + "");
      }
   }
}
