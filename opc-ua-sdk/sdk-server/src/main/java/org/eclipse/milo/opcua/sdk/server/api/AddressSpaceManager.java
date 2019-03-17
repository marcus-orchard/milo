/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class AddressSpaceManager extends AddressSpaceComposite {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<NodeManager<UaNode>> nodeManagers = new CopyOnWriteArrayList<>();

    public AddressSpaceManager(OpcUaServer server) {
        super(server);
    }

    /**
     * Register a {@link NodeManager} with this {@link AddressSpaceManager}.
     *
     * @param nodeManager the {@link NodeManager} to register.
     */
    public synchronized void register(NodeManager<UaNode> nodeManager) {
        if (!nodeManagers.contains(nodeManager)) {
            nodeManagers.add(nodeManager);
        } else {
            logger.warn("NodeManager already registered: {}", nodeManager);
        }
    }

    /**
     * Unregister a {@link NodeManager} with this {@link AddressSpaceManager}.
     *
     * @param nodeManager the {@link NodeManager} to register.
     */
    public synchronized void unregister(NodeManager<UaNode> nodeManager) {
        if (nodeManagers.contains(nodeManager)) {
            nodeManagers.remove(nodeManager);
        } else {
            logger.warn("NodeManager not registered: {}", nodeManager);
        }
    }

    public CompletableFuture<List<Reference>> browseAll(AccessContext context, NodeId nodeId) {
        ViewDescription view = new ViewDescription(
            NodeId.NULL_VALUE,
            DateTime.NULL_VALUE,
            uint(0)
        );

        return browseAll(context, view, nodeId);
    }

    public CompletableFuture<List<Reference>> browseAll(AccessContext context, ViewDescription view, NodeId nodeId) {
        BrowseContext browseContext = new BrowseContext(
            getServer(),
            context.getSession().orElse(null)
        );

        browse(browseContext, view, nodeId);

        return browseContext.getFuture();
    }

    /**
     * Get the managed {@link UaNode} identified by {@code nodeId} from the first registered {@link NodeManager} that
     * has it, if there is one.
     *
     * @param nodeId the {@link NodeId} identifying the managed {@link UaNode}.
     * @return the managed {@link UaNode} identified by {@code nodeId}, if there is one.
     */
    public Optional<UaNode> getManagedNode(NodeId nodeId) {
        return nodeManagers.stream()
            .filter(n -> n.containsNode(nodeId))
            .findFirst()
            .flatMap(n -> n.getNode(nodeId));
    }

    /**
     * Get the managed {@link UaNode} identified by {@code nodeId} from the first registered {@link NodeManager} that
     * has it, if there is one.
     *
     * @param nodeId the {@link ExpandedNodeId} identifying the managed {@link UaNode}.
     * @return the managed {@link UaNode} identified by {@code nodeId}, if there is one.
     */
    public Optional<UaNode> getManagedNode(ExpandedNodeId nodeId) {
        return nodeId.local().flatMap(this::getManagedNode);
    }

    /**
     * Collect all {@link Reference}s from all registered {@link NodeManager}s where {@code nodeId} is the source
     * {@link NodeId} in the Reference.
     *
     * @param nodeId the {@link NodeId} of the source NodeId in the {@link Reference}.
     * @return all {@link Reference}s from all registered {@link NodeManager}s where {@code nodeId} is the source
     * {@link NodeId} in the Reference.
     */
    public List<Reference> getManagedReferences(NodeId nodeId) {
        return nodeManagers.stream()
            .map(n -> n.getReferences(nodeId))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * Collect all {@link Reference}s from all registered {@link NodeManager}s where {@code nodeId} is the source
     * {@link NodeId} in the Reference and the Reference passes {@code filter}.
     *
     * @param nodeId the {@link NodeId} of the source NodeId in the {@link Reference}.
     * @param filter a {@link Predicate} to apply to the collected {@link Reference}s.
     * @return all {@link Reference}s from all registered {@link NodeManager}s where {@code nodeId} is the source
     * {@link NodeId} in the Reference.
     */
    public List<Reference> getManagedReferences(NodeId nodeId, Predicate<Reference> filter) {
        return nodeManagers.stream()
            .map(n -> n.getReferences(nodeId, filter))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

}
