package anana5.sense.logpoints;

import java.io.Serializable;

@FunctionalInterface
interface GraphRequest<T extends Serializable> extends Request<SerialRefGraph, T> {}
