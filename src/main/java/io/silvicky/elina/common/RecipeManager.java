package io.silvicky.elina.common;

import net.minecraft.world.item.Item;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;

import java.util.*;

public class RecipeManager
{
    public static final Map<Identifier, Tuple<List<Ingredient>, Holder<Item>>> recipes=new HashMap<>();
    public static final Map<Holder<Item>, Set<Identifier>> recipesWithSource=new HashMap<>();
    private static boolean isReady=false;
    private static void resolveRecipe(Identifier id, Recipe<?> recipe)
    {
        switch (recipe)
        {
            case ShapedRecipe shapedRecipe -> recipes.put(id,new Tuple<>(shapedRecipe.placementInfo().ingredients(), shapedRecipe.result.getItemHolder()));
            case ShapelessRecipe shapelessRecipe -> recipes.put(id,new Tuple<>(shapelessRecipe.placementInfo().ingredients(), shapelessRecipe.result.getItemHolder()));
            case SingleItemRecipe singleStackRecipe -> recipes.put(id,new Tuple<>(List.of(singleStackRecipe.input()),singleStackRecipe.result.getItemHolder()));
            default -> {}
        }
        //TODO more types
    }
    public static void load(MinecraftServer server)
    {
        if(isReady)return;
        for(RecipeHolder<?> recipeEntry:server.getRecipeManager().getRecipes())
        {
            resolveRecipe(recipeEntry.id().identifier(),recipeEntry.value());
        }
        for(Map.Entry<Identifier, Tuple<List<Ingredient>, Holder<Item>>> i:recipes.entrySet())
        {
            for(Ingredient ingredient:i.getValue().getA())
            {
                for(Holder<Item> item:ingredient.values)
                {
                    recipesWithSource.computeIfAbsent(item,v->new HashSet<>()).add(i.getKey());
                }
            }
        }
        isReady=true;
    }
}
