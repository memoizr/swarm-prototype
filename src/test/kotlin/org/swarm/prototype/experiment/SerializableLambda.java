package org.swarm.prototype.experiment;

import java.io.Serializable;

import kotlin.jvm.functions.Function1;

public interface SerializableLambda<P1, R> extends Function1<P1, R>, Serializable {
    @Override R invoke(P1 p1);
}
