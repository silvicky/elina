package io.silvicky.elina.common;

import net.minecraft.item.Item;
import net.minecraft.recipe.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;

public class RecipeManager
{
    public static final Map<Identifier, Pair<List<Ingredient>, RegistryEntry<Item>>> recipes=new HashMap<>();
    public static final Map<RegistryEntry<Item>, Set<Identifier>> recipesWithSource=new HashMap<>();
    private static void resolveRecipe(Identifier id,Recipe<?> recipe)
    {
        switch (recipe)
        {
            case ShapedRecipe shapedRecipe -> recipes.put(id,new Pair<>(shapedRecipe.getIngredientPlacement().getIngredients(), shapedRecipe.result.getRegistryEntry()));
            case ShapelessRecipe shapelessRecipe -> recipes.put(id,new Pair<>(shapelessRecipe.getIngredientPlacement().getIngredients(), shapelessRecipe.result.getRegistryEntry()));
            case SingleStackRecipe singleStackRecipe -> recipes.put(id,new Pair<>(List.of(singleStackRecipe.ingredient()),singleStackRecipe.result.getRegistryEntry()));
            default -> {}
        }
        //TODO more types
    }
    public static void load(MinecraftServer server)
    {
        for(RecipeEntry<?> recipeEntry:server.getRecipeManager().values())
        {
            resolveRecipe(recipeEntry.id().getValue(),recipeEntry.value());
        }
        for(Map.Entry<Identifier, Pair<List<Ingredient>, RegistryEntry<Item>>> i:recipes.entrySet())
        {
            for(Ingredient ingredient:i.getValue().getLeft())
            {
                for(RegistryEntry<Item> item:ingredient.entries)
                {
                    recipesWithSource.computeIfAbsent(item,v->new HashSet<>()).add(i.getKey());
                }
            }
        }
    }
}
