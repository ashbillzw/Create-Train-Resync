package com.ashbill.trainresync.compat.jei;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraftforge.common.MinecraftForge;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.library.load.registration.IngredientManagerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class EnglishSearchPlugin implements IModPlugin {
    private SearchPrefixHelp help;

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        help = new SearchPrefixHelp(runtime.getIngredientListOverlay());
        MinecraftForge.EVENT_BUS.register(help);
    }

    @Override
    public void onRuntimeUnavailable() {
        if (help != null) MinecraftForge.EVENT_BUS.unregister(help);
        help = null;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("trainresync", "english_search");
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        // JEI 15 passes its populated builder here, before it publishes the ingredient manager.
        if (!(registration instanceof IngredientManagerBuilder builder)) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getLanguageManager().getSelected().equals("en_us")) return;

        Language english = ClientLanguage.loadFrom(client.getResourceManager(), List.of("en_us"), false);
        for (var stack : builder.build().getAllItemStacks()) {
            Component name = stack.getHoverName();
            String alias = ChatFormatting.stripFormatting(inEnglish(name, english).getString()).strip();
            if (!alias.isEmpty() && !alias.equalsIgnoreCase(name.getString())) {
                registration.addAlias(stack, alias);
            }
        }
    }

    private static MutableComponent inEnglish(Component name, Language english) {
        MutableComponent result;
        if (name.getContents() instanceof TranslatableContents text) {
            String pattern = english.getOrDefault(text.getKey(), text.getFallback() == null ? "" : text.getFallback());
            Object[] args = text.getArgs().clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component argument) args[i] = inEnglish(argument, english);
            }
            // Let vanilla expand positional arguments and escaped percent signs in the English template.
            result = args.length == 0 ? Component.literal(pattern) : Component.translatableWithFallback("", pattern, args);
        } else {
            result = MutableComponent.create(name.getContents());
        }
        for (Component sibling : name.getSiblings()) result.append(inEnglish(sibling, english));
        return result;
    }
}
