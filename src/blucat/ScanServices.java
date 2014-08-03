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

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;

/**
 * 
 * @author Joseph Paul Cohen
 *
 */
public class ScanServices {

	
	public static void scanDevice(String server) throws IOException{
		
		

		PrintUtil.out.println("#Scanning RFCOMM Channels 1-30");
		for (int i = 1; i <= 30; i++){
			testUrl(server ,"btspp://", i);
			//testUrl(server ,"btl2cap://", i);
			//testUrl(server ,"btgoep://", i);
		}
		
		
		
		PrintUtil.out.println("#Scanning L2CAP Channels 0-65000");
		for(int i = 0; i < 65000; i++){
			
			try{
				testUrl(server ,"btl2cap://", i);
	
			}catch(Exception e){
				
			}
		}
		
		
		
		
	}
	
	
	
	public static boolean testUrl(String server, String protocol, int channel) throws IOException{
	
		String url = protocol + server + ":" + channel;
		String fullurl = url + ";authenticate=false;encrypt=false";
		try{
			
			Connection con = Connector.open(fullurl, Connector.READ_WRITE, true);
			PrintUtil.out.println(url + " -> Open Channel!!! " + con.getClass().getSimpleName());
			con.close();
			return true;
		}catch(IllegalArgumentException a){  
		
		}catch(Exception e){
			
			String msg = e.getMessage();
			
			if (!msg.contains("0xe00002cd") && !msg.contains("timeout")){
			
				PrintUtil.out.println(url + " -\\> " + msg);
				//e.printStackTrace();
			}
			
			return false;
		}
		
		return true;
	}
	
}
