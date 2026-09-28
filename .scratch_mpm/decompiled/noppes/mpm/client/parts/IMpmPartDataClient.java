package noppes.mpm.client.parts;

import noppes.mpm.constants.EnumAnimation;

public interface IMpmPartDataClient<T extends MpmPart> {
   void start(T var1);

   boolean animation(T var1, EnumAnimation var2, int var3, float var4);

   boolean animation(T var1, EnumAnimation var2, float var3);
}
