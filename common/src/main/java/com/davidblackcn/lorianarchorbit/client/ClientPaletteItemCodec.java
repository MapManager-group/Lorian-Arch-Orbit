package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.PaletteMatchMode;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

final class ClientPaletteItemCodec {
    private ClientPaletteItemCodec() {
    }

    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static Optional<ItemStack> resolve(Minecraft minecraft, PaletteMember member) {
        Identifier id = Identifier.tryParse(member.itemId());
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(id).flatMap(item -> {
            ItemStack stack = item.getDefaultInstance();
            if (member.matchMode() == PaletteMatchMode.EXACT_COMPONENTS) {
                Optional<DataComponentPatch> patch = decodePatch(minecraft, member.components());
                if (patch.isEmpty()) {
                    return Optional.empty();
                }
                stack.applyComponents(patch.get());
            }
            return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
        });
    }

    static boolean itemMatches(PaletteMember member, ItemStack stack) {
        return member.itemId().equals(itemId(stack));
    }

    static boolean exactMatches(Minecraft minecraft, PaletteMember member, ItemStack stack) {
        return itemMatches(member, stack)
                && decodePatch(minecraft, member.components()).map(stack.getComponentsPatch()::equals).orElse(false);
    }

    static Optional<JsonElement> encodePatch(Minecraft minecraft, ItemStack stack) {
        if (minecraft.level == null) {
            return Optional.empty();
        }
        var ops = RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());
        return DataComponentPatch.CODEC.encodeStart(ops, stack.getComponentsPatch()).result();
    }

    private static Optional<DataComponentPatch> decodePatch(Minecraft minecraft, JsonElement json) {
        if (minecraft.level == null || json == null) {
            return Optional.empty();
        }
        var ops = RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());
        return DataComponentPatch.CODEC.parse(ops, json).result();
    }
}
