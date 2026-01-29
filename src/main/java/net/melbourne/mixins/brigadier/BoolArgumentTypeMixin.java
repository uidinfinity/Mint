package net.melbourne.mixins.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoolArgumentType.class)
public class BoolArgumentTypeMixin
{
    @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Ljava/lang/Boolean;", at = @At("HEAD"), remap = false, cancellable = true)
    private void parseHook(StringReader reader, CallbackInfoReturnable<Boolean> cir) throws CommandSyntaxException
    {
        String string = reader.readString();

        if (!string.equals("0") && !string.equalsIgnoreCase("false")) {
            if (!string.equals("1") && !string.equalsIgnoreCase("true"))
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidBool().createWithContext(reader, string);

            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }
}