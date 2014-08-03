package blucat;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;


public class PrintUtil {


	public static PrintStream out = System.out;
	public static PrintStream err = System.err;
	
	public static void verbose(String str){
		if(BlucatState.verbose)
			err.println(str);
	}
	
	public static void vverbose(String str){
		if(BlucatState.vverbose)
			err.print(str);
	}
	
	public static void enablewrite(){
		
		System.setOut(out);
	}
	
	public static void disablewrite(){
		
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {}
		}));
	}

}
