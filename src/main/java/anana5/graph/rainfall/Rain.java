package anana5.graph.rainfall;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.util.LList;
import anana5.util.Promise;

/**
 * A lazy graph implementation using promises.
 */
public class Rain<T> implements Graph<T> {    
    
    final LList<Droplet<T, Rain<T>>> droplets;

    public Rain() {
        this.droplets = new LList<>();
    }

    public Rain(Rain<T> other) {
        this.droplets = other.droplets;
    }
    
    public Rain(Iterable<Droplet<T, Rain<T>>> droplets) {
        this(new LList<>(droplets));
    }

    public <S> Rain(S s, Function<S, LList<Droplet<T, S>>> func) {
        this(func.apply(s).map(droplet -> droplet.fmap(let -> new Rain<>(let, func))));
    }
    
    public Rain(LList<Droplet<T, Rain<T>>> droplets) {
        this.droplets = droplets;
    }

    
    /** 
     * @return Promise<Collection<Droplet<T, Rain<T>>>>
     */
    public Promise<Collection<Droplet<T, Rain<T>>>> collect() {
        return droplets.collect();
    }

    
    /** 
     * @param func
     * @return R
     */
    public <R> R fold(Function<LList<Droplet<T, R>>, R> func) {
        return func.apply(droplets.map(droplet -> droplet.fmap(let -> let.fold(func))));
    }

    
    /** 
     * @param func
     * @return Rain<T>
     */
    public static <T, S> Rain<T> unfold(S s, Function<S, LList<Droplet<T, S>>> func) {
        return new Rain<>(s, func);
    }

    
    /** 
     * @param func
     * @return Rain<R>
     */
    public <R> Rain<R> map(Function<T, R> func) {
        return unfold(this, rainfall -> rainfall.droplets.map(droplet -> droplet.map(func)));
    }

    @Override
    public void traverse(Consumer<Vertex<T>> consumer) {
        Promise<Void> task = traverse((drop) -> {
            consumer.accept(drop);
            return Promise.nil();
        });

        task.run();
    }

    
    /** 
     * @param visitor
     * @return Promise<Void>
     */
    public Promise<Void> traverse(Function<Drop<T>, Promise<Void>> visitor) {
        return traverse(null, visitor);
    }

    
    /** 
     * @param visitor
     * @return Promise<Void>
     */
    private Promise<Void> traverse(Drop<T> parent, Function<Drop<T>, Promise<Void>> visitor) {
        return droplets.traverse((droplet) -> {
            Drop<T> drop = droplet.freeze(parent);
            return visitor.apply(drop).bind((void$) -> {
                return droplet.let.traverse(drop, visitor);
            });
        });
    }

    
    /** 
     * @param rain
     * @return Rain<T>
     */
    @SafeVarargs
    public static <T> Rain<T> merge(Rain<T>... rain) {
        @SuppressWarnings("unchecked")
        LList<Droplet<T, Rain<T>>>[] droplets = new LList[rain.length];
        for (int i = 0; i < rain.length; i++) {
            droplets[i] = rain[i].droplets;
        }
        return new Rain<>(LList.merge(droplets));
    }
    
}
