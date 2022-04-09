package anana5.sense.logpoints;

import java.io.Serializable;

import anana5.util.Promise;

@FunctionalInterface
interface GraphRequest<T extends Serializable> extends Request<Promise<BoxGraph>, T> {}
