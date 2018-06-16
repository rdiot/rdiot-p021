package com.rdiot.pi4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;


public class dht11 {

	private int[] dht11_data = {0,0,0,0,0};
	private static final int MAX_TIME = 85;
	
	public dht11() {
		if (Gpio.wiringPiSetup() == -1) {
			System.out.println("GPIO Setup Failed");
			return;
		}
		
		GpioUtil.export(3, GpioUtil.DIRECTION_OUT); //OUTPUT
	}
	
    public String getTemperature() throws JsonProcessingException {
    	String value = null;    	
        int laststate = Gpio.HIGH;
        int j = 0;
        dht11_data[0] = dht11_data[1] = dht11_data[2] = dht11_data[3] = dht11_data[4] = 0;
  
        Gpio.pinMode(3, Gpio.OUTPUT);
        Gpio.digitalWrite(3, Gpio.LOW);
        Gpio.delay(18);
  
        Gpio.digitalWrite(3, Gpio.HIGH);       
        Gpio.pinMode(3, Gpio.INPUT);
  
        for (int i = 0; i < MAX_TIME; i++) {
           int counter = 0;
           while (Gpio.digitalRead(3) == laststate) {
               counter++;
               Gpio.delayMicroseconds(1);
               if (counter == 255) {
                   break;
               }
           }
  
           laststate = Gpio.digitalRead(3);
  
           if (counter == 255) {
               break;
           }
  
           /* ignore first 3 transitions */
           if ((i >= 4) && (i % 2 == 0)) {
              /* shove each bit into the storage bytes */
              dht11_data[j / 8] <<= 1;
              if (counter > 16) {
                  dht11_data[j / 8] |= 1;
              }
              j++;
            }
         }
         // check we read 40 bits (8bit x 5 ) + verify checksum in the last
         // byte
         if ((j >= 40) && checkParity()) {
             float h = (float)((dht11_data[0] << 8) + dht11_data[1]) / 10;
             if ( h > 100 )
             {
                 h = dht11_data[0];   // for DHT11
             }
             float c = (float)(((dht11_data[2] & 0x7F) << 8) + dht11_data[3]) / 10;
             if ( c > 125 )
             {
                 c = dht11_data[2];   // for DHT11
             }
             if ( (dht11_data[2] & 0x80) != 0 )
             {
                 c = -c;
             }
             float f = c * 1.8f + 32;
             System.out.println( "Temperature = " + c + "(" + f + "f)" + " Humidity = " + h );
                          
             value = "{\"temperature\":\""+ c + "\", \"humidity\":\""+h+"\"}";
             
             System.out.println(value);

         }else  {
             System.out.println( "Data not good, skip" );

         }         
         return value;
     }
  
    private boolean checkParity() {
        return (dht11_data[4] == ((dht11_data[0] + dht11_data[1] + dht11_data[2] + dht11_data[3]) & 0xFF));
      }
	
}
