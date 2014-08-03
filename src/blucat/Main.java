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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.BlueCoveImpl;

/**
 * 
 * @author Joseph Paul Cohen
 *
 */

public class Main {


	static void forceshutdown(){
		
		PrintUtil.verbose("#Shutting down");
		try {
			BlucatState.shutdown = true;
			if (nn(BlucatState.rssiexec)) 	BlucatState.rssiexec.shutdownNow();
			if (nn(BlucatState.pushexec)) 	BlucatState.pushexec.shutdownNow();
			if (nn(BlucatState.is)) 		BlucatState.is.close();
			if (nn(BlucatState.os)) 		BlucatState.os.close();
			if (nn(BlucatState.connection)) BlucatState.connection.close();
			//BlueCoveImpl.shutdown();
			PrintUtil.verbose("#Bye");
		} catch (IOException e) {
			PrintUtil.verbose("#Error when shutting down: " + e.getMessage());
		}
	}
	
	static boolean nn(Object o){
		
		return o!=null?true:false;
	}
	
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args1) throws IOException, InterruptedException {

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				forceshutdown();
			}
			

		}));
		
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_JSR_82_PSM_MINIMUM_OFF,"true");
		
		
		//String[] newargs = {"services"};
		
		//args = newargs;
		
		try{
			
			PrintUtil.disablewrite();
			
			List<String> arg = new LinkedList<String>(Arrays.asList(args1));
			int index = 0;
			
			if (arg.remove("-v"))
				BlucatState.verbose = true;
			
			if (arg.remove("-vv")){
				BlucatState.verbose = true;
				BlucatState.vverbose = true;
			}
			
			if (arg.remove("-k"))
				BlucatState.keepalive = true;
			
			if (arg.remove("-z"))
				BlucatState.zip = true;
			
			if (arg.remove("-csv"))
				BlucatState.csv = true;
			
			if (arg.remove("-rssi"))
				BlucatState.rssi = true;
			
			
			if ((index = arg.indexOf("-b")) != -1){
				
				BlucatState.buffersize = Integer.parseInt(arg.get(index+1));
				arg.remove(index+1);
				arg.remove(index);
				
				PrintUtil.verbose("#Buffer size set to: " +  BlucatState.buffersize);
			}
			
			
			if ((index = arg.indexOf("-t")) != -1){
				
				BlucatState.timeout = Integer.parseInt(arg.get(index+1));
				arg.remove(index+1);
				arg.remove(index);
				
				PrintUtil.verbose("#Timeout set to: " +  BlucatState.timeout + "ms");
			}
			
			if (arg.contains("-e")){
				index = arg.indexOf("-e");
				
				// concat all args together till the end of line
				while (index != arg.size()-1){
					BlucatState.execString += arg.get(index+1) + " ";
					arg.remove(index+1);
				}
				// remove the -e
				arg.remove(index);
			}
				
			
			if (arg.size() == 0)
				printUsage();
				
			
			//all props are set by now
			BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_CONNECT_TIMEOUT,String.valueOf(BlucatState.timeout));
			
			
			
			if ("devices".equalsIgnoreCase(arg.get(0))){
				RemoteDeviceDiscovery.runDiscovery();
				System.exit(0);
			}
			
			if ("doctor".equalsIgnoreCase(arg.get(0))){
				BluCatUtil.doctorDevice();
				System.exit(0);
			}
			
			if ("services".equalsIgnoreCase(arg.get(0))){
				ListServices.listServices();
				System.exit(0);
			}
			
			if (arg.size() == 1 && "-l".equalsIgnoreCase(arg.get(0))){
				BlucatServer.startServerRFCOMM();
				System.exit(0);
			}
			
			if (arg.size() == 2 && "scan".equalsIgnoreCase(arg.get(0))){
				ScanServices.scanDevice(arg.get(1));
				System.exit(0);
			}
			
			if (arg.size() == 2 && "testpair".equalsIgnoreCase(arg.get(0))){
				PairUtil.testpair(arg.get(1));
				System.exit(0);
			}
			
			if (arg.size() == 2 && "-l".equalsIgnoreCase(arg.get(0))){
				BlucatServer.startServerChannel(arg.get(1));
				System.exit(0);
			}
			
			if (arg.size() == 2 && "-l2cap".equalsIgnoreCase(arg.get(0))){
				BlucatState.l2cap = true;
				BlucatServer.startServerChannel(arg.get(1));
				System.exit(0);
			}
			
			if (arg.size() == 2 && "-uuid".equalsIgnoreCase(arg.get(0))){
				BlucatServer.startServerUuid(arg.get(1));
				System.exit(0);
			}
			
			if (arg.size() == 2 && "-url".equalsIgnoreCase(arg.get(0))){
				BlucatClient.startClient(arg.get(1));
				System.exit(0);
			}
			
			
			printUsage();
			
		}catch (BluetoothStateException e){
			
			String msg = e.getMessage();
			
			PrintUtil.err.println("#Error: " + msg);
			
			if (msg.contains("Bluetooth Device is not available"))
				PrintUtil.err.println("#Is there a device plugged in?");
			
			BluCatUtil.doctorDevice();
			
		}catch (Exception e){
			PrintUtil.err.println("#Error: " + e.getMessage());
			if (BlucatState.vverbose) e.printStackTrace();
			forceshutdown();
			System.exit(1);
		}
	}


	private static void printUsage(){
		
		PrintUtil.out.println(
				"blucat - by Joseph Paul Cohen 2012 - josephpcohen.com\n" +
		
						
				"-_-_-_-_-_-_-_,------,\n" +
				"_-_-_-_-_-_-_-|   /\\_/\\\n" +
				"-_-_-_-_-_-_-~|__( ^ .^)\n" +
				"              \"\"  \"\"\n" +
				
				
				
				"Usage:\n" +
				"  blucat devices : Lists devices \n" +
				"  blucat services : Lists all RFCOMM services \n" +
				"  blucat services <device> : List RFCOMM services for one device\n" +
				"  blucat scan <device> : Scan all RFCOMM channels\n" +
				"  blucat -l : Listen for RFCOMM connection \n" +
				"  blucat -l <port> : Listen for RFCOMM connection on port\n" +
				"  blucat -uuid <uuid> : Listen for UUID and attempt RFCOMM\n" +
				"  blucat -l <port> -e <command>: Listen for RFCOMM connection, execute <command> when connection\n" +
				"  blucat <server args> -k : Keep the connection alive\n" +
				"  blucat -url <url> : Connect to RFCOMM URL \n" + 
				"  blucat doctor : Run this if it's not working \n");
		System.exit(0);
	}
}
