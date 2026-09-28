package noppes.mpm.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

public class TextBlockClient {
   public List<Component> lines = new ArrayList<>();
   private Style style;
   public int color = 14737632;
   public String name;

   public TextBlockClient(String name, String text, int lineWidth) {
      this(text, lineWidth, Minecraft.m_91087_().f_91074_);
      this.name = name;
   }

   public TextBlockClient(String name, String text, int lineWidth, int color) {
      this(name, text, lineWidth);
      this.color = color;
   }

   public TextBlockClient(String text, int lineWidth) {
      this(text, lineWidth, Minecraft.m_91087_().f_91074_);
   }

   public TextBlockClient(Component text, int lineWidth) {
      this.lines.add(text);
   }

   public TextBlockClient(String text, int lineWidth, Player player) {
      this.style = Style.f_131099_;
      String line = "";
      text = text.replaceAll("\n", " \n ");
      text = text.replaceAll("\r", " \r ");
      String[] words = text.split(" ");
      Font font = Minecraft.m_91087_().f_91062_;

      for (String word : words) {
         if (!word.isEmpty()) {
            if (word.length() == 1) {
               char c = word.charAt(0);
               if (c == '\r' || c == '\n') {
                  this.addLine(line);
                  line = "";
                  continue;
               }
            }

            String newLine;
            if (line.isEmpty()) {
               newLine = word;
            } else {
               newLine = line + " " + word;
            }

            if (font.m_92895_(newLine) > lineWidth) {
               this.addLine(line);
               line = word.trim();
            } else {
               line = newLine;
            }
         }
      }

      if (!line.isEmpty()) {
         this.addLine(line);
      }
   }

   private void addLine(String text) {
      MutableComponent line = Component.m_237115_(text);
      line.m_6270_(this.style);
      this.lines.add(line);
   }
}
