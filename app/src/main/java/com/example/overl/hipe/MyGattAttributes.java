package com.example.overl.hipe;

import com.junkchen.blelib.GattAttributes;

/**
 * Created by JunkChen on 2016/3/14 0014.
 */
public class MyGattAttributes extends GattAttributes {
    //GATT Characteristics
    public static final String CHARACTERISTIC_CONFIG_CONTROL = "33221111-5544-7766-9988-AABBCCDDEEFF";
    public static final String CHARACTERISTIC_CONFIG_PASSWORD = "33221112-5544-7766-9988-AABBCCDDEEFF";
    public static final String CHARACTERISTIC_CONFIG_STATUS = "33221113-5544-7766-9988-AABBCCDDEEFF";
    public static final String SERVICE_UART = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final String CHARACTERISTIC_UART_SEND = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final String CHARACTERISTIC_UART_RECEIVE= "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";

    /*public MyGattAttributes() {
        attributes.put(CHARACTERISTIC_CONFIG_CONTROL, "Config Control");
        attributes.put(CHARACTERISTIC_CONFIG_PASSWORD, "Config Password");
        attributes.put(CHARACTERISTIC_CONFIG_STATUS, "Config Status");
        attributes.put(SERVICE_UART, "Service Uart");
        attributes.put(CHARACTERISTIC_UART_SEND, "Characteristic Uart Send");
        attributes.put(CHARACTERISTIC_UART_RECEIVE, "Characteristic Uart Receive");
    }*/

    static {
        // Characteristics name.
        attributes.put(CHARACTERISTIC_CONFIG_CONTROL, "Config Control");
        attributes.put(CHARACTERISTIC_CONFIG_PASSWORD, "Config Password");
        attributes.put(CHARACTERISTIC_CONFIG_STATUS, "Config Status");
        attributes.put(SERVICE_UART, "Service Uart");
        attributes.put(CHARACTERISTIC_UART_SEND, "Characteristic Uart Send");
        attributes.put(CHARACTERISTIC_UART_RECEIVE, "Characteristic Uart Receive");
    }
}
