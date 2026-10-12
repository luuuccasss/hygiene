package fr.luuuccasss.hygiene;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /hygiene                       ta propre saleté
 * /hygiene <joueur>              celle d'un autre joueur (op)
 * /hygiene set <joueur> <0-100>  la régler (op)
 */
public final class HygieneCommand {
    private HygieneCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("hygiene")
            .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
            .then(argument("joueur", EntityArgumentType.player())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> show(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "joueur"))))
            .then(literal("set")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("joueur", EntityArgumentType.player())
                    .then(argument("valeur", FloatArgumentType.floatArg(0, 100))
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "joueur");
                            ((Dirty) target).hygiene$setDirt(FloatArgumentType.getFloat(ctx, "valeur"));
                            return show(ctx.getSource(), target);
                        })))));
    }

    private static int show(ServerCommandSource source, ServerPlayerEntity player) {
        Dirty dirty = (Dirty) player;
        float dirt = dirty.hygiene$getDirt();
        Stage stage = dirty.hygiene$getStage();
        int filled = Math.round(dirt / 5f);
        MutableText bar = Text.literal("█".repeat(filled)).formatted(stage.color)
            .append(Text.literal("█".repeat(20 - filled)).formatted(Formatting.DARK_GRAY));
        source.sendFeedback(() -> Text.literal("Saleté de ").formatted(Formatting.GRAY)
            .append(player.getDisplayName())
            .append(Text.literal(" : ").formatted(Formatting.GRAY))
            .append(bar)
            .append(Text.literal(String.format(" %d %% · %s", Math.round(dirt), stage.label)).formatted(stage.color)), false);
        return Math.round(dirt);
    }
}
