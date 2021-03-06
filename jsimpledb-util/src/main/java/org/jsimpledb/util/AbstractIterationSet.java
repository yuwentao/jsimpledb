
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.util;

import com.google.common.collect.Iterators;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Support superclass for {@link Set} implementations for which calculating {@link #size} requires
 * an iteration through all of the set's elements to count them.
 *
 * <p>
 * Superclass methods in {@link AbstractSet} that rely on {@link #size} are overridden with alternate
 * implementations that avoid the use of {@link #size} when possible.
 *
 * <p>
 * For a read-only implementation, subclasses should implement {@link #contains contains()} and {@link #iterator iterator()}.
 *
 * <p>
 * For a mutable implementation, subclasses should also implement {@link #add add()}, {@link #remove remove()},
 * and {@link #clear clear()}, and make the {@link #iterator iterator()} mutable.
 */
public abstract class AbstractIterationSet<E> extends AbstractSet<E> {

    protected AbstractIterationSet() {
    }

    /**
     * Calculate size.
     *
     * <p>
     * The implementation in {@link AbstractIterationSet} iterates through all of the elements.
     */
    @Override
    public int size() {
        return Iterators.size(this.iterator());
    }

    /**
     * Overridden in {@link AbstractIterationSet} to minimize the use of {@link #size}.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Set))
            return false;
        final Set<?> that = (Set<?>)obj;
        final Iterator<?> i1 = this.iterator();
        final Iterator<?> i2 = that.iterator();
        while (true) {
            final boolean hasNext1 = i1.hasNext();
            final boolean hasNext2 = i2.hasNext();
            if (!hasNext1 && !hasNext2)
                return true;
            if (!hasNext1 || !hasNext2)
                return false;
            if (!this.contains(i2.next()))
                return false;
            i1.next();
        }
    }

    // This is here to silence a checkstyle warning
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Overridden in {@link AbstractIterationSet} to minimize the use of {@link #size}.
     */
    @Override
    public boolean isEmpty() {
        return !this.iterator().hasNext();
    }

    /**
     * Overridden in {@link AbstractIterationSet} to minimize the use of {@link #size}.
     */
    @Override
    public Object[] toArray() {
        final ArrayList<E> list = new ArrayList<>();
        for (E elem : this)
            list.add(elem);
       return list.toArray();
    }

    /**
     * Overridden in {@link AbstractIterationSet} to minimize the use of {@link #size}.
     */
    @Override
    public <T> T[] toArray(T[] array) {
        final ArrayList<E> list = new ArrayList<>();
        for (E elem : this)
            list.add(elem);
       return list.toArray(array);
    }
}

