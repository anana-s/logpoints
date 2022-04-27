package anana5.sense.logpoints;

import java.io.Serializable;

@FunctionalInterface
interface LogPointsRequest<T extends Serializable> extends Request<LogPoints, T> {}
