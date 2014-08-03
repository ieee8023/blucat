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

import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnectionNotifier;
import com.intel.bluetooth.BluetoothConsts;

/**
 * 
 * @author Joseph Paul Cohen
 *
 */
public class BlucatServer {

	public static void startServerRFCOMM() throws IOException{
		
		String url = "btspp://localhost:" + BluetoothConsts.RFCOMM_PROTOCOL_UUID + 
				";name=BlueCatPipe;authenticate=false;encrypt=false;master=true";
		
		PrintUtil.verbose("#" + "Creating RFCOMM server");
		
        StreamConnectionNotifier service = (StreamConnectionNotifier) Connector.open(url);

        ServiceRecord rec = LocalDevice.getLocalDevice().getRecord(service);
        
        //PrintUtil.out.println(rec.toString());
        String remoteUrl = rec.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
        remoteUrl = remoteUrl.substring(0, remoteUrl.indexOf(";"));

        PrintUtil.verbose("#" + new Date() + " - Listening at " + remoteUrl);
        
        BlucatConnection.handle(service);
	}
	
	public static void startServerUuid(String uuidValue) throws IOException{
		
		uuidValue = uuidValue.replace("-", "");
		
		UUID uuid = new UUID(uuidValue, false);
		
		String url = "btspp://localhost:" + uuid.toString() + 
				";name=BlueCatPipe"; //;authenticate=false;authorize=false;encrypt=false;master=false";
		
		PrintUtil.verbose("Creating server with UUID " + uuid);
	
        StreamConnectionNotifier service = (StreamConnectionNotifier) Connector.open(url);

        ServiceRecord rec = LocalDevice.getLocalDevice().getRecord(service);
        
        //PrintUtil.out.println(rec.toString());
        String remoteUrl = rec.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
        remoteUrl = remoteUrl.substring(0, remoteUrl.indexOf(";"));

        PrintUtil.verbose("#" + new Date() + " - Listening at " + remoteUrl);
        
        BlucatConnection.handle(service);

	}
	
	public static void startServerChannel(String port) throws IOException{
		
		String url = null;
		
		
		if (BlucatState.l2cap){
			url = "btl2cap://localhost:" + port + ";name=BlueCatL2CAP";
		}else{
			url = "btspp://localhost:" + port + ";name=BlueCatRFCOM";
		}
		
		PrintUtil.verbose("#" + "Creating server on channel " + port);
		
        Connection service = Connector.open(url);

        ServiceRecord rec = LocalDevice.getLocalDevice().getRecord(service);
        
        //PrintUtil.out.println(rec.toString());
        String remoteUrl = rec.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
        remoteUrl = remoteUrl.substring(0, remoteUrl.indexOf(";"));

        PrintUtil.verbose("#" + new Date() + " - Listening at " + remoteUrl);
        
        BlucatConnection.handle(service);
	}
	
	
	
	
	
	
	

}
