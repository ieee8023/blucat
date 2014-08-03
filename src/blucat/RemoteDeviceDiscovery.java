package blucat;
/* 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.bluetooth.*;
import javax.microedition.io.Connector;

import com.intel.bluetooth.RemoteDeviceHelper;

/**
 * 
 * @author Joseph Paul Cohen
 *
 */
public class RemoteDeviceDiscovery {
	
    public final static Set<RemoteDevice> devicesDiscovered = new HashSet<RemoteDevice>();

    public static void runDiscovery() throws IOException, InterruptedException{
    	PrintUtil.out.println("#" + "Searching for devices");
    	RemoteDeviceDiscovery.findDevices();
    	for (RemoteDevice d : devicesDiscovered){
    		
    		PrintUtil.out.println("+," + deviceName(d));
    	}
    	
    	PrintUtil.out.println("#" + "Found " + devicesDiscovered.size() + " device(s)");
    }
    
    public static void findDevices() throws IOException, InterruptedException {

        final Object inquiryCompletedEvent = new Object();

        devicesDiscovered.clear();

        DiscoveryListener listener = new DiscoveryListener() {

            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                PrintUtil.vverbose("#" + "Device " + btDevice.getBluetoothAddress() + " found");
                devicesDiscovered.add(btDevice);
                try {
                	PrintUtil.vverbose("#" + "     name " + btDevice.getFriendlyName(false));
                } catch (IOException cantGetDeviceName) {
                }
            }

            public void inquiryCompleted(int discType) {
            	PrintUtil.vverbose("#" + "Device Inquiry completed!");
                synchronized(inquiryCompletedEvent){
                    inquiryCompletedEvent.notifyAll();
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {
            }

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
            	PrintUtil.vverbose("#" + "servicesDiscovered");
            }
        };

        synchronized(inquiryCompletedEvent) {
            
        	LocalDevice ld = LocalDevice.getLocalDevice();
        	
        	PrintUtil.vverbose("#My Name is:" + ld.getFriendlyName());
        	
//        	boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.LIAC, listener);
//            if (started) {
//            	PrintUtil.vverbose("wait for device inquiry to complete...");
//                inquiryCompletedEvent.wait();
//                PrintUtil.vverbose(devicesDiscovered.size() +  " device(s) found");
//            }
        	
        	
        	boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
            if (started) {
            	PrintUtil.vverbose("#" + "wait for device inquiry to complete...");
                inquiryCompletedEvent.wait();
                PrintUtil.vverbose("#" + devicesDiscovered.size() +  " device(s) found");
            }
        }
        
    }

    public static Set<RemoteDevice> getDevices(){
    	
    	return devicesDiscovered;
    }
    
    public static String deviceName(RemoteDevice d){
    	
    	String address = d.getBluetoothAddress();
		
		String name = "";
		try{
			name = d.getFriendlyName(false);
		}catch (IOException e){
			PrintUtil.verbose("#Error: " + e.getMessage());
			try{
				name = d.getFriendlyName(false);
			}catch (IOException e2){
				PrintUtil.verbose("#Error: " + e2.getMessage());
			}
			
		}
		
		String rssi = "NA";
		

		
		String toret = "";
		
		if (BlucatState.csv)
			toret += (new Date()).getTime() + ", ";
		
		toret+= BluCatUtil.clean(address) + ", " +
				"\"" + BluCatUtil.clean(name) + "\", " +
				"Trusted:" + d.isTrustedDevice() + ", " +
				"Encrypted:" + d.isEncrypted();
		
		
		if (BlucatState.rssi){
			
			try {
				rssi = String.valueOf(RemoteDeviceHelper.readRSSI(d));
			} catch (Throwable e) {
				
				// now connect and try
				String url  = "btl2cap://" + d.getBluetoothAddress() + ":1";
				
				try {
					BlucatState.connection = Connector.open(url,Connector.READ_WRITE,true);
					rssi = String.valueOf(RemoteDeviceHelper.readRSSI(d));
					BlucatState.connection.close();
					
				} catch (IOException e1) {}
			}
			
			toret+= ", " + rssi;
			
		}
			
		return toret;
		
    }
    
    
}