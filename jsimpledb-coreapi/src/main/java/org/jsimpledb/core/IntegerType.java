
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.core;

import org.dellroad.stuff.java.Primitive;

/**
 * {@code int} primitive type.
 */
class IntegerType extends IntegralType<Integer> {

    private static final long serialVersionUID = 1978611631822982974L;

    IntegerType() {
       super(Primitive.INTEGER);
    }

    @Override
    protected Integer downCast(long value) {
        return (int)value;
    }

    @Override
    public Integer validate(Object obj) {
        if (obj instanceof Character)
            return (int)((Character)obj).charValue();
        if (obj instanceof Byte || obj instanceof Short)
            return ((Number)obj).intValue();
        return super.validate(obj);
    }
}

