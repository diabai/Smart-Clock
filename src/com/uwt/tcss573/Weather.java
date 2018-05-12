package com.uwt.tcss573;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/weather")
public class Weather {

	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getWeather() {
		return "Your test has successfully been completed";
	}
}
