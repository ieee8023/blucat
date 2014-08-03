package blucat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.bluetooth.RemoteDevice;

import org.apache.commons.io.IOUtils;

import com.intel.bluetooth.RemoteDeviceHelper;

public class BlucatStreams {

	
	public BlucatStreams(final InputStream is, final OutputStream os) throws IOException {

		
//		if (BlucatState.push) {
//			try{
//				BlucatState.pushexec = Executors.newSingleThreadScheduledExecutor();
//				BlucatState.pushexec.scheduleAtFixedRate(new Runnable() {
//					@Override
//					public void run() {
//
//						try {
//
//							PrintUtil.vverbose("#Pushing buffers");
//							os.flush();
//							PrintUtil.out.flush();
//							
//						} catch (Exception e) {
//							PrintUtil.verbose("#Unable to push buffers");
//						}
//
//					}
//				}, 0, 1, TimeUnit.SECONDS);
//			}catch(Exception e){
//				
//				PrintUtil.verbose("#Strange Error: " + e.getMessage());
//				e.printStackTrace();
//			}
//		}
		
		
		
		
		if (BlucatState.execString == "")
			onConsole(is,os);
		else
			onProcess(is,os);
		
	}

	private void onConsole(final InputStream is, final OutputStream os) {
		

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					IOUtils.copy(is, PrintUtil.out);
				} catch (IOException e) {

					PrintUtil.err.println("\n#Error: " + e.getMessage());
					if (BlucatState.vverbose)
						e.printStackTrace();
				}
			}
		}).start();

		try {

			IOUtils.copy(System.in, os);
		} catch (IOException e) {

			PrintUtil.err.println("\n#Error: " + e.getMessage());
			if (BlucatState.vverbose)
				e.printStackTrace();
		}
		
	}
	
	
	
	private void onProcess(final InputStream is, final OutputStream os) throws IOException {
		
		PrintUtil.verbose("#" + new Date() + " - Process Redirect Connection: " + BlucatState.execString);
		
		final Process p = Runtime.getRuntime().exec(BlucatState.execString);
		
		final OutputStream pos = p.getOutputStream();
		final InputStream pis = p.getInputStream();
		final InputStream err = p.getErrorStream();
		
		final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {

				try {

					os.flush();
					pos.flush();
					os.write("".getBytes());
					PrintUtil.vverbose("#flushing\n");
					
				} catch (IOException e) {

					p.destroy();
					
					PrintUtil.verbose(e.getMessage());
					exec.shutdownNow();
				}

			}
		}, 0, 1, TimeUnit.SECONDS);
		
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					IOUtils.copy(is, pos);
				} catch (IOException e) {

					PrintUtil.err.println("\n#Error: " + e.getMessage());
					if (BlucatState.vverbose)
						e.printStackTrace();
				}
			}
		}).start();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					IOUtils.copy(err, os);
				} catch (IOException e) {

					PrintUtil.err.println("\n#Error: " + e.getMessage());
					if (BlucatState.vverbose)
						e.printStackTrace();
				}
			}
		}).start();

		

		try {

			IOUtils.copy(pis, os);
		} catch (IOException e) {

			PrintUtil.err.println("\n#Error: " + e.getMessage());
			if (BlucatState.vverbose)
				e.printStackTrace();
		}

		
		p.destroy();
		exec.shutdownNow();
		
		if (BlucatState.vverbose){
			
			try{
				PrintUtil.vverbose("Process return value: " + p.exitValue() + "\n");
			}catch(Exception e){
				PrintUtil.vverbose("No process return value\n");
			}
		}	
	}
}
