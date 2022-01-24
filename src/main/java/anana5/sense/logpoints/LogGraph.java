package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import anana5.sense.graph.Promise;
import anana5.sense.graph.Rainfall;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class LogGraph extends Rainfall<Stmt> {
    Predicate<Droplet<Stmt, LogGraph>.SnowFlake> predicate = null;

    public LogGraph filter(Filter f) {
        this.predicate = f;
        return this;
    }

    public LogGraph build(Collection<SootMethod> entrypoints) {
        Builder builder = new Builder();
        return (LogGraph)builder.build(entrypoints);
    }

    private static class Builder implements Function<Collection<Builder.Pointer>, Rain<Stmt, Collection<Builder.Pointer>>> {
        Map<SootMethod, Context> cache = new HashMap<>();

        @Override
        public Rain<Stmt, Collection<Builder.Pointer>> apply(Collection<Builder.Pointer> pointers) {
            return new Rain<>(new Promise<>(() -> {
                Puddle<Stmt, Collection<Builder.Pointer>> puddle = new Puddle<>();
                for (Pointer pointer : pointers) {
                    puddle.addAll(pointer.target());
                }
                return puddle;
            }));
        }

        public Rainfall<Stmt> build(Collection<SootMethod> entrypoints) {
            return build(null, Collections.emptyList(), entrypoints);
        }

        public Rainfall<Stmt> build(Context context, Collection<Pointer> rets, Collection<SootMethod> methods) {
            Collection<Pointer> pointers = new ArrayList<>(methods.size());
            for (SootMethod method : methods) {
                pointers.addAll(context.push(method, rets).roots());
            }
            return Rainfall.unfold(this, pointers);
        }
    
        private class Pointer {
            Context ctx;
            Stmt stmt;
    
            Pointer(Context context, Stmt stmt) {
                this.ctx = context;
                this.stmt = stmt;
            }
    
            public Puddle<Stmt, Collection<Builder.Pointer>> target() {
                return new Puddle<>();
            }
        }
    
        private class Context {
            Context parent;
            ExceptionalUnitGraph cfg;
            Collection<Pointer> rets;
            public Context(Context parent, SootMethod method, Collection<Pointer> rets) {
                this.parent = parent;
                this.cfg = new ExceptionalUnitGraph(method.getActiveBody());
                this.rets = rets;
            }
            public Context push(SootMethod method, Collection<Pointer> rets) {
                return new Context(this, method, rets);
            }

            public Collection<Pointer> roots() {
                Collection<Pointer> pointers = new ArrayList<>();
                for (Unit unit : cfg.getHeads()) {
                    pointers.add(new Pointer(this, (Stmt)unit));
                }
                return pointers;
            }
        }

        private static boolean skip(SootMethod m) {
            return m.isPhantom() 
            || !m.isConcrete()
            || m.isJavaLibraryMethod()
            || m.getDeclaringClass().isLibraryClass();
        }
    }

    public interface Filter extends Predicate<Droplet<Stmt, LogGraph>.SnowFlake> {}
}
