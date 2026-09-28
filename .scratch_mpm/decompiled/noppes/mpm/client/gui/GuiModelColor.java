package noppes.mpm.client.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import noppes.mpm.client.gui.util.GuiNPCInterface;
import noppes.mpm.client.gui.util.GuiNpcButton;
import noppes.mpm.client.gui.util.GuiNpcTextField;
import noppes.mpm.client.gui.util.ITextfieldListener;

public class GuiModelColor extends GuiNPCInterface implements ITextfieldListener {
   private Screen parent;
   private static final ResourceLocation colorPicker = new ResourceLocation("moreplayermodels:textures/gui/color.png");
   private static final ResourceLocation colorgui = new ResourceLocation("moreplayermodels:textures/gui/color_gui.png");
   private int colorX;
   private int colorY;
   private GuiNpcTextField textfield;
   public int color;
   private GuiModelColor.ColorCallback callback;

   public GuiModelColor(Screen parent, int color, GuiModelColor.ColorCallback callback) {
      this.parent = parent;
      this.callback = callback;
      this.ySize = 230;
      this.closeOnEsc = false;
      this.background = colorgui;
      this.color = color;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.colorX = this.guiLeft + 4;
      this.colorY = this.guiTop + 50;
      this.addTextField(this.textfield = new GuiNpcTextField(0, this, this.guiLeft + 35, this.guiTop + 25, 60, 20, this.getColor()));
      this.addButton(new GuiNpcButton(this, 66, this.guiLeft + 107, this.guiTop + 8, 20, 20, "X"));
      this.textfield.m_94202_(this.color);
   }

   @Override
   public void buttonEvent(GuiNpcButton guibutton) {
      if (guibutton.id == 66) {
         this.close();
      }
   }

   @Override
   public boolean m_5534_(char c, int i) {
      String prev = this.textfield.m_94155_();
      boolean bo = super.m_5534_(c, i);
      String newText = this.textfield.m_94155_();
      if (newText.equals(prev)) {
         return bo;
      } else {
         try {
            this.color = Integer.parseInt(this.textfield.m_94155_(), 16);
            this.callback.color(this.color);
            this.textfield.m_94202_(this.color);
         } catch (NumberFormatException var7) {
            this.textfield.m_94144_(prev);
         }

         return bo;
      }
   }

   @Override
   public void m_88315_(GuiGraphics graphics, int par1, int par2, float par3) {
      super.m_88315_(graphics, par1, par2, par3);
      graphics.m_280218_(colorPicker, this.colorX, this.colorY, 0, 0, 120, 120);
   }

   @Override
   public boolean m_6375_(double i, double j, int k) {
      boolean bo = super.m_6375_(i, j, k);
      if (!(i < this.colorX) && !(i > this.colorX + 120) && !(j < this.colorY) && !(j > this.colorY + 120)) {
         InputStream stream = null;

         try {
            Resource resource = (Resource)this.f_96541_.m_91098_().m_213713_(colorPicker).get();
            BufferedImage bufferedimage = ImageIO.read(stream = resource.m_215507_());
            int color = bufferedimage.getRGB((int)(i - this.guiLeft - 4.0) * 4, (int)(j - this.guiTop - 50.0) * 4) & 16777215;
            if (color != 0) {
               this.color = color;
               this.callback.color(color);
               this.textfield.m_94202_(color);
               this.textfield.m_94144_(this.getColor());
            }
         } catch (IOException var19) {
         } finally {
            if (stream != null) {
               try {
                  stream.close();
               } catch (IOException var18) {
               }
            }
         }

         return true;
      } else {
         return bo;
      }
   }

   @Override
   public void unFocused(GuiNpcTextField textfield) {
      try {
         this.color = Integer.parseInt(textfield.m_94155_(), 16);
      } catch (NumberFormatException var3) {
         this.color = 0;
      }

      this.callback.color(this.color);
      textfield.m_94202_(this.color);
   }

   public String getColor() {
      String str = Integer.toHexString(this.color);

      while (str.length() < 6) {
         str = "0" + str;
      }

      return str;
   }

   @Override
   public void save() {
   }

   public interface ColorCallback {
      void color(int var1);
   }
}
