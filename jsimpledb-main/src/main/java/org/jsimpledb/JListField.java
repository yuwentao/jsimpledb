
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb;

import com.google.common.base.Preconditions;

import java.lang.reflect.Method;
import java.util.List;

import org.jsimpledb.schema.ListSchemaField;

/**
 * Represents a list field in a {@link JClass}.
 */
public class JListField extends JCollectionField {

    JListField(JSimpleDB jdb, String name, int storageId, JSimpleField elementField, String description, Method getter) {
        super(jdb, name, storageId, elementField, description, getter);
    }

    @Override
    public List<?> getValue(JObject jobj) {
        Preconditions.checkArgument(jobj != null, "null jobj");
        return jobj.getTransaction().readListField(jobj.getObjId(), this.storageId, false);
    }

    @Override
    public <R> R visit(JFieldSwitch<R> target) {
        return target.caseJListField(this);
    }

    @Override
    ListSchemaField toSchemaItem(JSimpleDB jdb) {
        final ListSchemaField schemaField = new ListSchemaField();
        super.initialize(jdb, schemaField);
        return schemaField;
    }

    @Override
    JListFieldInfo toJFieldInfo() {
        return new JListFieldInfo(this);
    }

// Bytecode generation

    @Override
    Method getFieldReaderMethod() {
        return ClassGenerator.READ_LIST_FIELD_METHOD;
    }
}

