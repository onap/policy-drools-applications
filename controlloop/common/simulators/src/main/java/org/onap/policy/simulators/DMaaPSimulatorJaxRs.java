package org.onap.policy.simulators;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/events")
public class DMaaPSimulatorJaxRs {
	
	private static final Map<String, BlockingQueue<String>> queues = new ConcurrentHashMap<String, BlockingQueue<String>>();
	private static final Logger logger = LoggerFactory.getLogger(DMaaPSimulatorJaxRs.class);
	
	@GET
	@Path("/{topicName}/{consumeGroup}/{consumerId}")
	public String subscribeBad(@DefaultValue("0") @QueryParam("timeout") int timeout, @PathParam("topicName") String topicName) {
		if (queues.containsKey(topicName)) {
			BlockingQueue<String> queue = queues.get(topicName);
			String response = "No Data";
			try {
				response = queue.poll(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				logger.debug("error in DMaaP simulator", e);
			}
			if (response == null) {
			    response = "No Data";
			}
			return response;
		}
		else if (timeout > 0) {
			try {
				Thread.sleep(timeout);
				if (queues.containsKey(topicName)) {
					BlockingQueue<String> queue = queues.get(topicName);
					String response = queue.poll();
					if (response == null) {
					    response = "No Data";
					}
					return response;
				}
			} catch (InterruptedException e) {
				logger.debug("error in DMaaP simulator", e);
			}
		}
		return "No topic";
	}
	
	@POST
	@Path("/{topicName}")
	@Consumes(MediaType.TEXT_PLAIN)
	public String publish(@PathParam("topicName") String topicName, String body) { 
		if (queues.containsKey(topicName)) {
			BlockingQueue<String> queue = queues.get(topicName);
			queue.offer(body);
		}
		else {
			BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
			queue.offer(body);
			queues.put(topicName, queue);
		}
		
		return "";
	}
}
