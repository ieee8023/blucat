package blucat;
import java.lang.reflect.Field;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;

import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.BlueCoveLocalDeviceProperties;
import com.intel.bluetooth.BluetoothStack;


public class BluCatUtil {

	
	public static void doctorDevice() throws BluetoothStateException{
		
		if(System.getProperty("os.name").contains("Linux")){

			PrintUtil.err.println("Is libbluetooth3 and libbluetooth-dev installed?");
			
			PrintUtil.err.println("run: sudo apt-get install libbluetooth3 libbluetooth-dev");
			
		}
		
		
		
		if (!LocalDevice.isPowerOn()){
			
			PrintUtil.err.println("#There is no Bluetooth Adaptor powered on");
			System.exit(-1);
		}
		
	
		

		PrintUtil.out.println(" BlueCoveState");
		try{
			PrintUtil.out.println("  ThreadBluetoothStackID = " + BlueCoveImpl.getThreadBluetoothStackID());
			PrintUtil.out.println("  CurrentThreadBluetoothStackID = " + BlueCoveImpl.getCurrentThreadBluetoothStackID());
			PrintUtil.out.println("  LocalDevicesID = " + BlueCoveImpl.getLocalDevicesID());
		}catch(Exception e){
			PrintUtil.out.println("Error enabling bluecove stack: " + e.getMessage());
			return;
		}
		
	
		
		
		PrintUtil.out.println(" BlueCoveConfigProperties");
		

		try{
			String result = "";
			
			
			for (Field f : BlueCoveConfigProperties.class.getDeclaredFields()){
			
				if (f.getName().startsWith("PROPERTY")){
					PrintUtil.out.print( "  " + f.getName() + " = ");
					try{
						result = String.valueOf(LocalDevice.getProperty(String.valueOf(f.get(null))));
					}catch(Exception e){
						result = e.getMessage();
					}catch(IllegalAccessError iae){
						result = "IllegalAccessError";
					}
						PrintUtil.out.println(result);
				}
			}
			
		}catch(Exception e){
			PrintUtil.out.println("Error getting properties " + e.getMessage());
			//e.printStackTrace();
			return;
		}
		
		
		PrintUtil.out.println(" LocalDeviceProperties");
		try{
			String result = "";
			

			String[] deviceprops = {
					"bluetooth.api.version",
					"bluetooth.master.switch",
					"bluetooth.sd.attr.retrievable.max",
					"bluetooth.connected.devices.max",
					"bluetooth.l2cap.receiveMTU.max",
					"bluetooth.sd.trans.max",
					"bluetooth.connected.inquiry.scan",
					"bluetooth.connected.page.scan",
					"bluetooth.connected.inquiry",
					"bluetooth.connected.page"
					};
			
			for (String prop: deviceprops){
				
				PrintUtil.out.print( "  " + prop + " = ");
				result = LocalDevice.getProperty(prop);
				PrintUtil.out.println(result);
				
			}
			

			
			for (Field f : BlueCoveLocalDeviceProperties.class.getDeclaredFields()){
			
				if (f.getName().startsWith("LOCAL_DEVICE")){
					PrintUtil.out.print( "  " + f.getName() + " = ");
					result = String.valueOf(LocalDevice.getProperty(String.valueOf(f.get(null))));
					PrintUtil.out.println(result);
				}
			}
			
		}catch(Exception e){
			PrintUtil.out.println("Error getting local device properties " + e.getMessage());
			//e.printStackTrace();
			return;
		}
		
		
		PrintUtil.out.println(" LocalDeviceFeatures");
		try{
			String result = "";
			
			for (Field f : BluetoothStack.class.getDeclaredFields()){
			
				if (f.getName().startsWith("FEATURE")){
					PrintUtil.out.print( "  " + f.getName() + " = ");
					result = String.valueOf(BlueCoveImpl.instance().getLocalDeviceFeature((f.getInt(null))));
					PrintUtil.out.println(result);
				}
			}
			
		}catch(Exception e){
			PrintUtil.out.println("Error getting local device features " + e.getMessage());
			//e.printStackTrace();
			return;
		}
		
		
		//TODO look for more problems using lsusb and command line debug tools
		
		PrintUtil.out.println("\nI don't see anything wrong");
		
	}
	
	
	
	public static String clean(String str){
		
		if (str != null)
			return str.replace("\"", "''")
					.replace("\n", " ");
		else
			return str;
		
	}
	
	
}
