
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.vaadin;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.vaadin.data.Container;
import com.vaadin.shared.ui.label.ContentMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.dellroad.stuff.vaadin7.ProvidesProperty;
import org.dellroad.stuff.vaadin7.SimpleKeyedContainer;
import org.jsimpledb.JClass;
import org.jsimpledb.JSimpleDB;
import org.jsimpledb.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hierarical container that contains the tree of all Java model classes in the database schema.
 */
@SuppressWarnings("serial")
public class TypeContainer extends SimpleKeyedContainer<Class<?>, TypeContainer.Node> implements Container.Hierarchical {

    public static final String NAME_PROPERTY = "name";
    public static final String STORAGE_ID_PROPERTY = "storageId";
    public static final String TYPE_PROPERTY = "type";

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final JSimpleDB jdb;
    private final Class<?> rootType;
    private final ArrayList<Node> rootList = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param jdb underlying database
     */
    public TypeContainer(JSimpleDB jdb) {
        this(jdb, null);
    }

    /**
     * Constructor.
     *
     * @param jdb underlying database
     * @param type type restrictions, or null for none
     */
    public TypeContainer(JSimpleDB jdb, Class<?> type) {
        super(Node.class);
        Preconditions.checkArgument(jdb != null, "null jdb");
        this.jdb = jdb;

        // Get the types of all JClasses assignable to the given type, and use lowest common ancestor as the "top type"
        final HashSet<Class<?>> types = new HashSet<>();
        for (JClass<?> jclass : this.jdb.getJClasses().values()) {
            if (type == null || type.isAssignableFrom(jclass.getType()))
                types.add(jclass.getType());
        }
        this.rootType = !types.isEmpty() ? Util.findLowestCommonAncestorOfClasses(types).getRawType() : Object.class;
    }

    @Override
    public void connect() {
        super.connect();
        this.reload();
    }

    @Override
    public Class<?> getKeyFor(Node node) {
        return node.getType();
    }

    public Class<?> getRootType() {
        return this.rootType;
    }

    public void reload() {

        // Node set
        final ArrayList<Node> nodes = new ArrayList<>();

        // Create Node's for each JClass
        boolean addedRoot = false;
        for (JClass<?> jclass : this.jdb.getJClasses().values()) {
            if (this.rootType.isAssignableFrom(jclass.getType())) {
                nodes.add(new Node(jclass));
                if (jclass.getType() == this.rootType)
                    addedRoot = true;
            }
        }

        // Ensure there is a root node
        if (!addedRoot)
            nodes.add(new Node(this.rootType));

        // Sort by name
        Collections.sort(nodes, new Comparator<Node>() {
            @Override
            public int compare(Node node1, Node node2) {
                return node1.propertyName().compareTo(node2.propertyName());
            }
        });

        // Determine parents by inspecting Java types
        for (Node node1 : nodes) {
            final Class<?> type1 = node1.getType();
            for (Node node2 : nodes) {
                if (node2 == node1)
                    continue;
                final Class<?> type2 = node2.getType();
                if (type2.isAssignableFrom(type1)) {
                    final Node previous = node1.getParent();
                    if (previous == null || previous.getType().isAssignableFrom(type2))
                        node1.setParent(node2);
                }
            }
        }

        // Derive children
        for (Node node1 : nodes) {
            for (Node node2 : nodes) {
                if (node2 == node1.getParent())
                    node2.getChilds().add(node1);
            }
        }

        // Derive roots
        this.rootList.clear();
        for (Node node : nodes) {
            if (node.getParent() == null)
                this.rootList.add(node);
        }

        // Load container
        this.load(nodes);
    }

// Node

    public static class Node {

        public static final Function<Node, Class<?>> TYPE_FUNCTION = new Function<Node, Class<?>>() {
            @Override
            public Class<?> apply(Node node) {
                return node != null ? node.getType() : null;
            }
        };

        private final Class<?> type;
        private final JClass<?> jclass;
        private final ArrayList<Node> childs = new ArrayList<>();

        private Node parent;

        public Node(JClass<?> jclass) {
            this(jclass.getType(), jclass);
        }

        public Node(Class<?> type) {
            this(type, null);
        }

        private Node(Class<?> type, JClass<?> jclass) {
            Preconditions.checkArgument(type != null, "null type");
            this.type = type;
            this.jclass = jclass;
        }

        public Class<?> getType() {
            return this.type;
        }

        public Node getParent() {
            return this.parent;
        }
        public void setParent(Node parent) {
            this.parent = parent;
        }

        public List<Node> getChilds() {
            return this.childs;
        }

        @ProvidesProperty(TypeContainer.NAME_PROPERTY)
        public String propertyName() {
            return this.jclass != null ? this.jclass.getName() : this.type.getSimpleName();
        }

        @ProvidesProperty(TypeContainer.STORAGE_ID_PROPERTY)
        public Integer propertyStorageId() {
            return this.jclass != null ? this.jclass.getStorageId() : null;
        }

        @ProvidesProperty(TypeContainer.TYPE_PROPERTY)
        public SizedLabel proeprtyType() {
            return new SizedLabel("<code>" + this.getType().toString() + "</code>", ContentMode.HTML);
        }
    }

// Container.Hierarchical methods

    @Override
    public Collection<Class<?>> getChildren(Object itemId) {
        final Node node = this.getJavaObject(itemId);
        return node != null ? Lists.transform(node.getChilds(), Node.TYPE_FUNCTION) : Collections.<Class<?>>emptySet();
    }

    @Override
    public Class<?> getParent(Object itemId) {
        final Node child = this.getJavaObject(itemId);
        if (child == null)
            return null;
        final Node parent = child.getParent();
        if (parent == null)
            return null;
        return parent.getType();
    }

    @Override
    public Collection<Class<?>> rootItemIds() {
        return Lists.transform(this.rootList, Node.TYPE_FUNCTION);
    }

    @Override
    public boolean areChildrenAllowed(Object itemId) {
        final Node node = this.getJavaObject(itemId);
        return node != null && !node.getChilds().isEmpty();
    }

    @Override
    public boolean isRoot(Object itemId) {
        final Node node = this.getJavaObject(itemId);
        return node != null && node.getParent() == null;
    }

    @Override
    public boolean hasChildren(Object itemId) {
        final Node node = this.getJavaObject(itemId);
        return node != null && !node.getChilds().isEmpty();
    }

// Mutators are not supported

    @Override
    public boolean setParent(Object itemId, Object newParentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeItem(Object itemId) {
        throw new UnsupportedOperationException();
    }
}

