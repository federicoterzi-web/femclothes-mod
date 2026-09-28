package noppes.mpm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.command.EnumArgument;
import noppes.mpm.ModelData;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.client.PacketAnimationStart;
import noppes.mpm.packets.client.PacketPlayerDataSend;
import noppes.mpm.util.NoppesStringUtils;

public class CommandMPM {
   private static List<String> entities;
   private static ArgumentType<EnumAnimation> animationArgumentType = EnumArgument.enumArgument(EnumAnimation.class);
   public static final SuggestionProvider<CommandSourceStack> ENTITIES = SuggestionProviders.m_121658_(
      new ResourceLocation("entities"), (context, builder) -> SharedSuggestionProvider.m_82981_(entities.stream(), builder)
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildAspect) {
      entities = new ArrayList<>();

      for (EntityType ent : ForgeRegistries.ENTITY_TYPES.getValues()) {
         if (ent.m_20674_() != MobCategory.MISC) {
            entities.add(ForgeRegistries.ENTITY_TYPES.getKey(ent).toString());
         }
      }

      entities.add("clear");
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.m_82127_("mpm");
      getSubCommands((LiteralArgumentBuilder<CommandSourceStack>)command.requires(source -> source.m_6761_(0)), buildAspect);
      dispatcher.register(command);
   }

   private static void getSubCommands(LiteralArgumentBuilder<CommandSourceStack> command, CommandBuildContext buildAspect) {
      command.then(
         Commands.m_82127_("url")
            .then(
               Commands.m_82129_("targets", EntityArgument.m_91470_()).then(Commands.m_82129_("url", StringArgumentType.greedyString()).executes(context -> {
                  Collection<ServerPlayer> players = getPlayers(context);
                  String url = StringArgumentType.getString(context, "url");
                  if (url.equalsIgnoreCase("clear")) {
                     url = "";
                  }

                  for (ServerPlayer player : players) {
                     ModelData data = ModelData.get(player);
                     if (!data.url.equals(url)) {
                        data.url = url;
                        Packets.sendNearby(player, new PacketPlayerDataSend(player.m_20148_(), data.writeToNBT()));
                     }
                  }

                  return players.size();
               }))
            )
      );
      command.then(
         Commands.m_82127_("entity")
            .then(
               Commands.m_82129_("targets", EntityArgument.m_91470_())
                  .then(((RequiredArgumentBuilder)Commands.m_82129_("entity", ResourceLocationArgument.m_106984_()).suggests(ENTITIES).executes(context -> {
                     Collection<ServerPlayer> players = getPlayers(context);
                     return setEntity(context, buildAspect, players, new CompoundTag());
                  })).then(Commands.m_82129_("nbt", CompoundTagArgument.m_87657_()).executes(context -> {
                     Collection<ServerPlayer> players = getPlayers(context);
                     return setEntity(context, buildAspect, players, CompoundTagArgument.m_87660_(context, "nbt"));
                  })))
            )
      );
      command.then(
         Commands.m_82127_("name")
            .then(
               Commands.m_82129_("targets", EntityArgument.m_91470_()).then(Commands.m_82129_("name", StringArgumentType.greedyString()).executes(context -> {
                  Collection<ServerPlayer> players = getPlayers(context);
                  String name = StringArgumentType.getString(context, "name");
                  if (name.equalsIgnoreCase("clear")) {
                     name = "";
                  }

                  for (ServerPlayer player : players) {
                     ModelData data = ModelData.get(player);
                     if (!data.displayName.equals(name)) {
                        data.displayName = name;
                        Packets.sendNearby(player, new PacketPlayerDataSend(player.m_20148_(), data.writeToNBT()));
                        player.refreshDisplayName();
                     }
                  }

                  return players.size();
               }))
            )
      );
      command.then(
         Commands.m_82127_("sendmodel")
            .then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.m_82129_("targets", EntityArgument.m_91470_()).executes(context -> {
               ModelData fromData = ModelData.get(((CommandSourceStack)context.getSource()).m_81375_());
               Collection<ServerPlayer> players = getPlayers(context);
               return sendModel(players, fromData);
            })).then(Commands.m_82127_("clear").executes(context -> {
               Collection<ServerPlayer> players = getPlayers(context);
               return sendModel(players, new ModelData());
            }))).then(Commands.m_82129_("from", EntityArgument.m_91466_()).executes(context -> {
               ModelData fromData = ModelData.get(EntityArgument.m_91474_(context, "from"));
               Collection<ServerPlayer> players = getPlayers(context);
               return sendModel(players, fromData);
            })))
      );
      command.then(
         Commands.m_82127_("animation")
            .then(Commands.m_82129_("targets", EntityArgument.m_91470_()).then(Commands.m_82129_("animation", animationArgumentType).executes(context -> {
               Collection<ServerPlayer> players = getPlayers(context);
               EnumAnimation animation = (EnumAnimation)context.getArgument("animation", EnumAnimation.class);

               for (ServerPlayer player : players) {
                  ModelData data = ModelData.get(player);
                  if (data.animation == animation) {
                     data.setAnimation(EnumAnimation.NONE);
                  } else {
                     data.setAnimation(animation);
                  }

                  Packets.sendNearby(player, new PacketAnimationStart(player.m_20148_(), data.animation));
               }

               return players.size();
            })))
      );
      command.then(
         Commands.m_82127_("scale")
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("targets", EntityArgument.m_91470_())
                     .then(Commands.m_82129_("all", StringArgumentType.word()).executes(context -> {
                        Collection<ServerPlayer> players = getPlayers(context);
                        CommandMPM.Scale scale = CommandMPM.Scale.Parse(StringArgumentType.getString(context, "all"));

                        for (ServerPlayer player : players) {
                           ModelData data = ModelData.get(player);
                           data.head.setScale(scale.scaleX, scale.scaleY, scale.scaleZ);
                           data.body.setScale(scale.scaleX, scale.scaleY, scale.scaleZ);
                           data.arm1.setScale(scale.scaleX, scale.scaleY, scale.scaleZ);
                           data.arm2.setScale(scale.scaleX, scale.scaleY, scale.scaleZ);
                           data.leg1.setScale(scale.scaleX, scale.scaleY, scale.scaleZ);
                           data.leg2.setScale(scale.scaleX, scale.scaleY, scale.scaleZ);
                           Packets.sendNearby(player, new PacketPlayerDataSend(player.m_20148_(), data.writeToNBT()));
                        }

                        return players.size();
                     })))
                  .then(
                     Commands.m_82129_("head", StringArgumentType.word())
                        .then(
                           Commands.m_82129_("body", StringArgumentType.word())
                              .then(
                                 Commands.m_82129_("arms", StringArgumentType.word())
                                    .then(Commands.m_82129_("legs", StringArgumentType.word()).executes(context -> {
                                       Collection<ServerPlayer> players = getPlayers(context);
                                       CommandMPM.Scale head = CommandMPM.Scale.Parse(StringArgumentType.getString(context, "head"));
                                       CommandMPM.Scale body = CommandMPM.Scale.Parse(StringArgumentType.getString(context, "body"));
                                       CommandMPM.Scale arms = CommandMPM.Scale.Parse(StringArgumentType.getString(context, "arms"));
                                       CommandMPM.Scale legs = CommandMPM.Scale.Parse(StringArgumentType.getString(context, "legs"));

                                       for (ServerPlayer player : players) {
                                          ModelData data = ModelData.get(player);
                                          data.head.setScale(head.scaleX, head.scaleY, head.scaleZ);
                                          data.body.setScale(body.scaleX, body.scaleY, body.scaleZ);
                                          data.arm1.setScale(arms.scaleX, arms.scaleY, arms.scaleZ);
                                          data.arm2.setScale(arms.scaleX, arms.scaleY, arms.scaleZ);
                                          data.leg1.setScale(legs.scaleX, legs.scaleY, legs.scaleZ);
                                          data.leg2.setScale(legs.scaleX, legs.scaleY, legs.scaleZ);
                                          Packets.sendNearby(player, new PacketPlayerDataSend(player.m_20148_(), data.writeToNBT()));
                                       }

                                       return players.size();
                                    }))
                              )
                        )
                  )
            )
      );
   }

   private static Collection<ServerPlayer> getPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      return EntityArgument.m_91477_(context, "targets");
   }

   private static int sendModel(Collection<ServerPlayer> players, ModelData fromData) {
      CompoundTag compound = fromData.writeToNBT();

      for (ServerPlayer player : players) {
         ModelData data = ModelData.get(player);
         data.readFromNBT(compound);
         data.save();
         Packets.sendNearby(player, new PacketPlayerDataSend(player.m_20148_(), data.writeToNBT()));
      }

      return players.size();
   }

   private static int setEntity(
      CommandContext<CommandSourceStack> context, CommandBuildContext buildAspect, Collection<ServerPlayer> players, CompoundTag extra
   ) throws CommandSyntaxException {
      ResourceLocation resource = (ResourceLocation)context.getArgument("entity", ResourceLocation.class);
      if (!resource.toString().equalsIgnoreCase("minecraft:clear")) {
         Reference<EntityType<?>> ref = ResourceArgument.m_247102_(buildAspect, Registries.f_256939_).parse(new StringReader(resource.toString()));
         resource = ForgeRegistries.ENTITY_TYPES.getKey((EntityType)ref.get());
      } else {
         resource = null;
      }

      for (ServerPlayer player : players) {
         ModelData data = ModelData.get(player);
         if (!NoppesStringUtils.areEqual(data.getEntityName(), resource) || !data.extra.equals(extra)) {
            data.setEntity(resource);
            data.extra = extra;
            Packets.sendNearby(player, new PacketPlayerDataSend(player.m_20148_(), data.writeToNBT()));
         }
      }

      return players.size();
   }

   static class Scale {
      float scaleX;
      float scaleY;
      float scaleZ;

      private static CommandMPM.Scale Parse(String s) throws NumberFormatException {
         CommandMPM.Scale scale = new CommandMPM.Scale();
         if (s.contains(",")) {
            String[] split = s.split(",");
            if (split.length != 3) {
               throw new NumberFormatException("Not enough args given");
            }

            scale.scaleX = Float.parseFloat(split[0]);
            scale.scaleY = Float.parseFloat(split[1]);
            scale.scaleZ = Float.parseFloat(split[2]);
         } else {
            scale.scaleZ = scale.scaleY = scale.scaleX = Float.parseFloat(s);
         }

         return scale;
      }
   }
}
