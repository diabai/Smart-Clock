package com.uwt.tcss573;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

@Path("/weather")
public class Weather {

	static int[][] matrix = new int[32][64];
	static ArrayList<String> weather = new ArrayList<>();

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getWeather() throws IOException {
		updateMatrix();
		return "Weather data collected from your location is: " + weather.toString();
	}

	/**
	 * Updates the matrix
	 * 
	 * @throws IOException
	 */
	public static void updateMatrix() throws IOException {

		// Gets the device's public IP address
		String IP = getCurrentIP();
		System.out.println("Your public IP address is : " + IP);
		// Get GPS coordinates
		LookupService cl = new LookupService("GeoLiteCity.dat",
				LookupService.GEOIP_MEMORY_CACHE | LookupService.GEOIP_CHECK_CACHE);

		Location location = cl.getLocation(IP);
		float latitude = location.latitude;
		float longitude = location.longitude;
		String weatherData = getWeather(latitude, longitude);
		weather = extractData(weatherData);
		System.out.println("Data needed: " + weather.toString());
		initializeMatrix();
	}

	/**
	 * Finds the public IP address of this machine.
	 * 
	 * @return the public IP address of this machine.
	 */
	private static String getCurrentIP() {
		// Find public IP address
		String systemipaddress = "";
		try {
			URL url_name = new URL("http://bot.whatismyipaddress.com");

			BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));

			// reads system IPAddress
			systemipaddress = sc.readLine().trim();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return systemipaddress;
	}

	/**
	 * Extract data needed for our calculations from the JSON response.
	 * 
	 * @param weatherData
	 * @return the date, temperature and look from a location
	 */
	private static ArrayList<String> extractData(String weatherData) {

		ArrayList<String> data = new ArrayList<>();
		// Extracting date, temperature and look data from nested JSON array
		JSONObject obj = new JSONObject(weatherData);
		JSONObject query = (JSONObject) obj.get("query");
		JSONObject results = (JSONObject) query.get("results");
		JSONObject channel = (JSONObject) results.get("channel");

		// -----Extracting city & State from Location object
		JSONObject location = (JSONObject) channel.get("location");
		String city = location.getString("city");
		String state = location.getString("region");
		data.add(city);
		data.add(state);
		// -------------
		JSONObject item = (JSONObject) channel.get("item");
		JSONObject condition = (JSONObject) item.get("condition");

		String date = condition.getString("date");
		data.add(date);
		String temperature = condition.getString("temp");
		data.add(temperature);
		String looks = condition.getString("text");
		data.add(looks);
		return data;
	}

	/**
	 * Initializes the 2d matrix array with 0s
	 */
	public static void initializeMatrix() {

		for (int row = 0; row < matrix.length; row++) {
			for (int col = 0; col < matrix[0].length; col++) {
				matrix[row][col] = 0;
			}
		}
		// Prints out the matix
		// for (int[] x : matrix)
		// {
		// for (int y : x)
		// {
		// System.out.print(y + " ");
		// }
		// System.out.println();
		// }
	}

	/**
	 * Makes call to weather API, returns a json response containing weather data.
	 * 
	 * @return JSON object containing weather data.
	 * @throws IOException
	 */
	public static String getWeather(float latitude, float longitude) throws IOException {

		String result = "";
		// Pass dynamic latitude and longitude here.
		URL url = new URL("https://simple-weather.p.mashape.com/weatherdata?lat=" + latitude + "&lng=" + longitude);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		connection.setRequestProperty("X-Mashape-Key", "F0bd5JGnu6mshqDss9oqEfdipbkTp1ZvFrejsnuG7Cgfa4W93v");
		connection.setRequestProperty("Content-type", "application/json");
		connection.setRequestProperty("Accept", "application/json");

		int rspCode = connection.getResponseCode();
		if (rspCode == 200) {
			InputStream ist = connection.getInputStream();
			InputStreamReader isr = new InputStreamReader(ist);
			BufferedReader br = new BufferedReader(isr);

			String nextLine = br.readLine();
			while (nextLine != null) {
				// System.out.println("Line: " + nextLine);
				result += nextLine;
				nextLine = br.readLine();
			}
		}
		return result;
	}
}
