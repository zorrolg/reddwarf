package com.sun.gi.apps.rawsocket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.gi.framework.rawsocket.SimRawSocketListener;
import com.sun.gi.logic.GLOReference;
import com.sun.gi.logic.SimBoot;
import com.sun.gi.logic.SimTask;
import com.sun.gi.logic.SimTask.ACCESS_TYPE;
import com.sun.gi.logic.impl.GLOReferenceImpl;

/**
 * <p>Title: RawSocketTestBoot</p>
 * 
 * <p>Description: A test harness for the <code>RawSocketManager</code></p>
 * 
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: Sun Microsystems, TMI</p>
 * 
 * @author	Sten Anderson
 * @version 1.0
 */
public class RawSocketTestBoot implements SimBoot {
	
	List<GLOReference> servicerList;
	
	/**
	 * Called multiple times.  On firstBoot == true create all service 
	 * objects (which implement the SocketListener interface) and call createSO() 
	 * and store the references in a list in the boot object.  
	 * 
	 * On subsequent calls when firstBoot == false, don't createSO(), but the 
	 * list of GLOReferences will automatically have the IDs from the database.
	 * 
	 * Connect one socket per service object.
	 * 
	 */
	public void boot(GLOReference bootGLO, boolean firstBoot) {
		SimTask task = SimTask.getCurrent(); 
		if (firstBoot) {
			int numServicers = 5;		

			System.out.println("RawSocketTestBoot: firstBoot");
			//new Throwable().printStackTrace();
			servicerList = new ArrayList<GLOReference>();
			
			for (int i = 0; i < numServicers; i++) {
				GLOReference ssRef = task.createGLO(new SocketServicer(), null);			
				servicerList.add(ssRef);
			}
		}
		
		String host = "localhost";
		//String host = "192.168.0.5";

		System.out.println("RawSocketTestBoot: Socket ID = " + 
				task.openSocket(ACCESS_TYPE.GET, servicerList.get(0), host, 6000, false));
		
		System.out.println("RawSocketTestBoot: Socket ID = " + 
				task.openSocket(ACCESS_TYPE.GET, servicerList.get(1), host, 6000, false));
		
	/*	for (int i = 0; i < servicerList.size(); i++) {
			GLOReference ref = servicerList.get(i);
			System.out.println("RawSocketTestBoot: Socket ID = " + 
					task.openSocket(ACCESS_TYPE.GET, ref, host, getPort(), true));

		}
		*/
	}
	
	private int getPort() {
		return new Random().nextInt(10) + 5000;
	}
	

}
