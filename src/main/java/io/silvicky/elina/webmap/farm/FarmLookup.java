package io.silvicky.elina.webmap.farm;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import io.silvicky.elina.command.Farm;
import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;

import static io.silvicky.elina.common.RecipeManager.recipes;
import static io.silvicky.elina.common.RecipeManager.recipesWithSource;

public class FarmLookup
{
    private final Map<RegistryEntry<Item>,Double> itemCurrentCost=new HashMap<>();
    private final Map<RegistryEntry<Item>, Either<Farm.FindResult,Set<RegistryEntry<Item>>>> itemCurrentSolution=new HashMap<>();
    private final PriorityQueue<Pair<Double,RegistryEntry<Item>>> pq=new PriorityQueue<>(Comparator.comparingDouble(Pair::getLeft));
    public static final SimpleCommandExceptionType FARM_LOOKUP_NOT_FOUND=new SimpleCommandExceptionType(Text.literal("Please build first!"));
    private static class RecipeStatus
    {
        private double cost=0;
        private final Set<Ingredient> source=new HashSet<>();
        private final Set<RegistryEntry<Item>> resolvedSources=new HashSet<>();
        private final RegistryEntry<Item> target;
        public RecipeStatus(List<Ingredient> source, RegistryEntry<Item> target)
        {
            this.source.addAll(source);
            this.target=target;
        }
        public double getCost(){return cost;}
        public boolean isDone(){return source.isEmpty();}
        public boolean addItem(RegistryEntry<Item> id,double itemCost)
        {
            boolean hit=false;
            Iterator<Ingredient> it= source.iterator();
            while(it.hasNext())
            {
                Ingredient ingredient=it.next();
                if(ingredient.acceptsItem(id))
                {
                    hit=true;
                    it.remove();
                }
            }
            if(!hit)return false;
            resolvedSources.add(id);
            cost+=itemCost;
            return true;
        }
        public RegistryEntry<Item> getTarget(){return target;}
        public Set<RegistryEntry<Item>> getResolvedSources(){return resolvedSources;}
    }
    private final Map<Identifier, RecipeStatus> recipeStatus=new HashMap<>();
    private void build(List<Farm.FindResult> farms)
    {
        for(Map.Entry<Identifier, Pair<List<Ingredient>, RegistryEntry<Item>>> i:recipes.entrySet())
        {
            recipeStatus.put(i.getKey(),new RecipeStatus(i.getValue().getLeft(),i.getValue().getRight()));
        }
        for(Farm.FindResult findResult:farms)
        {
            for(Identifier id:findResult.info().items())
            {
                RegistryEntry<Item> i= Registries.ITEM.getOrThrow(RegistryKey.of(RegistryKeys.ITEM,id));
                if((!itemCurrentCost.containsKey(i))||itemCurrentCost.get(i)> findResult.distance())
                {
                    itemCurrentCost.put(i, findResult.distance());
                    itemCurrentSolution.put(i,Either.left(findResult));
                }
            }
        }
        for(Map.Entry<RegistryEntry<Item>, Double> i:itemCurrentCost.entrySet())
        {
            pq.add(new Pair<>(i.getValue(),i.getKey()));
        }
        while(!pq.isEmpty())
        {
            Pair<Double, RegistryEntry<Item>> i=pq.poll();
            if(itemCurrentCost.get(i.getRight())<i.getLeft())continue;
            for(Identifier recipe:recipesWithSource.getOrDefault(i.getRight(),new HashSet<>()))
            {
                RecipeStatus status=recipeStatus.get(recipe);
                if(status.addItem(i.getRight(),i.getLeft())&&status.isDone())
                {
                    double cost=status.getCost();
                    RegistryEntry<Item> target=status.getTarget();
                    if((!itemCurrentCost.containsKey(target))||itemCurrentCost.get(target)> cost)
                    {
                        itemCurrentCost.put(target, cost);
                        itemCurrentSolution.put(target,Either.right(status.getResolvedSources()));
                        pq.add(new Pair<>(cost,target));
                    }
                }
            }
        }
    }
    private static final HashMap<String,FarmLookup> instances=new HashMap<>();
    public static void build(String id,List<Farm.FindResult> farms)
    {
        FarmLookup instance=new FarmLookup();
        instance.build(farms);
        instances.put(id,instance);
    }
    public static void lookup(String id, RegistryEntry<Item> target) throws CommandSyntaxException
    {
        FarmLookup instance= instances.get(id);
        if(instance==null)
        {
            throw FARM_LOOKUP_NOT_FOUND.create();
        }
        //TODO
    }
}
