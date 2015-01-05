
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb;

import org.jsimpledb.core.Database;
import org.jsimpledb.kv.simple.SimpleKVDatabase;

/**
 * Factory for {@link JSimpleDB} instances.
 *
 * <p>
 * If no {@link Database} is configured, newly created {@link JSimpleDB} instances will use an initially empty,
 * in-memory {@link SimpleKVDatabase}.
 * </p>
 *
 * @see JSimpleDB
 */
public class JSimpleDBFactory {

    private Database database;
    private int schemaVersion;
    private StorageIdGenerator storageIdGenerator = new DefaultStorageIdGenerator();
    private Iterable<? extends Class<?>> classes;

    /**
     * Configure the Java model classes.
     *
     * <p>
     * Note: {@link org.jsimpledb.annotation.JSimpleClass &#64;JSimpleClass}-annotated super-types of any
     * class in {@code classes} will be included, even if the super-type is not explicitly specified in {@code classes}.
     * </p>
     *
     * @param classes classes annotated with {@link org.jsimpledb.annotation.JSimpleClass &#64;JSimpleClass} annotations
     */
    public JSimpleDBFactory setModelClasses(Iterable<? extends Class<?>> classes) {
        this.classes = classes;
        return this;
    }

    /**
     * Configure the underlying {@link Database} for this instance.
     *
     * <p>
     * By default this instance will use an initially empty, in-memory {@link SimpleKVDatabase}.
     * </p>
     *
     * @param database core API database to use
     */
    public JSimpleDBFactory setDatabase(Database database) {
        this.database = database;
        return this;
    }

    /**
     * Configure the schema version number associated with the configured Java model classes.
     *
     * <p>
     * A value of zero means to use whatever is the highest version already recorded in the database.
     * However, if this instance has no {@link Database} configured, then an empty
     * {@link SimpleKVDatabase} is used and therefore a schema version of {@code 1} is assumed.
     * </p>
     *
     * @param schemaVersion the schema version number of the schema derived from the configured Java model classes,
     *  or zero to default to the highest version already recorded in the database
     */
    public JSimpleDBFactory setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
        return this;
    }

    /**
     * Configure the {@link StorageIdGenerator} for auto-generating storage ID's when not explicitly
     * specified in {@link org.jsimpledb.annotation.JSimpleClass &#64;JSimpleClass},
     * {@link org.jsimpledb.annotation.JField &#64;JField}, etc., annotations.
     *
     * <p>
     * This instance will initially be configured with a {@link DefaultStorageIdGenerator}.
     * To disable auto-generation of storage ID's altogether, configure a null value here.
     * </p>
     *
     * @param storageIdGenerator storage ID generator, or null to disable auto-generation of storage ID's
     */
    public JSimpleDBFactory setStorageIdGenerator(StorageIdGenerator storageIdGenerator) {
        this.storageIdGenerator = storageIdGenerator;
        return this;
    }

    /**
     * Construct a {@link JSimpleDB} instance using this instance's configuration.
     *
     * @throws IllegalArgumentException if this instance has an incomplete or invalid configuration
     * @throws IllegalArgumentException if any Java model class has an invalid annotation
     */
    public JSimpleDB newJSimpleDB() {
        Database database1 = this.database;
        int schemaVersion1 = this.schemaVersion;
        if (database1 == null) {
            database1 = new Database(new SimpleKVDatabase());
            if (schemaVersion1 == 0)
                schemaVersion1 = 1;
        }
        return new JSimpleDB(database1, schemaVersion1, this.storageIdGenerator, this.classes);
    }
}
