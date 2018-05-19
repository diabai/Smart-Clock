package com.uwt.tcss573;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.awt.Color;

import org.json.JSONObject;

/**
 * 
 * @author ikdiabate
 *
 */
@Path("/weather")
public class Weather {

	static int[][] matrix = new int[32][64];
	static Color[][] colorMatrix = new Color[32][64];
	static HashMap<Integer, String> codes;

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getWeather() throws IOException {
		generateCodes();
		ArrayList<String> mData = updateMatrix();
		return "Weather data collected from your location is: " + mData.toString();
	}

	/**
	 * Stores weather codes in a hashmap to be retrieved later.
	 */
	public void generateCodes() {
		codes = new HashMap<>();

		codes.put(11, "Showers");
		codes.put(26, "Cloudy");
		codes.put(28, "Mostly Cloudy");
		codes.put(30, "Partly Cloudy");
		codes.put(32, "Sunny");
		codes.put(34, "Mostly Sunny");
		codes.put(4, "Thunderstorm");
		codes.put(47, "Scattered Thunderstorm");
	}

	/**
	 * Updates the matrix
	 * 
	 * @throws IOException
	 */
	public ArrayList<String> updateMatrix() throws IOException {

		// Gets the device's public IP address
		// String IP = getCurrentIP();
		// System.out.println("Your public IP address is : " + IP);
		// // Get GPS coordinates
		// LookupService cl = new LookupService("GeoLiteCity.dat",
		// LookupService.GEOIP_MEMORY_CACHE | LookupService.GEOIP_CHECK_CACHE);
		//
		// Location location = cl.getLocation(IP);
		// float latitude = location.latitude;
		// float longitude = location.longitude;
		String weatherData = getWeather(47.2445343f, -122.43777349999999f);
		ArrayList<String> weather = extractData(weatherData);
		return weather;
		// System.out.println("Data needed: " + weather.toString());
		// initializeMatrix();
	}

	/**
	 * Finds the public IP address of this machine.
	 * 
	 * @return the public IP address of this machine.
	 */
	private String getCurrentIP() {
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
	private ArrayList<String> extractData(String weatherData) {

		ArrayList<String> data = new ArrayList<>();
		// Extracting date, temperature and look data from nested JSON array
		JSONObject obj = new JSONObject(weatherData);
		JSONObject query = (JSONObject) obj.get("query");
		JSONObject results = (JSONObject) query.get("results");
		JSONObject channel = (JSONObject) results.get("channel");

		// ------------- Extract the weather condition of the location
		JSONObject item = (JSONObject) channel.get("item");
		JSONObject condition = (JSONObject) item.get("condition");

		String weatherCode = condition.getString("code");
		data.add(weatherCode);

		String temperature = condition.getString("temp");
		data.add(temperature);

		return data;
	}

	/**
	 * Initializes the matrix.
	 */
	public static void initializeMatrix() {

		for (int row = 0; row < matrix.length; row++) {
			for (int col = 0; col < matrix[0].length; col++) {
				matrix[row][col] = 0;
			}
		}
	}

	/**
	 * Displays the contents of the matrix.
	 */
	public static void displayMatrix() {
		// Prints out the matix
		for (int[] x : matrix) {
			for (int y : x) {
				System.out.print(y + " ");
			}
			System.out.println();
		}
	}

	/**
	 * Reads a file and displays its contents
	 *
	 */
	public static void readFile(String filename) {

		BufferedReader br = null;
		FileReader fr = null;

		try {

			// br = new BufferedReader(new FileReader(FILENAME));
			fr = new FileReader(filename);
			br = new BufferedReader(fr);

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				System.out.println(sCurrentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			try {
				if (br != null)
					br.close();

				if (fr != null)
					fr.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	// todo:
	public static LinkedList<Pixel> convertMatrix(int[][] theMatrix) {

		// Use For Loop, if value is 1 then store its position, col is x and row is y
		// value.

		Color tempColor = colorMatrix[col][row];
		Pixel tempPixel = new Pixel(col, row, tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue());

	}

	public static int[][] clearMatrix(int[][] theMatrix, int startCol, int startRow, int width, int height) {

	}

	public static int[][] addToMatrix(int[][] theMatrix, LinkedList<Pixel> theLayout, int startCol, int startRow) {
		for (int i; i < theLayout.size; i++) {
    	col = theLayout.get(i).getCol();
      row = theLayout.get(i).getRow();
      r = theLayout.get(i).getR();
      g = theLayout.get(i).getG();
      b = theLayout.get(i).getB();
      
      theMatrix
    }
  
  
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

	/**
	 * Class to represent and store information about each matrix pixel such as its
	 * position and color.
	 * 
	 * @author Ming Hoi & Ibrahim Diabate
	 *
	 */
	private class Pixel {
		private int col;
		private int row;
		private int r;
		private int g;
		private int b;

		// Consctructor
		public Pixel(int col, int row, int r, int g, int b) {
			this.col = col;
			this.row = row;
			this.r = r;
			this.g = g;
			this.b = b;
		}

		/**
		 * Defining getters
		 *
		 **/
		public int getCol() {
			return col;
		}

		public int getRow() {
			return row;
		}

		public int getR() {
			return r;
		}

		public int getG() {
			return g;
		}

		public int getB() {
			return b;
		}
	}

}
