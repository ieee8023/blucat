package blucat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;
import com.intel.bluetooth.BluetoothServerConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import com.intel.bluetooth.RemoteDeviceHelper;
import com.intel.bluetooth.obex.BlueCoveOBEX;
import compression.CompressedBlockInputStream;
import compression.CompressedBlockOutputStream;

public class BlucatConnection {


	public static void handle(Connection connection) throws IOException {
		
		if (connection instanceof StreamConnectionNotifier){
	    	
			BlucatState.serverConnection = connection;
			do{
				BlucatState.connection = ((StreamConnectionNotifier)connection).acceptAndOpen();
		    	BlucatConnection.handle(BlucatState.connection);
			}while (BlucatState.keepalive);	
	    	
	    }else if (connection instanceof L2CAPConnectionNotifier){
	    
	    	BlucatState.serverConnection = connection;
	    	do{
	    		BlucatState.connection = ((L2CAPConnectionNotifier)connection).acceptAndOpen();
		    	BlucatConnection.handle(BlucatState.connection);
	    	}while (BlucatState.keepalive);	
		
	    }else{		
		
			if (BlucatState.vverbose) {
				try{
					final RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
					BlucatState.rssiexec = Executors.newSingleThreadScheduledExecutor();
					BlucatState.rssiexec.scheduleAtFixedRate(new Runnable() {
						@Override
						public void run() {
	
							try {
	
								PrintUtil.err.println("#RSSI:" + RemoteDeviceHelper.readRSSI(dev));
							} catch (Exception e) {
								PrintUtil.verbose("#Unable to read RSSI");
							}
	
						}
					}, 0, 1, TimeUnit.SECONDS);
				}catch(Exception e){
					
					PrintUtil.verbose("#Strange Error: " + e.getMessage());
					e.printStackTrace();
				}
			}
			
			handleSingle(connection);
			
			if (BlucatState.rssiexec != null)
				BlucatState.rssiexec.shutdownNow();
	    }
	}
	
	
	
	public static void handleSingle(Connection connection) throws IOException{
	
		// mark for shut down at the end
		BlucatState.connection = connection;
		
		if (connection instanceof StreamConnection) {
			StreamConnection con = (StreamConnection) connection;

			PrintUtil.verbose("#" + new Date() + " Connected");

			BlucatState.is = con.openDataInputStream();
			BlucatState.os = con.openDataOutputStream();

			// BlucatState.is = new BufferedInputStream(BlucatState.is);
			// BlucatState.os = new BufferedOutputStream(BlucatState.os);

			if (BlucatState.zip) {

				PrintUtil.verbose("#" + "Zip Mode");
				BlucatState.os = new CompressedBlockOutputStream(BlucatState.os, BlucatState.buffersize);
				BlucatState.is = new CompressedBlockInputStream(BlucatState.is);
			}

			new BlucatStreams(BlucatState.is, BlucatState.os);

			
			
			

		} else if (connection instanceof L2CAPConnection) {
			final L2CAPConnection con = (L2CAPConnection) connection;

			PrintUtil.verbose("#ReceiveMTU: " + con.getReceiveMTU() + ", "
					+ "TransmitMTU: " + con.getTransmitMTU());

			PrintUtil.verbose("#Status: " + con.ready());
			
			BlucatState.is = new InputStream() {
				
//				byte[] buf = new byte[con.getReceiveMTU()];
//				int index = -1;
//				int buflength = 0;
				
				@Override
				public int read() throws IOException {
					
//					index++;
//					if (index == buflength){
//						buflength = con.receive(buf);
//						index = 0;
//					}
//					
//					return buf[index];
					
					byte[] buf = new byte[1];
					
					con.receive(buf);
					
					return new Integer(buf[0]).intValue();
					
				}
			};
					
			BlucatState.os = new OutputStream() {
				
				@Override
				public void write(int arg0) throws IOException {
					
					//TODO make this better 
					
					byte[] buf = new byte[1];
					
					buf[0] = new Integer(arg0).byteValue();
					
					con.send(buf);
				}
			};
			
			new BlucatStreams(BlucatState.is, BlucatState.os);
			
			
			
			
//			byte[] buf = new byte[con.getReceiveMTU()];
//			int r = con.receive(buf);
//			PrintUtil.out.println(r);
//			PrintUtil.out.println(Arrays.toString(buf));

			
			
			
			
			
		} else if (connection instanceof ClientSession) {
			ClientSession con = (ClientSession) connection;

			HeaderSet hsConnectReply = con.connect(null);
			if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
				PrintUtil.out.println("Failed to receive OBEX_HTTP_OK");
			}
			PrintUtil.verbose("#" + new Date() + " Connected");

			PrintUtil.verbose("# MTU selected "
					+ BlueCoveOBEX.getPacketSize(con));

			PrintUtil.err.println("    This part isn't done yet : (");
			PrintUtil.err
					.println("    There is some code here so check it out!");

			byte data[] = "Hello world!".getBytes("iso-8859-1");

			HeaderSet hsOperation = con.createHeaderSet();
			hsOperation.setHeader(HeaderSet.NAME, "Hey There!");
			hsOperation.setHeader(HeaderSet.TYPE, "text");
			hsOperation.setHeader(HeaderSet.LENGTH, new Long(data.length));

			// Create PUT Operation
			Operation putOperation = con.put(hsOperation);

			// Send some text to server

			BlucatState.os = putOperation.openOutputStream();
			BlucatState.os.write(data);
			BlucatState.os.close();

			BlucatState.is = putOperation.openInputStream();

			try {
				byte[] buffer = new byte[BlucatState.buffersize]; // Adjust if
																	// you want
				int bytesRead;
				while ((bytesRead = BlucatState.is.read(buffer)) != -1) {

					PrintUtil.out.write(buffer, 0, bytesRead);
					PrintUtil.out.flush();
				}
			} catch (IOException e) {

				PrintUtil.err.println("\nError: " + e.getMessage());
				if (BlucatState.vverbose)
					e.printStackTrace();
			}

			// //////to output status

			Map<Integer, String> statusCodes = new HashMap<Integer, String>();
			for (Field f : ResponseCodes.class.getDeclaredFields()) {
				try {
					statusCodes.put(f.getInt(null), f.getName());
				} catch (Exception e) {
				}
			}

			String statusCode = statusCodes.get(putOperation.getResponseCode());

			if (statusCode == null)
				statusCode = "Unknown Code: " + putOperation.getResponseCode();

			PrintUtil.err.println("#Operation Result Status: " + statusCode);

			putOperation.close();

			con.disconnect(null);

	
			
			
		} else {
			PrintUtil.err
					.println("This type of connection is not implemented yet: "
							+ connection.getClass().getSimpleName()
							+ " implementing "
							+ Arrays.toString(connection.getClass()
									.getInterfaces()));
		}
		
		
		

		PrintUtil.verbose("#" + new Date() + " Connection Closed");

	}
}
