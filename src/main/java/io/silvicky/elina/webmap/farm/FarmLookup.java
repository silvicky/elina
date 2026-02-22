package io.silvicky.elina.webmap.farm;

import com.mojang.datafixers.util.Either;
import io.silvicky.elina.command.Farm;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;

public class FarmLookup
{
    private final Map<Identifier,Double> itemCurrentCost=new HashMap<>();
    private final Map<Identifier, Either<Farm.FindResult,Set<Identifier>>> itemCurrentSolution=new HashMap<>();
    private final PriorityQueue<Pair<Double,Identifier>> pq=new PriorityQueue<>(Comparator.comparingDouble(Pair::getLeft));
    private static class RecipeStatus
    {
        private double cost=0;
        private final Set<Identifier> source=new HashSet<>();
        private final Set<Identifier> resolvedSources=new HashSet<>();
        private final Identifier target;
        public RecipeStatus(Set<Identifier> source,Identifier target)
        {
            this.source.addAll(source);
            this.target=target;
        }
        public double getCost(){return cost;}
        public boolean isDone(){return source.isEmpty();}
        public boolean addItem(Identifier id,double itemCost)
        {
            if(!source.contains(id))return false;
            source.remove(id);
            resolvedSources.add(id);
            cost+=itemCost;
            return true;
        }
        public Identifier getTarget(){return target;}
        public Set<Identifier> getResolvedSources(){return resolvedSources;}
    }
    private final Map<Identifier, RecipeStatus> recipeStatus=new HashMap<>();
    private final Map<Identifier, Set<Identifier>> recipesWithSource=new HashMap<>();
    private void build(List<Farm.FindResult> farms)
    {
        for(Farm.FindResult findResult:farms)
        {
            for(Identifier i:findResult.info().items())
            {
                if((!itemCurrentCost.containsKey(i))||itemCurrentCost.get(i)> findResult.distance())
                {
                    itemCurrentCost.put(i, findResult.distance());
                    itemCurrentSolution.put(i,Either.left(findResult));
                }
            }
        }
        for(Map.Entry<Identifier, Double> i:itemCurrentCost.entrySet())
        {
            pq.add(new Pair<>(i.getValue(),i.getKey()));
        }
        while(!pq.isEmpty())
        {
            Pair<Double, Identifier> i=pq.poll();
            if(itemCurrentCost.get(i.getRight())<i.getLeft())continue;
            for(Identifier recipe:recipesWithSource.getOrDefault(i.getRight(),new HashSet<>()))
            {
                RecipeStatus status=recipeStatus.get(recipe);
                if(status.addItem(i.getRight(),i.getLeft())&&status.isDone())
                {
                    double cost=status.getCost();
                    Identifier target=status.getTarget();
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
    public static void lookup(String id,Identifier target)
    {
        FarmLookup instance= instances.get(id);
        if(instance==null)
        {
            //TODO
        }
    }
}
