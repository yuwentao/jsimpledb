
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.kv.raft.cmd;

import java.util.Collections;
import java.util.Map;

import org.jsimpledb.Session;
import org.jsimpledb.cli.CliSession;
import org.jsimpledb.kv.KVTransaction;
import org.jsimpledb.kv.raft.Consistency;
import org.jsimpledb.kv.raft.RaftKVDatabase;
import org.jsimpledb.kv.raft.RaftKVTransaction;
import org.jsimpledb.kv.raft.fallback.FallbackKVTransaction;

public abstract class AbstractTransactionRaftCommand extends AbstractRaftCommand {

    protected AbstractTransactionRaftCommand(String spec) {
        super(spec);
    }

    protected abstract class RaftTransactionAction extends RaftAction
      implements Session.RetryableAction, Session.HasTransactionOptions {

        @Override
        public final void run(CliSession session, RaftKVDatabase db) throws Exception {
            KVTransaction kvt = session.getKVTransaction();
            final RaftKVTransaction raftTX;
            if (kvt instanceof RaftKVTransaction)
                raftTX = (RaftKVTransaction)kvt;
            else if (kvt instanceof FallbackKVTransaction) {
                kvt = ((FallbackKVTransaction)kvt).getKVTransaction();
                if (kvt instanceof RaftKVTransaction)
                    raftTX = (RaftKVTransaction)kvt;
                else
                    throw new Exception("Raft Fallback key/value store is currently in standalone mode");
            } else
                throw new Exception("key/value store is not Raft or Raft fallback");
            this.run(session, raftTX);
        }

        @Override
        public Map<String, ?> getTransactionOptions() {
            return Collections.singletonMap(RaftKVDatabase.OPTION_CONSISTENCY, this.getConsistency());
        }

        protected Consistency getConsistency() {
            return Consistency.LINEARIZABLE;
        }

        protected abstract void run(CliSession session, RaftKVTransaction tx) throws Exception;
    }
}

