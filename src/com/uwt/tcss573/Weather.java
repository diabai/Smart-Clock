package com.uwt.tcss573;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import java.awt.Color;

import org.json.JSONObject;

import com.google.gson.Gson;

/**
 * 
 * @author ikdiabate
 *
 */
@Path("/weather")
public class Weather {

	final int CODE_SHOWERS = 11;
	final int CODE_CLOUDY = 26;
	final int CODE_MOSTLY_CLOUDY = 28;
	final int CODE_PARTLY_CLOUDY = 30;
	final int CODE_SUNNY = 32;
	final int CODE_MOSTLY_SUNNY = 34;
	final int CODE_THUNDERSTORM = 4;
	final int CODE_SCATTERED_THUNDERSTORM = 47;
	static ArrayList<String> settings = new ArrayList<>();

	/**
	 * Default constructor.
	 */
	public Weather() {
		settings.add("val1");
		settings.add("val2");
		settings.add("val3");
		settings.add("val4");
		settings.add("val5");
	}

	static Color[][] colorMatrix = new Color[32][64];

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getWeather1(@QueryParam("lat") float latitude, @QueryParam("lng") float longitude)
			throws IOException {
		// Create and initialize empty matrix
		int[][] matrix = new int[32][64];
		initializeMatrix(matrix);

		// Get Weather data and process it
		String response = getWeather(latitude, longitude);
		ArrayList<String> weather = extractData(response);

		// Adding layout to the matrix
		int weatherCode = Integer.parseInt(weather.get(0));
		addWeatherIcon(weatherCode, matrix);
		addTemperature(weather.get(1), matrix);

		// Convert the matrix to array
		LinkedList<Pixel> pixelArray = convertMatrix(matrix);

		// add "\n" to indicate
		String responseText = "";

		for (int i = 0; i < pixelArray.size(); i++) {
			responseText += pixelArray.get(i).toString() + "\n";
		}
		// Need to arrange into JSON String here.
		return responseText;
	}

	
	@POST
	@Path("/storesettings")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String storeSettings(String msg) throws IOException, SQLException, ClassNotFoundException {

		settings.add(msg);
		return "Settings <"+msg+"> was successfully added to our database.";
	}
	
	@GET
	@Path("/getsettings")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSettings() throws IOException, SQLException, ClassNotFoundException {
		String result = "";
		for (int i = 0; i < settings.size(); i++) {
			result+= settings.get(i) + " "; 
		}
		return result;
	}
	

	
	/**
	 * Add the weather icon to the matrix layout.
	 * 
	 * @throws IOException
	 *
	 */
	private void addWeatherIcon(int theCode, int[][] matrix) throws IOException {
		String filename = "";
		if (theCode == 26 || theCode == 28 || theCode == 30) {
			filename += "cloud.csv";
		} else if (theCode == 11 || theCode == 12) {
			filename += "rain_shower.csv";
		} else if (theCode == 32 || theCode == 34) {
			filename = "sun.csv";
		} else if (theCode == 4 || theCode == 47) {
			// Will add icon later --> (Not really necessary, there is never such weather in
			// our state)

		} else {
			// Need to add default action
			filename += "rain_shower.csv"; // Could change this if necessary
		}

		LinkedList<Pixel> layout = readFile(filename);
		addToMatrix(layout, matrix, 20, 17);
	}

	/**
	 * Add the temperature to the matrix layout.
	 * 
	 * @throws IOException
	 *
	 */
	private void addTemperature(String theTemperature, int[][] matrix) throws IOException {
		// Convert the temperature to char array (e.g. from 12 to [1,2])
		char[] tempChar = theTemperature.toCharArray();

		LinkedList<Pixel> layout = readFile(tempChar[0] + ".csv");
		addToMatrix(layout, matrix, 2, 20);

		LinkedList<Pixel> mLayout = readFile(tempChar[1] + ".csv");
		addToMatrix(mLayout, matrix, 8, 20);
	}

	/**
	 * Add the layout to the matrix according to the starting column and row
	 *
	 */
	public void addToMatrix(LinkedList<Pixel> theLayout, int[][] matrix, int startCol, int startRow) {
		int row, col, r, g, b = 0;

		for (int i = 0; i < theLayout.size(); i++) {
			row = theLayout.get(i).getRow();
			col = theLayout.get(i).getCol();
			System.out.println("row = " + row + " col = " + col);
			// Not sure it is [col][row] or [row][col]
			matrix[row + startRow][col + startCol] = 1;

			r = theLayout.get(i).getR();
			g = theLayout.get(i).getG();
			b = theLayout.get(i).getB();

			colorMatrix[row + startRow][col + startCol] = new Color(r, g, b);
		}
	}

	/**
	 * 
	 * Convert the 2D matrix to 1D array (the LinkedList of Pixel).
	 */
	public LinkedList<Pixel> convertMatrix(int[][] matrix) {
		// Use For Loop, if value is 1 then store its position, col is x and row is y
		// value.
		LinkedList<Pixel> pixelArray = new LinkedList<>();

		for (int row = 0; row < 32; row++) {
			for (int col = 0; col < 64; col++) {
				if (matrix[row][col] == 1) {
					Color tempColor = colorMatrix[row][col];
					Pixel tempPixel = new Pixel(col, row, tempColor.getRed(), tempColor.getGreen(),
							tempColor.getBlue());
					pixelArray.add(tempPixel);
				}
			}
		}
		return pixelArray;
	}

	// Not really needed for now as we will initialize a new empty matrix for every
	// call.
	/*
	 * public static void clearMatrix(int startCol, int startRow, int width, int
	 * height) { for (int row = startRow; row < startRow + height; row++) { for (int
	 * col = startCol; col < startCol + width; col++) { matrix[row][col] = 0;
	 * colorMatrix[row][col] = null; } } }
	 */

	/**
	 * Initializes the matrix.
	 */
	public void initializeMatrix(int[][] matrix) {

		for (int row = 0; row < matrix.length; row++) {
			for (int col = 0; col < matrix[0].length; col++) {
				matrix[row][col] = 0;
			}
		}
	}

	/**
	 * Makes call to weather API, returns a json response containing weather data.
	 * 
	 * @return JSON object containing weather data.
	 * @throws IOException
	 */
	public String getWeather(float latitude, float longitude) throws IOException {

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
	 * Extracts data needed for our calculations from the JSON response.
	 * 
	 * @param weatherData
	 *            the entire raw JSON response returned from API call.
	 * @return an ArrayList containing the code and temperature of a location.
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
	 * Displays the contents of the matrix.
	 */
	public void displayMatrix(int[][] matrix) {
		// Prints out the matix
		for (int[] x : matrix) {
			for (int y : x) {
				System.out.print(y + " ");
			}
			System.out.println();
		}
	}

	/**
	 * Reads a file and stores its content into a Pixel object (x and y coordinates,
	 * and rgb values)
	 * 
	 * @param filename
	 *            the file to read
	 * @return a LinkedList of Pixel objects.
	 * @throws IOException
	 */
	public LinkedList<Pixel> readFile(String filename) throws IOException {

		BufferedReader br = new BufferedReader(
				new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(filename)));

		LinkedList<Pixel> layout = new LinkedList<>();

		String sCurrentLine = "";

		while ((sCurrentLine = br.readLine()) != null) {
			String line[] = sCurrentLine.split(",");

			Pixel tempPixel = new Pixel(Integer.parseInt(line[0]), Integer.parseInt(line[1]), Integer.parseInt(line[2]),
					Integer.parseInt(line[3]), Integer.parseInt(line[4]));
			layout.add(tempPixel);
		}

		if (br != null) {
			br.close();
		}
		return layout;
	}

	/**
	 * Class to represent and store information about each matrix pixel such as its
	 * position and color.
	 * 
	 * @author Ming Hoi & Ibrahim Diabate
	 *
	 */
	class Pixel {
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

		@Override
		public String toString() {

			return col + "," + row + "," + r + "," + g + "," + b;
		}
	}

}
