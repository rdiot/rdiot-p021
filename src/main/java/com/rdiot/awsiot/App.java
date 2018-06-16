package com.rdiot.awsiot;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rdiot.pi4j.dht11;


public class App 
{
	private static final String clientEndpoint = "xxx.iot.ap-northeast-2.amazonaws.com";       // replace <prefix> and <region> with your own
	private static final String clientId = "sdk-java";                              // replace with your own client ID. Use unique client IDs for concurrent connections.
	private static final String certificateFile = "/home/pi/AWSIoT/RaspberryPi.cert.pem";                       // X.509 based certificate file
	private static final String privateKeyFile = "/home/pi/AWSIoT/RaspberryPi.private.key";                        // PKCS#1 or PKCS#8 PEM encoded private key file
	
    private static final String topicName = "sensor/dht11";
    private static final AWSIotQos topicQos = AWSIotQos.QOS0;
    
    private static AWSIotMqttClient awsIotClient;
    
    public static void setClient(AWSIotMqttClient client) {
        awsIotClient = client;
    }
	
    public static class NonBlockingPublisher implements Runnable {
        private final AWSIotMqttClient awsIotClient;

        public NonBlockingPublisher(AWSIotMqttClient awsIotClient) {
            this.awsIotClient = awsIotClient;
        }

        @Override
        public void run() {
        	String objectContent = null;
        	dht11 dht = new dht11();

            while (true) {
            	
            	for (int i=0; i<10; i++) {
        	      	
            		try {
            			Thread.sleep(2000);
            		} catch (InterruptedException e) {
            			e.printStackTrace();
            		}
            	            
            	    try {
						objectContent = dht.getTemperature();
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	            
            	    if(objectContent != null) {
            	    	System.out.println(objectContent);        		
            	        break;
            	    }            
            	}
            	
            	String payload = objectContent;

                AWSIotMessage message = new NonBlockingPublishListener(topicName, topicQos, payload);
                try {
                    awsIotClient.publish(message);
                } catch (AWSIotException e) {
                    System.out.println(System.currentTimeMillis() + ": publish failed for " + payload);
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println(System.currentTimeMillis() + ": NonBlockingPublisher was interrupted");
                    return;
                }
            }
        }
    }
    
    
    private static void initClient() {
        
        if (awsIotClient == null && certificateFile != null && privateKeyFile != null) {
          
        	// SampleUtil.java and its dependency PrivateKeyReader.java can be copied from the sample source code.
        	// Alternatively, you could load key store directly from a file - see the example included in this README.
        	KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
        	awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
        }
  
        if (awsIotClient == null) {
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }
    }
    
    
    public static void main( String[] args ) throws AWSIotException, Exception
    {    	

    	initClient();
    	awsIotClient.connect();
    	    	
        Thread nonBlockingPublishThread = new Thread(new NonBlockingPublisher(awsIotClient));
        
        nonBlockingPublishThread.start();
        nonBlockingPublishThread.join();
    	
    }     
}
