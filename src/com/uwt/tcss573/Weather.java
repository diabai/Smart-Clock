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

	final int CODE_SHOWERS = 11;
	final int CODE_CLOUDY = 26;
	final int CODE_MOSTLY_CLOUDY = 28;
	final int CODE_PARTLY_CLOUDY = 30;
	final int CODE_SUNNY = 32;
	final int CODE_MOSTLY_SUNNY = 34;
	final int CODE_THUNDERSTORM = 4;
	final int CODE_SCATTERED_THUNDERSTORM = 47;
	
	static int[][] matrix = new int[32][64];
	static Color[][] colorMatrix = new Color[32][64];
	static HashMap<Integer, String> codes;

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getWeather() throws IOException {
		generateCodes();
		
		// Create an empty matrix
		initializeMatrix();
		
		// Get Weather data and process it
		String response = getWeather(47.2445343f, -122.43777349999999f);
		ArrayList<String> weather = extractData(response);
		
		// Adding layout to the matrix
		addWeatherIcon(Integer.parseInt(weather[0]));
		addTemperature(weather[1]);
		
		// Convert the matrix to array
		LinkedList<Pixel> responseArray = convertMatrix();
		
		// After this, create a json file and add the responseArray to it as a text.
		// When loop every pixel in the linkedlist, dont forget to add "\n" to indicate
		// each new line.

		String responseText = 
		
		// Maybe dont need json file, just return the json file as a plain text.
		// Then i will convert the plain text to json in the pi.
		return "Weather data collected from your location is: " + mData.toString();
	}

	/**
	 * Add the weather icon to the matrix layout.
	 *
	 */
        private void addWeatherIcon(int theCode) {
		String filename = "";
		if (theCode == 26 || theCode == 28 || theCode == 30) {
			filename += "cloud.csv";
		} else if (theCode == 11) {
			fileame += "rain_shower.csv";
		} else if (theCode == 32 || theCode = 34) {
			filename = "sun.csv";
		} else if (theCode == 4 || theCode = 47) {
			// Will add icon later
			
		} else {
			// Need to add default action
		}
		
		LinkedList<Pixel> layout = readFile(filename);
		addToMatrix(layout, 20, 17);
	}

	/**
	 * Add the temperature to the matrix layout.
	 *
	 */
	private void addTemperature(String theTemperature) {
		// Convert from int to char array
		char[] tempChar = theTemperature.toCharArray();
		
		LinkedList<Pixel> layout = readFile(char[0] + ".csv");	
		addToMatrix(layout, 2, 20);
		
		LinkedList<Pixel> layout = readFile(char[1] + ".csv");	
		addToMatrix(layout, 8, 20);
		
		/* Will add the degree icon later.
		LinkedList<Pixel> layout = readFile("degree.csv");
		addToMatrix(layout, 14, 20);
		*/
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
	public static LinkedList<Pixel> readFile(String filename) {

		BufferedReader br = null;
		FileReader fr = null;
		LinkedList<Pixel> layout = new LinkedList<>();
		
		try {

			// br = new BufferedReader(new FileReader(FILENAME));
			fr = new FileReader(filename);
			br = new BufferedReader(fr);

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				String line[] = sCurrentLine.split(",");
				
				Pixel tempPixel = new Pixel(line[0], line[1], line[2], line[3]m line[4]);
				layout.add(tempPixel);
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
		
		return layout;
	}

	/**
	 * Add the layout to the matrix according to the starting column and row
	 *
	 */
	public static void addToMatrix(LinkedList<Pixel> theLayout, int startCol, int startRow) {
		int row, col, r, g, b = 0;

		for (int i = 0; i < theLayout.size(); i++) {
			row = theLayout.get(i).getRow();
			col = theLayout.get(i).getCol();

			// Not sure it is [col][row] or [row][col]
			matrix[row + startRow][col + startCol] = 1;

			r = theLayout.get(i).getR();
			g = theLayout.get(i).getG();
			b = theLayout.get(i).getB();

			colorMatrix[row + startRow][col + startCol] = new Color(r, g, b);
		}
	}

	/**
	 * Convert the 2D matrix to 1D array.
	 *
	 */
	public LinkedList<Pixel> convertMatrix() {
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

	// Not really needed for now as we will initialize a new empty matrix for every call.
	/*
	public static void clearMatrix(int startCol, int startRow, int width, int height) {
		for (int row = startRow; row < startRow + height; row++) {
			for (int col = startCol; col < startCol + width; col++) {
				matrix[row][col] = 0;
				colorMatrix[row][col] = null;
			}
		}
	}
	*/
	
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
	}

}
