package anana5.util;

public class LSet<T> {
    private static class Node<T> {
        private final T value;
        private final LSet<T> left;
        private final LSet<T> right;
        private final int size;
        private Node(T value, int size, LSet<T> left, LSet<T> right) {
            this.value = value;
            this.left = left;
            this.right = right;
            this.size = size;
        }
        private static <T> Node<T> of(T value, int size, LSet<T> left, LSet<T> right) {
            return new Node<T>(value, size, left, right);
        }
    }
    private final Promise<Maybe<Node<T>>> unfix;
    private LSet(Promise<Maybe<Node<T>>> promise) {
        unfix = promise;
    }
    public static <T> LSet<T> fix(Promise<Maybe<Node<T>>> promise) {
        return new LSet<>(promise);
    }
    public static <T> LSet<T> bind(Promise<LSet<T>> promise) {
        return LSet.fix(promise.then(set -> set.unfix));
    }
    public static <T> LSet<T> of(Node<T> node) {
        return LSet.fix(Promise.just(Maybe.just(node)));
    }
    public static <T> LSet<T> of() {
        return LSet.fix(Promise.just(Maybe.nothing()));
    }

    private LSet<T> rrotate() {
        return LSet.fix(unfix.then(mt -> {
            if (!mt.check()) {
                return Promise.just(mt);
            }
            Node<T> t = mt.get();
            return t.left.unfix.then(mtl -> {
                if (!mtl.check()) {
                    return Promise.just(mt);
                }
                Node<T> tl = mtl.get();
                return tl.right.unfix.then(mtlr -> {
                    if (!mtlr.check()) {
                        return Promise.just(mt);
                    }
                    Node<T> tlr = mtlr.get();
                    return Promise.just(Maybe.just(
                        Node.of(tl.value, t.size,
                            tl.left,
                            LSet.<T>of(Node.of(t.value, t.size - tl.size + tlr.size,
                                tl.right,
                                t.right
                            ))
                        )
                    ));
                });
            });
        }));
    }

    private LSet<T> lrotate() {
        return LSet.fix(unfix.then(mt -> {
            if (!mt.check()) {
                return Promise.just(mt);
            }
            Node<T> t = mt.get();
            return t.right.unfix.then(mtr -> {
                if (!mtr.check()) {
                    return Promise.just(mt);
                }
                Node<T> tr = mtr.get();
                return tr.left.unfix.then(mtrl -> {
                    if (!mtrl.check()) {
                        return Promise.just(mt);
                    }
                    Node<T> trl = mtrl.get();
                    return Promise.just(Maybe.just(
                        Node.of(tr.value, t.size,
                            LSet.<T>of(Node.of(t.value, t.size - tr.size + trl.size,
                                t.left,
                                tr.left
                            )),
                            tr.right
                        )
                    ));
                });
            });
        }));
    }

    private LSet<T> rmaintain() {
        return LSet.fix(unfix.then(mt -> {
            if (!mt.check()) {
                return Promise.just(mt);
            }
            Node<T> t = mt.get();
            return t.right.unfix.then(mtr -> {
                if (!mtr.check()) {
                    return Promise.just(mtr);
                }
                Node<T> tr = mtr.get();
                return t.right.unfix.then(mtl -> {
                    if (!mtr.check()) {
                        return Promise.just(mtl);
                    }
                    Node<T> tl = mtl.get();
                    return null;
                });
            });
        }));
    }
}
