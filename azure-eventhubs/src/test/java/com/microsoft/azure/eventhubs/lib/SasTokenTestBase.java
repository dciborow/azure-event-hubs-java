/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.eventhubs.lib;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.microsoft.azure.eventhubs.ConnectionStringBuilder;
import com.microsoft.azure.eventhubs.EventHubsException;
import com.microsoft.azure.eventhubs.SharedAccessSignatureTokenProvider;

public class SasTokenTestBase extends ApiTestBase {
    
    private static ConnectionStringBuilder originalConnectionString;
    
    @BeforeClass
    public static void replaceConnectionString()  throws Exception {
        
        originalConnectionString = TestContext.getConnectionString();
        final String connectionStringWithSasToken = new ConnectionStringBuilder(
                    originalConnectionString.getEndpoint(),
                    originalConnectionString.getEntityPath(),
                    SharedAccessSignatureTokenProvider.generateSharedAccessSignature(originalConnectionString.getSasKeyName(),
                            originalConnectionString.getSasKey(), 
                            String.format("amqp://%s/%s", originalConnectionString.getEndpoint().getHost(), originalConnectionString.getEntityPath()),
                            Duration.ofDays(1)))
                    .toString();

        TestContext.setConnectionString(connectionStringWithSasToken);
    }
    
    @AfterClass
    public static void undoReplace() throws EventHubsException {
        
        TestContext.setConnectionString(originalConnectionString.toString());
    }
}
