package noppes.mpm.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import noppes.mpm.ModelData;
import noppes.mpm.client.Preset;
import noppes.mpm.client.PresetController;
import noppes.mpm.client.RenderEvent;
import noppes.mpm.client.SkinUtil;
import noppes.mpm.client.gui.util.GuiCustomScroll;
import noppes.mpm.client.gui.util.GuiLinkButton;
import noppes.mpm.client.gui.util.GuiNPCInterface;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.ICustomScrollListener;
import noppes.mpm.client.gui.util.ISubGuiListener;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.server.PacketPlayerDataUpdate;

public class GuiMPM extends GuiNPCInterface implements ICustomScrollListener, ISubGuiListener {
   public static final ResourceLocation resource = new ResourceLocation("moreplayermodels", "textures/gui/smallbg.png");
   public ModelData playerdata;
   protected CompoundTag original = new CompoundTag();
   private GuiCustomScroll scroll = null;
   private static boolean showMenu = false;

   public GuiMPM() {
      this.playerdata = ModelData.get(Minecraft.m_91087_().f_91074_);
      this.original = this.playerdata.writeToNBT();
      this.xSize = 182;
      this.ySize = 185;
      this.drawDefaultBackground = false;
      this.closeOnEsc = true;
      if (PresetController.instance.presets.isEmpty()) {
         PresetController.instance.load();
      }
   }

   @Override
   public void m_7856_() {
      this.xSize = showMenu ? 240 : 182;
      super.m_7856_();
      if (this.scroll == null) {
         this.scroll = new GuiCustomScroll(this, 0);
         this.scroll.setSize(90, 184);
      }

      List<String> list = new ArrayList<>();

      for (Preset preset : PresetController.instance.presets.values()) {
         if (preset.menu) {
            list.add(preset.name);
         }
      }

      this.scroll.setList(list);
      this.scroll.setSelected(this.playerdata.presetName);
      if (!this.scroll.hasSelected()) {
         this.scroll.setSelectedIndex(0);
      }

      this.scroll.guiLeft = this.guiLeft + 4;
      this.scroll.guiTop = this.guiTop + 12;
      this.addScroll(this.scroll);
      this.addButton(new GuiNpcButton(0, this.guiLeft + 98, this.guiTop + 176, 78, 20, "gui.menu", button -> {
         showMenu = !showMenu;
         this.m_7856_();
      }));
      if (showMenu) {
         this.addButton(new GuiNpcButton(1, this.guiLeft - 91, this.guiTop + 10, 90, 20, "gui.add", button -> this.setSubGui(new GuiCreationLoad())));
         this.addButton(new GuiNpcButton(2, this.guiLeft - 91, this.guiTop + 31, 90, 20, "gui.remove", button -> {
            ConfirmScreen gui = new ConfirmScreen(result -> {
               if (result) {
                  PresetController.instance.removePreset(this.scroll.getSelected());
                  this.scroll.getList().remove(this.scroll.getSelected());
                  Preset presetx = PresetController.instance.getPreset(this.scroll.getList().get(0));
                  this.playerdata.readFromNBT(presetx.data.writeToNBT());
                  this.playerdata.presetName = presetx.name;
               }

               Minecraft.m_91087_().m_91152_(this);
            }, Component.m_237119_(), Component.m_237115_("message.delete"));
            this.f_96541_.m_91152_(gui);
         }));
         this.addButton(new GuiNpcButton(3, this.guiLeft - 91, this.guiTop + 52, 90, 20, "gui.editmodel", button -> {
            try {
               this.setSubGui((GuiNPCInterface)GuiCreationScreenInterface.Gui.getClass().newInstance());
            } catch (InstantiationException var3x) {
            } catch (IllegalAccessException var4) {
            }
         }));
         this.addButton(new GuiNpcButton(8, this.guiLeft - 91, this.guiTop + 73, 90, 20, "gui.parts", button -> {
            ModelData data = ModelData.get(this.player);
            this.setSubGui(new GuiCreationNewParts(data));
         }));
         this.addButton(new GuiNpcButton(4, this.guiLeft - 91, this.guiTop + 94, 90, 20, "gui.options", button -> {
            ModelData data = ModelData.get(this.player);
            this.setSubGui(new GuiCreationOptions(data));
         }));
         this.getButton(2).f_93623_ = this.getButton(3).f_93623_ = this.getButton(4).f_93623_ = this.scroll.getList().size() > 1;
         this.addButton(new GuiNpcButton(5, this.guiLeft - 91, this.guiTop + 138, 90, 20, "gui.config", button -> this.setSubGui(new GuiConfig())));
         this.addButton(new GuiNpcButton(6, this.guiLeft - 91, this.guiTop + 159, 90, 20, "gui.emotes", button -> this.setSubGui(new GuiEditEmotes())));
         this.addButton(new GuiNpcButton(7, this.guiLeft - 91, this.guiTop + 180, 90, 20, "config.reloadskins", button -> SkinUtil.reloadSkins()));
         this.m_142416_(
            new GuiLinkButton(this.guiLeft + 184, this.guiTop + 40, Component.m_237113_("- Website"), button -> this.setScreen(new ConfirmLinkScreen(bo -> {
               if (bo) {
                  Util.m_137581_().m_137646_("http://www.kodevelopment.nl/minecraft/moreplayermodels/");
               }

               this.setScreen(this);
            }, "http://www.kodevelopment.nl/minecraft/moreplayermodels/", true)))
         );
         this.m_142416_(
            new GuiLinkButton(
               this.guiLeft + 184, this.guiTop + 52, 7506394, Component.m_237113_("- Discord"), button -> this.setScreen(new ConfirmLinkScreen(bo -> {
                  if (bo) {
                     Util.m_137581_().m_137646_("http://www.kodevelopment.nl/discord");
                  }

                  this.setScreen(this);
               }, "http://www.kodevelopment.nl/discord", true))
            )
         );
         this.m_142416_(
            new GuiLinkButton(
               this.guiLeft + 184, this.guiTop + 64, 16345172, Component.m_237113_("- Patreon"), button -> this.setScreen(new ConfirmLinkScreen(bo -> {
                  if (bo) {
                     Util.m_137581_().m_137646_("https://www.patreon.com/Noppes");
                  }

                  this.setScreen(this);
               }, "https://www.patreon.com/Noppes", true))
            )
         );
         this.m_142416_(
            new GuiLinkButton(
               this.guiLeft + 184, this.guiTop + 76, 16766720, Component.m_237113_("- Part packs"), button -> this.setScreen(new ConfirmLinkScreen(bo -> {
                  if (bo) {
                     Util.m_137581_().m_137646_("http://www.kodevelopment.nl/minecraft/moreplayermodels/packs/");
                  }

                  this.setScreen(this);
               }, "http://www.kodevelopment.nl/minecraft/moreplayermodels/packs/", true))
            )
         );
      }
   }

   @Override
   public void m_88315_(GuiGraphics graphics, int x, int y, float f) {
      this.m_280273_(graphics);
      graphics.m_280218_(resource, this.guiLeft, this.guiTop + 8, 0, 0, this.xSize, 192);
      super.m_88315_(graphics, x, y, f);
      LivingEntity entity = this.playerdata.getEntity(this.f_96541_.f_91074_);
      if (entity == null) {
         entity = this.player;
      }

      if (!this.hasSubGui()) {
         RenderEvent.entityResource = this.playerdata.resourceLocation;
         InventoryScreen.m_274545_(graphics, this.guiLeft + 140, this.guiTop + 140, 56, this.guiLeft + 120 - x, this.guiTop + 50 - y, entity);
         if (showMenu) {
            IModInfo info = ModList.get().getMods().stream().filter(t -> t.getModId().equals("moreplayermodels")).findFirst().get();
            graphics.m_280430_(
               this.f_96547_, Component.m_237113_("More Player Models " + info.getVersion().toString()), this.guiLeft + 184, this.guiTop + 8, 16777215
            );
            graphics.m_280430_(this.f_96547_, Component.m_237113_("by Noppes"), this.guiLeft + 184, this.guiTop + 18, 16777215);
         }
      }
   }

   @Override
   public void scrollClicked(double i, double j, int button, GuiCustomScroll scroll) {
      Preset preset = PresetController.instance.getPreset(scroll.getSelected());
      if (preset != null) {
         this.playerdata.readFromNBT(preset.data.writeToNBT());
         this.playerdata.presetName = preset.name;
      }
   }

   @Override
   public void save() {
      CompoundTag newCompound = this.playerdata.writeToNBT();
      if (!this.original.equals(newCompound)) {
         this.playerdata.save();
         this.playerdata.lastEdited = System.currentTimeMillis();
         Packets.sendServer(new PacketPlayerDataUpdate(newCompound));
         this.original = newCompound;
      }
   }

   @Override
   public void subGuiClosed(GuiNPCInterface subgui) {
      if (subgui instanceof GuiCreationScreenInterface || subgui instanceof GuiCreationOptions || subgui instanceof GuiCreationNewParts) {
         Preset p = PresetController.instance.getPreset(this.getScroll(0).getSelected());
         if (p != null) {
            p.data = this.playerdata.copy();
            PresetController.instance.save();
         }
      }
   }

   @Override
   public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
   }
}
