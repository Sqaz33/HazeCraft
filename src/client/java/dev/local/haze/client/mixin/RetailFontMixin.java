package dev.local.haze.client.mixin;

import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** FreeType supports retail CFF outlines; allow them only for our private font resources. */
@Mixin(TrueTypeGlyphProviderDefinition.class)
public abstract class RetailFontMixin {
    @Redirect(method="load",at=@At(value="INVOKE",target="Lorg/lwjgl/util/freetype/FreeType;FT_Get_Font_Format(Lorg/lwjgl/util/freetype/FT_Face;)Ljava/lang/String;"))
    private String haze$cff(FT_Face face){
        String format=FreeType.FT_Get_Font_Format(face);
        TrueTypeGlyphProviderDefinition provider=(TrueTypeGlyphProviderDefinition)(Object)this;
        return provider.location().getNamespace().equals("haze")&&"CFF".equals(format)?"TrueType":format;
    }
}
