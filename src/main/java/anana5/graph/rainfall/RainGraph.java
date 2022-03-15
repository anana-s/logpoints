package anana5.graph.rainfall;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import anana5.graph.Edge;
import anana5.graph.Graph;
import anana5.graph.Vertex;

public class RainGraph<T> implements Graph<T, Vertex<T>, Edge<T, Vertex<T>>> {
    private Rain<T> rain;

    @Override
    public void traverse(Consumer<? super Vertex<T>> consumer) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void traverse(BiConsumer<? super Vertex<T>, ? super Vertex<T>> consumer) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <R> R fold(R initial, BiFunction<Collection<? super R>, ? super Vertex<T>, ? super Vertex<T>> consumer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Vertex<T>> vertices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Edge<T, Vertex<T>>> edges() {
        // TODO Auto-generated method stub
        return null;
    }
}
