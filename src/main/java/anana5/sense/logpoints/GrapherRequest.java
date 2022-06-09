package anana5.sense.logpoints;

import java.io.Serializable;

@FunctionalInterface
interface GrapherRequest<T extends Serializable> extends Request<Grapher, T> {}
