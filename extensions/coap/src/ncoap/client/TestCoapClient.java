package ncoap.client;

import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.message.*;
import java.net.*;

/*
 * To send a single request to coap://example.org:5683/test 
 * --host example.org --port 5683 --path /test --non --duration 20
 * To start the observation of coap://example.org:5683/obs 
 * --host example.org --port 5683 --path /obs --observing --maxUpdates 5 --duration 60
 */
/**
 *
 * @author stefanopiozingaro
 */
public class TestCoapClient extends CoapClient {

    private ClientCallback callback;

    private final String uriHost = "coap.me";
    private final int uriPort = 5683;
    private final String uriPath = "/test";
    private final String uriQuery = null;
    private final boolean non = false;
    private final int duration = 60;
    private final boolean observe = false;
    private final int maxUpdates = 1;
    private final String proxyAddress = null;
    private final int proxyPort = 5683;
    private final boolean useProxy = false;

    public TestCoapClient() {
        super();
    }

    private void sendCoapRequest() throws URISyntaxException, UnknownHostException {

        // determine the URI of the resource to be requested
        URI resourceURI = new URI("coap", null, uriHost, uriPort, uriPath, uriQuery, null);

        // create the request
        int messageType = non ? MessageType.NON : MessageType.CON;
        CoapRequest coapRequest = new CoapRequest(messageType, MessageCode.GET, resourceURI, useProxy);

        // observe resource or not?
        if (observe) {
            coapRequest.setObserve(0);
        }

        // determine recipient (proxy or webresource host)
        InetSocketAddress remoteSocket;
        if (useProxy) {
            InetAddress inetProxyAddress = InetAddress.getByName(proxyAddress);
            remoteSocket = new InetSocketAddress(inetProxyAddress, proxyPort);
        } else {
            InetAddress serverAddress = InetAddress.getByName(uriHost);
            int serverPort = uriPort;
            remoteSocket = new InetSocketAddress(serverAddress, serverPort);
        }

        // define the client callback if observing
        if (observe) {

            this.callback = new ClientCallback() {

                private int responseCounter = 0;
                private int transmissionCounter = 0;
                private boolean timedOut = false;

                @Override
                public void processCoapResponse(CoapResponse coapResponse) {
                    int value = responseCounter++;
                }

                public int getResponseCount() {
                    return responseCounter;
                }

                @Override
                public void processRetransmission() {
                    int value = transmissionCounter++;
                }

                @Override
                public void processTransmissionTimeout() {
                    timedOut = true;
                }

                @Override
                public void processResponseBlockReceived(long receivedLength, long expectedLength) {
                }

                public boolean isTimedOut() {
                    return timedOut;
                }

                @Override
                public boolean continueObservation() {
                    boolean result = getResponseCount() < maxUpdates;
                    return result;
                }
            };

        } else {

            this.callback = new ClientCallback() {

                private boolean responseReceived = false;
                private int transmissionCounter = 0;
                private boolean timedOut = false;

                @Override
                public void processCoapResponse(CoapResponse coapResponse) {
                    this.responseReceived = true;
                }

                public int getResponseCount() {
                    return this.responseReceived ? 1 : 0;
                }

                @Override
                public void processRetransmission() {
                    int value = transmissionCounter++;
                }

                @Override
                public void processTransmissionTimeout() {
                    timedOut = true;
                }

                @Override
                public void processResponseBlockReceived(long receivedLength, long expectedLength) {
                }

                public boolean isTimedOut() {
                    return timedOut;
                }
            };
        }

        //Send the CoAP request
        this.sendCoapRequest(coapRequest, remoteSocket, callback);
    }

    private void safelyShutdown() throws InterruptedException {

        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) / 1000 <= duration) {
            Thread.sleep(100);
        }

        //Wait for another 10 seconds to answer the next update notification with a RST to stop the observation
        if (observe) {
            Thread.sleep(10000);
        }

        this.shutdown();
    }

    public static void main(String[] args) throws Exception {

        // Start the client
        TestCoapClient client = new TestCoapClient();

        // Send the request
        client.sendCoapRequest();
        client.safelyShutdown();

    }
}
