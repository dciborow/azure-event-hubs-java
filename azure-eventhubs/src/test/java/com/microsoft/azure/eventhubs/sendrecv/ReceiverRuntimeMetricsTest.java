/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.eventhubs.sendrecv;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.microsoft.azure.eventhubs.ConnectionStringBuilder;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import com.microsoft.azure.eventhubs.ReceiverOptions;
import com.microsoft.azure.eventhubs.EventHubsException;
import com.microsoft.azure.eventhubs.lib.ApiTestBase;
import com.microsoft.azure.eventhubs.lib.TestBase;
import com.microsoft.azure.eventhubs.lib.TestContext;

public class ReceiverRuntimeMetricsTest  extends ApiTestBase {
    
    static final String cgName = TestContext.getConsumerGroupName();
    static final String partitionId = "0";
    static final Instant beforeTestStart = Instant.now();
    static final int sentEvents = 25;

    static EventHubClient ehClient;

    static PartitionReceiver receiverWithOptions = null;
    static PartitionReceiver receiverWithoutOptions = null;
    static PartitionReceiver receiverWithOptionsDisabled = null;

    @BeforeClass
    public static void initializeEventHub()  throws Exception {
        
        final ConnectionStringBuilder connectionString = TestContext.getConnectionString();
        ehClient = EventHubClient.createFromConnectionStringSync(connectionString.toString());
        
        ReceiverOptions options = new ReceiverOptions();
        options.setReceiverRuntimeMetricEnabled(true);
        
        ReceiverOptions optionsWithMetricsDisabled = new ReceiverOptions();
        optionsWithMetricsDisabled.setReceiverRuntimeMetricEnabled(false);

        receiverWithOptions = ehClient.createReceiverSync(cgName, partitionId, Instant.now(), options);
        receiverWithoutOptions = ehClient.createReceiverSync(cgName, partitionId, Instant.EPOCH);
        receiverWithOptionsDisabled = ehClient.createReceiverSync(cgName, partitionId, Instant.EPOCH, optionsWithMetricsDisabled);
        
        TestBase.pushEventsToPartition(ehClient, partitionId, sentEvents).get();
    }

    @Test()
    public void testRuntimeMetricsReturnedWhenEnabled() throws EventHubsException {

        LinkedList<EventData> receivedEventsWithOptions = new LinkedList<>();
        while (receivedEventsWithOptions.size() < sentEvents)
            for (EventData eData: receiverWithOptions.receiveSync(sentEvents))
                receivedEventsWithOptions.add(eData);
        
        HashSet<String> offsets = new HashSet<>();
        for (EventData eData: receivedEventsWithOptions)
            offsets.add(eData.getSystemProperties().getOffset());
        
        Assert.assertTrue(receiverWithOptions.getRuntimeInformation() != null);
        Assert.assertTrue(receiverWithOptions.getRuntimeInformation().getLastEnqueuedTime().isAfter(beforeTestStart));
        Assert.assertTrue(offsets.contains(receiverWithOptions.getRuntimeInformation().getLastEnqueuedOffset()));
        Assert.assertTrue(receiverWithOptions.getRuntimeInformation().getLastSequenceNumber() >= receivedEventsWithOptions.iterator().next().getSystemProperties().getSequenceNumber());
    }

    @Test()
    public void testRuntimeMetricsWhenDisabled() throws EventHubsException {

        receiverWithOptionsDisabled.receiveSync(10);
        Assert.assertTrue(receiverWithOptionsDisabled.getRuntimeInformation() == null);
    }
    
    @Test()
    public void testRuntimeMetricsDefaultDisabled() throws EventHubsException {

        receiverWithoutOptions.receiveSync(10);
        Assert.assertTrue(receiverWithoutOptions.getRuntimeInformation() == null);
    }
    
    @AfterClass()
    public static void cleanup() throws EventHubsException {
        
        if (receiverWithOptions != null)
            receiverWithOptions.closeSync();
        
        if (receiverWithoutOptions != null)
            receiverWithoutOptions.closeSync();

        if (receiverWithOptionsDisabled != null)
            receiverWithOptionsDisabled.closeSync();
        
        if (ehClient != null)
            ehClient.closeSync();
    }
}
