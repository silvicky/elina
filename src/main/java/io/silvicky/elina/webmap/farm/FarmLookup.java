package io.silvicky.elina.webmap.farm;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import io.silvicky.elina.Elina;
import io.silvicky.elina.command.Farm;
import io.silvicky.elina.common.RecipeManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Tuple;

import java.util.*;

import static io.silvicky.elina.command.Farm.FARM_NOT_FOUND;
import static io.silvicky.elina.common.RecipeManager.recipes;
import static io.silvicky.elina.common.RecipeManager.recipesWithSource;
import static io.silvicky.elina.common.Util.collectionToString;
import static io.silvicky.elina.common.Util.getPlayerUuid;
import static java.lang.String.format;

public class FarmLookup
{
    private final Map<Holder<Item>,Double> itemCurrentCost=new HashMap<>();
    private final Map<Holder<Item>, Either<Farm.FindResult,Set<Holder<Item>>>> itemCurrentSolution=new HashMap<>();
    private final PriorityQueue<Tuple<Double, Holder<Item>>> pq=new PriorityQueue<>(Comparator.comparingDouble(Tuple::getA));
    public static final SimpleCommandExceptionType FARM_LOOKUP_NOT_FOUND=new SimpleCommandExceptionType(Component.literal("Please build first!"));
    private static class RecipeStatus
    {
        private double cost=0;
        private final Set<Ingredient> source=new HashSet<>();
        private final Set<Holder<Item>> resolvedSources=new HashSet<>();
        private final Holder<Item> target;
        public RecipeStatus(List<Ingredient> source, Holder<Item> target)
        {
            this.source.addAll(source);
            this.target=target;
        }
        public double getCost(){return cost;}
        public boolean isDone(){return source.isEmpty();}
        public boolean addItem(Holder<Item> id, double itemCost)
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
        public Holder<Item> getTarget(){return target;}
        public Set<Holder<Item>> getResolvedSources(){return resolvedSources;}
    }
    private final Map<Identifier, RecipeStatus> recipeStatus=new HashMap<>();
    private void build(List<Farm.FindResult> farms)
    {
        for(Map.Entry<Identifier, Tuple<List<Ingredient>, Holder<Item>>> i:recipes.entrySet())
        {
            recipeStatus.put(i.getKey(),new RecipeStatus(i.getValue().getA(),i.getValue().getB()));
        }
        for(Farm.FindResult findResult:farms)
        {
            for(Identifier id:findResult.info().items())
            {
                Holder<Item> i= BuiltInRegistries.ITEM.getOrThrow(ResourceKey.create(Registries.ITEM,id));
                if((!itemCurrentCost.containsKey(i))||itemCurrentCost.get(i)> findResult.distance())
                {
                    itemCurrentCost.put(i, findResult.distance());
                    itemCurrentSolution.put(i,Either.left(findResult));
                }
            }
        }
        for(Map.Entry<Holder<Item>, Double> i:itemCurrentCost.entrySet())
        {
            pq.add(new Tuple<>(i.getValue(),i.getKey()));
        }
        while(!pq.isEmpty())
        {
            Tuple<Double, Holder<Item>> i=pq.poll();
            if(itemCurrentCost.get(i.getB())<i.getA())continue;
            for(Identifier recipe:recipesWithSource.getOrDefault(i.getB(),new HashSet<>()))
            {
                RecipeStatus status=recipeStatus.get(recipe);
                if(status.addItem(i.getB(),i.getA())&&status.isDone())
                {
                    double cost=status.getCost();
                    Holder<Item> target=status.getTarget();
                    if((!itemCurrentCost.containsKey(target))||itemCurrentCost.get(target)> cost)
                    {
                        itemCurrentCost.put(target, cost);
                        itemCurrentSolution.put(target,Either.right(status.getResolvedSources()));
                        pq.add(new Tuple<>(cost,target));
                    }
                }
            }
        }
    }
    private static final HashMap<String,FarmLookup> instances=new HashMap<>();
    public static void build(String id,List<Farm.FindResult> farms)
    {
        RecipeManager.load(Elina.server);
        FarmLookup instance=new FarmLookup();
        instance.build(farms);
        instances.put(id,instance);
    }
    private void outputSolution(CommandSourceStack source, Holder<Item> target, Either<Farm.FindResult,Set<Holder<Item>>> solution)
    {
        solution.left().ifPresent(findResult -> source.sendSuccess(() -> Component.literal(format("%s -> %s", target.getRegisteredName(), findResult)), false));
        solution.right().ifPresent(registryEntries -> source.sendSuccess(() -> Component.literal(format("%s -> {%s}", target.getRegisteredName(), collectionToString(registryEntries.stream().map(Holder::getRegisteredName).toList()))), false));
    }
    private void lookupInternal(CommandSourceStack source, Holder<Item> target) throws CommandSyntaxException
    {
        if(!itemCurrentSolution.containsKey(target))throw FARM_NOT_FOUND.create();
        Set<Holder<Item>> gone=new HashSet<>();
        Queue<Holder<Item>> q=new ArrayDeque<>();
        q.add(target);
        gone.add(target);
        while(!q.isEmpty())
        {
            Holder<Item> cur=q.poll();
            Either<Farm.FindResult,Set<Holder<Item>>> solution=itemCurrentSolution.get(cur);
            outputSolution(source,cur,solution);
            if(solution.right().isPresent())
            {
                for(Holder<Item> j:solution.right().get())
                {
                    if(!gone.contains(j))
                    {
                        gone.add(j);
                        q.add(j);
                    }
                }
            }
        }
    }
    public static void lookup(CommandSourceStack source, Holder<Item> target) throws CommandSyntaxException
    {
        FarmLookup instance= instances.get(getPlayerUuid(source.getPlayer()));
        if(instance==null)
        {
            throw FARM_LOOKUP_NOT_FOUND.create();
        }
        instance.lookupInternal(source,target);
    }
}
