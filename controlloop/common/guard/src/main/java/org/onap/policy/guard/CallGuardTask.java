package org.onap.policy.guard;

import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.pdp.PDPEngine;
import com.att.research.xacml.std.annotations.RequestParser;
import java.util.UUID;

import org.drools.core.WorkingMemory;


public class CallGuardTask implements Runnable {
	
	WorkingMemory workingMemory;
	PDPEngine embeddedPdpEngine;
	String restfulPdpUrl;
	String clname;
	String actor;
	String recipe;
	String target;
	String requestId;
	
    public CallGuardTask(PDPEngine engine, String url, WorkingMemory wm, String cl, String act, String rec, String tar, String reqId) { 
    
    	embeddedPdpEngine = engine; 
    	restfulPdpUrl = url;
    	workingMemory = wm;
    	clname = cl;
    	actor = act;
    	recipe = rec;
    	requestId = reqId;
    	target = tar;
    }
    public void run() {
    	long startTime = System.nanoTime();
    	com.att.research.xacml.api.Request request = null;
    	
    	PolicyGuardXacmlRequestAttributes xacmlReq = new PolicyGuardXacmlRequestAttributes(clname, actor,  recipe, target, requestId);
    	
    	try {
    		request = RequestParser.parseRequest(xacmlReq);
		} catch (IllegalArgumentException | IllegalAccessException | DataTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	
		
  		System.out.println("\n********** XACML REQUEST START ********");
		System.out.println(request);
		System.out.println("********** XACML REQUEST END ********\n");
		
		com.att.research.xacml.api.Response xacmlResponse = PolicyGuardXacmlHelper.callPDP(embeddedPdpEngine, "", request, false);
		
		System.out.println("\n********** XACML RESPONSE START ********");
		System.out.println(xacmlResponse);
		System.out.println("********** XACML RESPONSE END ********\n");
						
		PolicyGuardResponse guardResponse = PolicyGuardXacmlHelper.ParseXacmlPdpResponse(xacmlResponse);
		
		//
		//Create an artificial Guard response in case we didn't get a clear Permit or Deny
		//
		if(guardResponse.result.equals("Indeterminate")){
			guardResponse.operation = recipe;
			guardResponse.requestID = UUID.fromString(requestId);
		}
		
		long estimatedTime = System.nanoTime() - startTime;
		System.out.println("\n\n============ Guard inserted with decision "+ guardResponse.result + " !!! =========== time took: " +(double)estimatedTime/1000/1000 +" mili sec \n\n");
		workingMemory.insert(guardResponse);

    }

}
