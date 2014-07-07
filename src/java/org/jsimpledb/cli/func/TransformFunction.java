
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb.cli.func;

import com.google.common.collect.Iterables;

import java.util.Map;

import org.jsimpledb.cli.Session;
import org.jsimpledb.cli.parse.expr.EvalException;
import org.jsimpledb.cli.parse.expr.Value;

@CliFunction
public class TransformFunction extends ApplyExprFunction {

    public TransformFunction() {
        super("transform");
    }

    @Override
    public String getHelpSummary() {
        return "transforms a collection";
    }

    @Override
    public String getUsage() {
        return "transform(items, variable, expression)";
    }

    @Override
    public String getHelpDetail() {
        return "Creates a view of a collection where each item is transformed by assigning the item to the specified variable"
          + " and evaluating the specified expression.";
    }

    @Override
    protected Value apply(Session session, final ParamInfo params) {
        return new Value(null) {
            @Override
            public Object get(final Session session) {

                // Evaluate items
                Object items = params.getItems().evaluate(session).checkNotNull(session, "transform()");
                if (items instanceof Map)
                    items = ((Map<?, ?>)items).entrySet();
                if (!(items instanceof Iterable))
                    throw new EvalException("invalid transform() operation on object of type " + items.getClass().getName());

                // Return tranformed view
                return Iterables.transform((Iterable<?>)items, new com.google.common.base.Function<Object, Object>() {
                    @Override
                    public Object apply(Object item) {
                        return TransformFunction.this.evaluate(session, params.getVariable(), item, params.getExpr()).get(session);
                    }
                });
            }
        };
    }
}
