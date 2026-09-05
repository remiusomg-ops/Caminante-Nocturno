package com.renzo.caminantenocturno.recipe;

import com.renzo.caminantenocturno.CaminanteNocturnoMod;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class FrascoExplosivoRecipe extends CustomRecipe {
    public FrascoExplosivoRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        if (!inv.getItem(4).is(Items.GLASS_BOTTLE)) return false;
        if (!inv.getItem(1).is(Items.IRON_NUGGET)) return false;
        if (!inv.getItem(3).is(Items.IRON_NUGGET)) return false;
        if (!inv.getItem(5).is(Items.IRON_NUGGET)) return false;
        if (!inv.getItem(7).is(Items.IRON_NUGGET)) return false;

        return inv.getItem(0).isEmpty()
            && inv.getItem(2).isEmpty()
            && inv.getItem(6).isEmpty()
            && inv.getItem(8).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        return new ItemStack(CaminanteNocturnoMod.FRASCO_EXPLOSIVO.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CaminanteNocturnoMod.FRASCO_EXPLOSIVO_RECIPE.get();
    }
}
