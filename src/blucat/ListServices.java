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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import com.intel.bluetooth.BluetoothConsts;

/**
 * 
 * @author Joseph Paul Cohen
 *
 */
public class ListServices {

	
	public static void listServices(String target) throws Exception{
		
		throw new Exception("NYI");
	}
	
	
	
	public static void listServices() throws Exception{
		
		PrintUtil.out.println("#" + "Listing all services");
		@SuppressWarnings("unused")
		Set<ServiceRecord> records = findViaSDP();
	}
	
	

	final static Object serviceSearchCompletedEvent = new Object();
	

	static Set<ServiceRecord> findViaSDP() throws Exception{
		
		Set<ServiceRecord> toReturn = new HashSet<ServiceRecord>();
		
		UUID[] uuidSet ={
				//new UUID(0x1002),
			//BluetoothConsts.RFCOMM_PROTOCOL_UUID,
			BluetoothConsts.L2CAP_PROTOCOL_UUID
//			BluetoothConsts.OBEX_PROTOCOL_UUID,
//			new UUID(0x0003)

			
			
		};
		
        int[] attrIDs =  new int[] {
                0x0100 // Service name
                ,0x0003
        };
		
        
        RemoteDeviceDiscovery.findDevices();
		Set<RemoteDevice> devices  = RemoteDeviceDiscovery.getDevices();
		
		
		for (RemoteDevice remote : devices){
        
	        synchronized(serviceSearchCompletedEvent) {
	        	
	        	PrintUtil.verbose("#" + "Searching for services on ");
	            PrintUtil.out.println("+," + RemoteDeviceDiscovery.deviceName(remote));
	        
	            LocalDevice.getLocalDevice().getDiscoveryAgent()
	            .searchServices(attrIDs, uuidSet, remote, new ServiceDiscoveryListener(toReturn));
	            
	            serviceSearchCompletedEvent.wait();
	        }
		
		}
		return toReturn;
	}
	
	
	static class ServiceDiscoveryListener implements DiscoveryListener{

		Set<ServiceRecord> toReturn;
		
		public ServiceDiscoveryListener(Set<ServiceRecord> toReturn) {
			
			this.toReturn = toReturn;
		}
		
		@Override
		public void deviceDiscovered(RemoteDevice arg0, DeviceClass arg1) {
			//PrintUtil.out.println("deviceDiscovered");
		}

		@Override
		public void inquiryCompleted(int arg0) {
			//PrintUtil.out.println("done");
			
		}

		@Override
		public void serviceSearchCompleted(int arg0, int arg1) {
			
			//PrintUtil.out.println("service search completed!");
            synchronized(serviceSearchCompletedEvent){
                serviceSearchCompletedEvent.notifyAll();
            }
			
		}

		@Override
		public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
			
			//PrintUtil.out.println(arg1);
			for (ServiceRecord servRec : arg1) {
				printServiceRecord(servRec);
				toReturn.add(servRec);
            }
		}
	}
	
	
	private static void printServiceRecord(ServiceRecord rec){
		
		try{
			String name = "";
			if (rec.getAttributeValue(0x0100) != null)
				name = "" + rec.getAttributeValue(0x0100).getValue();
			
			String desc = "";
			if (rec.getAttributeValue(0x0003) != null)
				desc = "" + rec.getAttributeValue(0x0003).getValue();
			
			
			String url = rec.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
			if (url != null)
				url = url.substring(0, url.indexOf(";"));
			
			
			String remoteMac = rec.getHostDevice().getBluetoothAddress();
			String remoteName = rec.getHostDevice().getFriendlyName(false);
			
			PrintUtil.out.print("-,");
			
			if (BlucatState.csv)
				PrintUtil.out.print((new Date()).getTime() + ", " + BluCatUtil.clean(remoteMac) + ", \"" + BluCatUtil.clean(remoteName) + "\", ");
			
			PrintUtil.out.println("\"" + BluCatUtil.clean(name) + "\", \"" + BluCatUtil.clean(desc) + "\", " + BluCatUtil.clean(url));
			
			if (BlucatState.verbose){
				PrintUtil.out.println("  #Attributes Returned " + rec.getAttributeIDs().length );
				for (int i : rec.getAttributeIDs()){
					DataElement val = rec.getAttributeValue(i);
					
					@SuppressWarnings("deprecation")
					String sval = val.toString();
					sval = sval.replace("\n", "\n          ");
					
					PrintUtil.out.println("  #" + String.format("0x%04x",i) + "=" + sval);
				}
			}
			
			
			}catch(Exception e){
				
				PrintUtil.out.println("#Error: " + e.getMessage());
				e.printStackTrace();
			}
	}
}
