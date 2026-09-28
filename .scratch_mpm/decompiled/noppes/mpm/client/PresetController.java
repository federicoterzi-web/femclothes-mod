package noppes.mpm.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import noppes.mpm.LogWriter;
import noppes.mpm.ModelData;

public class PresetController {
   public HashMap<String, Preset> presets = new HashMap<>();
   private File dir;
   public static PresetController instance;

   public PresetController(File dir) {
      instance = this;
      this.dir = dir;
   }

   public Preset getPreset(String username) {
      if (this.presets.isEmpty()) {
         this.load();
      }

      return username != null && !username.isEmpty() ? this.presets.get(username.toLowerCase()) : null;
   }

   public void load() {
      CompoundTag compound = this.loadPreset();
      HashMap<String, Preset> presets = new HashMap<>();
      if (compound != null) {
         ListTag list = compound.m_128437_("Presets", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag comp = list.m_128728_(i);
            Preset preset = new Preset();
            preset.readFromNBT(comp);
            presets.put(preset.name.toLowerCase(), preset);
         }

         if (compound.m_128441_("PresetSelected")) {
            ModelData data = ModelData.get(Minecraft.m_91087_().f_91074_);
            if (data.presetName.isEmpty()) {
               data.presetName = compound.m_128461_("PresetSelected");
            }
         }
      }

      if (presets.isEmpty()) {
         Preset preset = new Preset();
         preset.data = ModelData.get(Minecraft.m_91087_().f_91074_);
         preset.name = "Default";
         preset.menu = true;
         presets.put("default", preset);
         ModelData data = new ModelData();
         preset = new Preset();
         preset.name = "Normal";
         preset.data = data;
         preset.menu = true;
         presets.put("normal", preset);
      }

      this.presets = presets;
   }

   private CompoundTag loadPreset() {
      String filename = "presets.dat";

      try {
         File file = new File(this.dir, filename);
         return !file.exists() ? null : NbtIo.m_128939_(new FileInputStream(file));
      } catch (Exception var4) {
         LogWriter.except(var4);

         try {
            File filex = new File(this.dir, filename + "_old");
            return !filex.exists() ? null : NbtIo.m_128939_(new FileInputStream(filex));
         } catch (Exception var3) {
            LogWriter.except(var3);
            return null;
         }
      }
   }

   public void save() {
      CompoundTag compound = new CompoundTag();
      ListTag list = new ListTag();

      for (Preset preset : this.presets.values()) {
         list.add(preset.writeToNBT());
      }

      compound.m_128365_("Presets", list);
      this.savePreset(compound);
   }

   private void savePreset(CompoundTag compound) {
      String filename = "presets.dat";

      try {
         File file = new File(this.dir, filename + "_new");
         File file1 = new File(this.dir, filename + "_old");
         File file2 = new File(this.dir, filename);
         NbtIo.m_128947_(compound, new FileOutputStream(file));
         if (file1.exists()) {
            file1.delete();
         }

         file2.renameTo(file1);
         if (file2.exists()) {
            file2.delete();
         }

         file.renameTo(file2);
         if (file.exists()) {
            file.delete();
         }
      } catch (Exception var6) {
         LogWriter.except(var6);
         var6.printStackTrace();
      }
   }

   public void addPreset(Preset preset) {
      this.presets.put(preset.name.toLowerCase(), preset);
      this.save();
   }

   public void removePreset(String preset) {
      if (preset != null) {
         this.presets.remove(preset.toLowerCase());
         this.save();
      }
   }
}
