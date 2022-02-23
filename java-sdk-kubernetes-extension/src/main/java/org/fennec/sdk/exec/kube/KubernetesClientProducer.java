package org.fennec.sdk.exec.kube;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Unique endpoint for kubernetes client
 */
public class KubernetesClientProducer {

    private static final KubernetesClientProducer PRODUCER = new KubernetesClientProducer();
    private final KubernetesClient client;

    private KubernetesClientProducer() {
        client = new DefaultKubernetesClient();
    }

    public static KubernetesClient getClient() {
        return PRODUCER.client;
    }
}
