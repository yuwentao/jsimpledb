
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.parse;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.regex.Matcher;

import org.jsimpledb.Session;
import org.jsimpledb.core.ObjId;
import org.jsimpledb.core.ObjType;
import org.jsimpledb.core.Schema;
import org.jsimpledb.core.Transaction;
import org.jsimpledb.util.NavigableSets;
import org.jsimpledb.util.ParseContext;

/**
 * Parses object IDs.
 */
public class ObjIdParser implements Parser<ObjId> {

    public static final int MAX_COMPLETE_OBJECTS = 100;

    @Override
    public ObjId parse(ParseSession session, ParseContext ctx, boolean complete) {

        // Get parameter
        final Matcher matcher = ctx.tryPattern("([0-9A-Fa-f]{0,16})");
        if (matcher == null)
            throw new ParseException(ctx, "invalid object ID");
        final String param = matcher.group(1);

        // Attempt to parse id
        try {
            return new ObjId(param);
        } catch (IllegalArgumentException e) {              // must be a partial ID
            if (!ctx.isEOF() || !complete)
                throw new ParseException(ctx, "invalid object ID");
        }

        // Get corresponding min & max ObjId (both inclusive)
        final char[] paramChars = param.toCharArray();
        final char[] idChars = new char[16];
        System.arraycopy(paramChars, 0, idChars, 0, paramChars.length);
        Arrays.fill(idChars, paramChars.length, idChars.length, '0');
        final String minString = new String(idChars);
        ObjId min;
        try {
            min = new ObjId(minString);
        } catch (IllegalArgumentException e) {
            if (!minString.startsWith("00"))
                throw new ParseException(ctx, "invalid object ID");
            min = null;
        }
        Arrays.fill(idChars, paramChars.length, idChars.length, 'f');
        ObjId max;
        try {
            max = new ObjId(new String(idChars));
        } catch (IllegalArgumentException e) {
            max = null;
        }

        // TODO: if multiple storage ID's match, complete only through the storage ID

        // Find object IDs in the range
        final CompletionFinder finder = new CompletionFinder(min, max);
        session.performParseSessionAction(finder);

        // Throw exception with completions
        throw new ParseException(ctx, "invalid object ID").addCompletions(ParseUtil.complete(finder.getCompletions(), param));
    }

    private static class CompletionFinder implements ParseSession.Action, Session.RetryableAction {

        private final ArrayList<String> completions = new ArrayList<>();
        private final ObjId min;
        private final ObjId max;

        CompletionFinder(ObjId min, ObjId max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public void run(ParseSession session) throws Exception {
            final Transaction tx = session.getTransaction();
            final ArrayList<NavigableSet<ObjId>> idSets = new ArrayList<>();
            this.completions.clear();
            for (Schema schema : tx.getSchemas().getVersions().values()) {
                for (ObjType objType : schema.getObjTypes().values()) {
                    final int storageId = objType.getStorageId();
                    if ((this.min != null && storageId < this.min.getStorageId())
                      || (this.max != null && storageId > this.max.getStorageId()))
                        continue;
                    NavigableSet<ObjId> idSet = tx.getAll(storageId);
                    if (this.min != null) {
                        try {
                            idSet = idSet.tailSet(this.min, true);
                        } catch (IllegalArgumentException e) {
                            // ignore
                        }
                    }
                    if (this.max != null) {
                        try {
                            idSet = idSet.headSet(this.max, true);
                        } catch (IllegalArgumentException e) {
                            // ignore
                        }
                    }
                    idSets.add(idSet);
                }
            }
            int count = 0;
            if (!idSets.isEmpty()) {
                for (ObjId id : Iterables.limit(NavigableSets.union(idSets), MAX_COMPLETE_OBJECTS))
                    this.completions.add(id.toString());
            }
        }

        public List<String> getCompletions() {
            return this.completions;
        }
    }
}

