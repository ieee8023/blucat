package com.intel.bluetooth;

import java.io.IOException;
import java.util.Arrays;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;

import org.apache.commons.io.IOUtils;

import blucat.BlucatState;
import blucat.PrintUtil;

public class PairUtil {

	
	public static void pair(String mac) throws Exception{
		
		
		try {
			
			
			
		//	RemoteDevice rem = RemoteDeviceHelper.createRemoteDevice(
			//				null, RemoteDeviceHelper.getAddress(mac),null, false);
			
			
			//Boolean b = BlueCoveImpl.instance().getBluetoothStack().authenticateRemoteDevice(
			//		RemoteDeviceHelper.getAddress(mac));
			
//			Boolean paired = BlueCoveImpl.instance().getBluetoothStack().isRemoteDeviceTrusted(RemoteDeviceHelper.getAddress(mac));
//			
//			if (paired == null)
//				throw new Exception("Your system does not support this");
//			
//			if (paired){
//				
//				// we are already paired.. don't try to pair again
//			}else{
//				
//				
//				
//				
//			}
			
			
			
			
			
			
			
			
			
			// look at cached devices
		//	for(RemoteDevice d : BlueCoveImpl.instance().getBluetoothStack().retrieveDevices(0)){
		//		PrintUtil.out.println(d.getFriendlyName(false));
		//	}
			
			//PrintUtil.out.println("sdasd" + b);//rem.isTrustedDevice());
			
			
			
			
//			PrintUtil.verbose("#ReceiveMTU: " +con.getReceiveMTU() + ", " + 
//					"TransmitMTU: " +con.getTransmitMTU());
//			
//			PrintUtil.verbose("#Status: " +con.ready());
//			
//			
//			byte[] sndbuf = new byte[200];
//			
//			//0x02
//			
//			
//			while(System.in.read(sndbuf) >= 0){
//			
//			final L2CAPConnection con = (L2CAPConnection) Connector.open("btl2cap://" + mac + ":1",Connector.READ_WRITE,true);
//			
//			
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						
//						byte[] buf = new byte[con.getReceiveMTU()];
//						
//						while(!BlucatState.shutdown){
//							int r = con.receive(buf);
//							//PrintUtil.out.println(r);
//							//PrintUtil.out.println(Arrays.toString(buf));
//						}
//						
//					} catch (IOException e) {
//	
//						//PrintUtil.err.println("\n#Error: " + e.getMessage());
//						//if (BlucatState.vverbose) e.printStackTrace();
//					}
//	
//				}
//			}).start();
//			
//			
//			
//			
//				
//				//sndbuf[1] = 0x03;
//				con.send(sndbuf);
//				//PrintUtil.out.println("Sent");
//				con.close();
//			}
//			
//			PrintUtil.out.println(Arrays.toString(sndbuf));
//			
//			
////			con.send(sndbuf);
////			PrintUtil.out.println("Sent");
////			
////	
////			con.send(sndbuf);
////			PrintUtil.out.println("Sent");
//			
//
//			
//			
//			
//			//PrintUtil.verbose("#Status: " +con.ready());
//			
//			
//			
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
}
