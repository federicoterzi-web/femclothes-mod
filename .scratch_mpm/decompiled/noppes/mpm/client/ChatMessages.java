package noppes.mpm.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.CullStateShard;
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
import net.minecraft.client.renderer.RenderStateShard.LightmapStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import noppes.mpm.MorePlayerModels;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ChatMessages {
   private static Map<String, ChatMessages> users = new Hashtable<>();
   protected static final TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new TransparencyStateShard("translucent_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   private static final ShaderStateShard sharder = new ShaderStateShard(GameRenderer::m_172832_);
   protected static final RenderType type = RenderType.m_173215_(
      "chatbubble",
      DefaultVertexFormat.f_85816_,
      Mode.QUADS,
      256,
      false,
      false,
      CompositeState.m_110628_().m_110661_(new CullStateShard(true)).m_110671_(new LightmapStateShard(true)).m_173292_(sharder).m_110691_(true)
   );
   protected static final RenderType typeDepth = RenderType.m_173215_(
      "chatbubbledepth",
      DefaultVertexFormat.f_85816_,
      Mode.QUADS,
      256,
      false,
      true,
      CompositeState.m_110628_()
         .m_110661_(new CullStateShard(true))
         .m_110685_(TRANSLUCENT_TRANSPARENCY)
         .m_110671_(new LightmapStateShard(true))
         .m_110663_(new DepthTestStateShard("always", 519))
         .m_173292_(sharder)
         .m_110691_(false)
   );
   private Map<Long, TextBlockClient> messages = new TreeMap<>();
   private int boxLength = 46;
   private float scale = 0.5F;
   private Component lastMessage = CommonComponents.f_237098_;
   private long lastMessageTime = 0L;
   private static Pattern[] patterns = new Pattern[]{
      Pattern.compile("^<+([a-zA-z0-9_]{2,16})>[:]? (.*)"),
      Pattern.compile("^\\[.*[\\]]{1,16}[^a-zA-z0-9]?([a-zA-z0-9_]{2,16})[:]? (.*)"),
      Pattern.compile("^[a-zA-z0-9_]{2,10}[^a-zA-z0-9]([a-zA-z0-9_]{2,16})[:]? (.*)")
   };

   public void addMessage(Component message) {
      if (MorePlayerModels.EnableChatBubbles) {
         long time = System.currentTimeMillis();
         if (!message.getString().equals(this.lastMessage.getString()) || this.lastMessageTime + 1000L <= time) {
            Map<Long, TextBlockClient> messages = new TreeMap<>(this.messages);
            messages.put(time, new TextBlockClient(message, this.boxLength * 4));
            if (messages.size() > 3) {
               messages.remove(messages.keySet().iterator().next());
            }

            this.messages = messages;
            this.lastMessage = message;
            this.lastMessageTime = time;
         }
      }
   }

   public void renderMessages(PoseStack poseStack, MultiBufferSource typeBuffer, float textscale, boolean inRange, int lightmapUV) {
      Map<Long, TextBlockClient> messages = this.getMessages();
      if (!messages.isEmpty()) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShader(GameRenderer::m_172832_);
         if (inRange) {
            this.render(poseStack, typeBuffer, typeBuffer.m_6299_(typeDepth), textscale, false, lightmapUV);
         }

         this.render(poseStack, typeBuffer, typeBuffer.m_6299_(type), textscale, true, lightmapUV);
      }
   }

   public void render(PoseStack poseStack, MultiBufferSource typeBuffer, VertexConsumer ivertex, float textScale, boolean depth, int lightmapUV) {
      float var14 = 0.02666667F;
      int size = 0;

      for (TextBlockClient block : this.messages.values()) {
         size += block.lines.size();
      }

      Minecraft mc = Minecraft.m_91087_();
      Font font = mc.f_91062_;
      int textYSize = (int)(size * 9 * this.scale);
      poseStack.m_85836_();
      poseStack.m_252880_(0.0F, textYSize * var14, 0.0F);
      poseStack.m_85841_(textScale, textScale, textScale);
      poseStack.m_252781_(mc.m_91290_().m_253208_());
      poseStack.m_85841_(-var14, -var14, var14);
      int black = depth ? -16777216 : -16777216;
      int white = depth ? -1140850689 : 1157627903;
      Pose entry = poseStack.m_85850_();
      Matrix4f matrix = entry.m_252922_();
      this.drawRect(ivertex, matrix, lightmapUV, -this.boxLength - 2, -2.0F, this.boxLength + 2, textYSize + 1, white, 0.11F);
      this.drawRect(ivertex, matrix, lightmapUV, -this.boxLength - 1, -3.0F, this.boxLength + 1, -2.0F, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, -this.boxLength - 1, textYSize + 2, -1.0F, textYSize + 1, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, 3.0F, textYSize + 2, this.boxLength + 1, textYSize + 1, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, -this.boxLength - 3, -1.0F, -this.boxLength - 2, textYSize, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, this.boxLength + 3, -1.0F, this.boxLength + 2, textYSize, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, -this.boxLength - 2, -2.0F, -this.boxLength - 1, -1.0F, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, this.boxLength + 2, -2.0F, this.boxLength + 1, -1.0F, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, -this.boxLength - 2, textYSize + 1, -this.boxLength - 1, textYSize, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, this.boxLength + 2, textYSize + 1, this.boxLength + 1, textYSize, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, 0.0F, textYSize + 1, 3.0F, textYSize + 4, white, 0.11F);
      this.drawRect(ivertex, matrix, lightmapUV, -1.0F, textYSize + 4, 1.0F, textYSize + 5, white, 0.11F);
      this.drawRect(ivertex, matrix, lightmapUV, -1.0F, textYSize + 1, 0.0F, textYSize + 4, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, 3.0F, textYSize + 1, 4.0F, textYSize + 3, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, 2.0F, textYSize + 3, 3.0F, textYSize + 4, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, 1.0F, textYSize + 4, 2.0F, textYSize + 5, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, -2.0F, textYSize + 4, -1.0F, textYSize + 5, black, 0.1F);
      this.drawRect(ivertex, matrix, lightmapUV, -2.0F, textYSize + 5, 1.0F, textYSize + 6, black, 0.1F);
      poseStack.m_85841_(this.scale, this.scale, this.scale);
      int index = 0;

      for (TextBlockClient block : this.messages.values()) {
         for (Component chat : block.lines) {
            font.m_272077_(
               chat, -font.m_92852_(chat) / 2, index * 9, black, false, matrix, typeBuffer, depth ? DisplayMode.SEE_THROUGH : DisplayMode.NORMAL, 0, lightmapUV
            );
            index++;
         }
      }

      poseStack.m_85849_();
   }

   public void drawRect(VertexConsumer ivertex, Matrix4f matrix, int lightmapUV, float x, float y, float x2, float y2, int color, float z) {
      if (x < x2) {
         float j1 = x;
         x = x2;
         x2 = j1;
      }

      if (y < y2) {
         float j1 = y;
         y = y2;
         y2 = j1;
      }

      float f1 = (color >> 16 & 0xFF) / 255.0F;
      float f2 = (color >> 8 & 0xFF) / 255.0F;
      float f3 = (color & 0xFF) / 255.0F;
      this.draw(ivertex, matrix, lightmapUV, x, y, z, f1, f2, f3);
      this.draw(ivertex, matrix, lightmapUV, x, y2, z, f1, f2, f3);
      this.draw(ivertex, matrix, lightmapUV, x2, y2, z, f1, f2, f3);
      this.draw(ivertex, matrix, lightmapUV, x2, y, z, f1, f2, f3);
   }

   private void draw(VertexConsumer ivertex, Matrix4f matrix, int lightmapUV, float x, float y, float z, float red, float green, float blue) {
      Vector4f v = matrix.transform(new Vector4f(x, y, z, 1.0F));
      ivertex.m_5483_(v.x(), v.y(), v.z()).m_85950_(red, green, blue, 1.0F).m_85969_(lightmapUV).m_5752_();
   }

   public static ChatMessages getChatMessages(String username) {
      if (users.containsKey(username)) {
         return users.get(username);
      } else {
         ChatMessages chat = new ChatMessages();
         users.put(username, chat);
         return chat;
      }
   }

   public static void parseMessage(String toParse) {
      toParse = toParse.replaceAll("§.", "");

      for (Pattern pattern : patterns) {
         Matcher m = pattern.matcher(toParse);
         if (m.find()) {
            String username = m.group(1);
            if (validPlayer(username)) {
               String message = m.group(2);
               getChatMessages(username).addMessage(Component.m_237115_(message));
               return;
            }
         }
      }
   }

   public static void test() {
      test("<Sirnoppes01> :)", "Sirnoppes01: :)");
      test("<Sirnoppes01> hey", "Sirnoppes01: hey");
      test("<Sir_noppes> hey", "Sir_noppes: hey");
      test("<Sirnoppes>: hey", "Sirnoppes: hey");
      test("[member]Sirnoppes: hey", "Sirnoppes: hey");
      test("[member]Sirnoppes01: hey", "Sirnoppes01: hey");
      test("[member]Sir_noppes: hey", "Sir_noppes: hey");
      test("[member] Sirnoppes: hey", "Sirnoppes: hey");
      test("[g][member]Sirnoppes: hey", "Sirnoppes: hey");
      test("[g] [member]Sirnoppes: hey", "Sirnoppes: hey");
      test("[g] [member]-Sirnoppes: hey", "Sirnoppes: hey");
      test("[Player755: Teleported Player755 to Player885]", "");
      test("member Sirnoppes: hey", "Sirnoppes: hey");
      test("member-Sirnoppes: hey", "Sirnoppes: hey");
      test("member: Sirnoppes: hey", "");
   }

   private static void test(String toParse, String result) {
      for (Pattern pattern : patterns) {
         Matcher m = pattern.matcher(toParse);
         if (m.find()) {
            String username = m.group(1);
            String message = m.group(2);
            if (message != null && username != null) {
               if (result.isEmpty()) {
                  System.err.println("failed: " + toParse + " - " + username + ": " + message);
                  return;
               }

               if ((username + ": " + message).equals(result)) {
                  System.out.println("success: " + toParse);
                  return;
               }
            }
         }
      }

      if (result.isEmpty()) {
         System.out.println("success: " + toParse);
      } else {
         System.err.println("failed: " + toParse);
      }
   }

   private static boolean validPlayer(String username) {
      for (Player player : Minecraft.m_91087_().f_91073_.m_6907_()) {
         if (username.equals(player.m_7755_()) || username.equals(player.m_5446_().getString())) {
            return true;
         }
      }

      return false;
   }

   private Map<Long, TextBlockClient> getMessages() {
      Map<Long, TextBlockClient> messages = new TreeMap<>();
      long time = System.currentTimeMillis();

      for (Entry<Long, TextBlockClient> entry : this.messages.entrySet()) {
         if (time <= entry.getKey() + 10000L) {
            messages.put(entry.getKey(), entry.getValue());
         }
      }

      return this.messages = messages;
   }

   public boolean hasMessage() {
      return !this.messages.isEmpty();
   }
}
