package blucat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;


public class PairUtil {

	public static boolean testpair(String mac){
		
		
		PrintUtil.out.println("#Trying to trigger a pair");
		//for (int i = 1; i <= 30; i++){
			String url = "btl2cap://" + mac + ":" + 3;
			String fullurl = url + ";authenticate=false;encrypt=false";
			
			//PrintUtil.out.println(url);
			
			// fun 
			// /System/Library/CoreServices/BluetoothUIServer.app/Contents/MacOS/BluetoothUIServer -numeric 00-00-00-00-ca-fe 0000
			
			
			BlucatState.rssiexec = Executors.newSingleThreadScheduledExecutor();
			BlucatState.rssiexec.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {

					try {

						PrintUtil.vverbose("#killing pairing windows");
						
						
						// Get the ui out of the way
						Runtime.getRuntime().exec("killall BluetoothUIServer");

						
						
					} catch (Exception e) {
						PrintUtil.verbose("#Unable to end");
					}
				}
			},1, 1, TimeUnit.SECONDS);
			
			
			
			ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
			es.schedule(new Runnable() {
				@Override
				public void run() {

					try {

						PrintUtil.out.println("#Timeout!");
						System.exit(1);

					} catch (Exception e) {
						PrintUtil.verbose("#Unable to end");
					}
				}
			},BlucatState.timeout/1000, TimeUnit.SECONDS);
			
			
			
			
			try{
				
				Connection con = Connector.open(fullurl, Connector.READ_WRITE, true);
				
				PrintUtil.out.println("#Connected");
				con.close();
				
				return true;
			}catch(IllegalArgumentException a){  
				PrintUtil.out.println("#Tell Joe to fix this for your platform");
				
			}catch(Exception e){
				
				//e.printStackTrace();
				String msg = e.getMessage();
				
				if (msg.contains("0x00000004")){
					
					PrintUtil.out.println("#They seem to be out of range");
				
				}else if(msg.contains("0x00000005")){

					PrintUtil.out.println("#A pairing request should have happened");
				
				}else{
					
					PrintUtil.out.println(msg);
				}
				
				
				
				
//				if (!msg.contains("0xe00002cd") && !msg.contains("timeout")){
//				
					
//					//e.printStackTrace();
//				}
				
				//return false;
			}
		//}
		
		return false;
	}
	
	
}
