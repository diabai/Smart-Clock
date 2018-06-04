package com.uwt.tcss573;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.awt.Color;
import org.json.JSONObject;
import io.minio.MinioClient;

/**
 * 
 * @author Ibrahim Diabate and Ming Hoi Lam
 * @version Spring 2018.
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

	static Color[][] colorMatrix = new Color[32][64];

	/**
	 * Makes GET request to Weather API.
	 * 
	 * @param latitude
	 *            the latitude
	 * @param longitude
	 *            the longitude
	 * @param temp
	 *            the temperature
	 * @param hum
	 *            the humidity
	 * @return the temperature and feel of a location
	 * @throws IOException
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getWeather1(@QueryParam("lat") float latitude, @QueryParam("lng") float longitude,
			@QueryParam("temp") int temp, @QueryParam("hum") int hum) throws IOException {
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

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
		Date now = new Date();
		String dateString = dateFormat.format(now);
		System.out.println(dateString);
		dateFormat = new SimpleDateFormat("HH");
		String timeString = dateFormat.format(now);
		System.out.println(timeString);
		String weatherString = dateString + "," + weather.get(1);
		String tempString = dateString + "," + timeString + "," + temp + "," + hum;

		appendData("weather.csv", weatherString);
		appendData("tempHum.csv", tempString);

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

	/**
	 * Makes POST request to S3 bucket to store settings.
	 * 
	 * @param msg
	 * @return an acknowledgment message upon successful POST request.
	 * @throws IOException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	@POST
	@Path("/storesettings")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String storeSettings(String msg) throws IOException, SQLException, ClassNotFoundException {

		MinioClient minioClient;
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			String file = minioClient.getObject("smart-clock-settings", "settings.txt").toString();
			if (!file.equals(msg)) {
				// Since cannot modify s3 object, so remove the file and create a new one
				minioClient.removeObject("smart-clock-settings", "settings.txt");

				// Store settings into string
				String settingsString = msg;

				// below dont need to modify anything. This code simply create a file in s3
				// bucket.
				ByteArrayInputStream bais = new ByteArrayInputStream(settingsString.getBytes("UTF-8"));
				minioClient.putObject("smart-clock-settings", "settings.txt", bais, bais.available(),
						"application/octet-stream");
				bais.close();
				////////
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "Settings successfully stored in DB.";

	}

	/**
	 * Makes GET request to S3 bucket to retrieve stored settings
	 * 
	 * @return the settings stored in the S3 bucket.
	 */
	@GET
	@Path("/getsettings")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSettings() {
		MinioClient minioClient;
		String settingsString = "";
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			// Retrieve string from S3.
			InputStream stream = minioClient.getObject("smart-clock-settings", "settings.txt");
			byte[] buf = new byte[16384];
			int bytesRead;
			while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
				settingsString += new String(buf, 0, bytesRead);
			}
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return settingsString;
	}

	@GET
	@Path("/getdatarange")
	@Produces(MediaType.TEXT_PLAIN)
	public String getDataRange(@QueryParam("type") String type) {
		String filename;
		String responseString = "";

		if (type.equalsIgnoreCase("weather")) {
			filename = "weather.csv";
		} else {
			filename = "tempHum.csv";
		}
		MinioClient minioClient;
		String rangeString = "";
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			// Retrieve string from S3.
			InputStream stream = minioClient.getObject("smart-clock-settings", filename);
			byte[] buf = new byte[16384];
			int bytesRead;
			while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
				rangeString += new String(buf, 0, bytesRead);
			}
			stream.close();

			String[] rangeLines = rangeString.split("\\r?\\n");
			for (int i = 1; i < rangeLines.length; i++) {
				String[] range = rangeLines[i].split(",");

				if (type.equalsIgnoreCase("weather")) {
					responseString += range[0] + "\n";
				} else {
					responseString += range[0] + "," + range[1] + "\n";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return responseString;
	}

	@GET
	@Path("/getweatherrecord")
	@Produces(MediaType.TEXT_PLAIN)
	public String getWeatherRecord(@QueryParam("start_date") String startDate, @QueryParam("end_date") String endDate) {
		String filename = "weather.csv";

		MinioClient minioClient;
		String weatherString = "";
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			// Retrieve string from S3.
			InputStream stream = minioClient.getObject("smart-clock-settings", filename);
			byte[] buf = new byte[16384];
			int bytesRead;
			while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
				weatherString += new String(buf, 0, bytesRead);
			}
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] weatherLines = weatherString.split("\\r?\\n");
		int startIndex = 1, endIndex = weatherLines.length;
		for (int i = 1; i < weatherLines.length; i++) {
			String[] keyValue = weatherLines[i].split(",");
			if (keyValue[0].equalsIgnoreCase(startDate)) {
				startIndex = i;
			}

			if (keyValue[0].equalsIgnoreCase(endDate)) {
				endIndex = i;
			}
		}

		String responseString = weatherLines[0] + "\n";

		for (int i = startIndex; i <= endIndex; i++) {
			responseString += weatherLines[i] + "\n";
		}

		return responseString;
	}

	@GET
	@Path("/gettemprecord")
	@Produces(MediaType.TEXT_PLAIN)
	public String getTempHumRecord(@QueryParam("start_date") String startDate, @QueryParam("end_date") String endDate,
			@QueryParam("start_hour") String startHour, @QueryParam("end_hour") String endHour) {
		String filename = "tempHum.csv";

		MinioClient minioClient;
		String tempString = "";
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			// Retrieve string from S3.
			InputStream stream = minioClient.getObject("smart-clock-settings", filename);
			byte[] buf = new byte[16384];
			int bytesRead;
			while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
				tempString += new String(buf, 0, bytesRead);
			}
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] tempLines = tempString.split("\\r?\\n");
		int startIndex = 1, endIndex = tempLines.length;
		for (int i = 1; i < tempLines.length; i++) {
			String[] keyValue = tempLines[i].split(",");
			if (keyValue[0].equalsIgnoreCase(startDate) && keyValue[1].equalsIgnoreCase(startHour)) {
				startIndex = i;
			}

			if (keyValue[0].equalsIgnoreCase(endDate) && keyValue[1].equalsIgnoreCase(endHour)) {
				endIndex = i;
			}
		}

		String responseString = tempLines[0] + "\n";

		for (int i = startIndex; i <= endIndex; i++) {
			responseString += tempLines[i] + "\n";
		}

		return responseString;
	}

	/**
	 * Update the current sensor status in sensor_status.txt
	 *
	 */
	@POST
	@Path("/heartbeat")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String heartbeat(String msg) {
		MinioClient minioClient;
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			minioClient.removeObject("smart-clock-settings", "sensor_status.txt");

			// Store settings into string
			String settingsString = msg;

			ByteArrayInputStream bais = new ByteArrayInputStream(settingsString.getBytes("UTF-8"));
			minioClient.putObject("smart-clock-settings", "sensor_status.txt", bais, bais.available(),
					"application/octet-stream");
			bais.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "Status successfully stored in DB.";
	}

	/**
	 * Makes GET request to S3 bucket to retrieve sensor status
	 *
	 */
	@GET
	@Path("/getsensorstatus")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSensorStatus() {
		MinioClient minioClient;
		String settingsString = "";
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			// Retrieve string from S3.
			InputStream stream = minioClient.getObject("smart-clock-settings", "sensor_status.txt");
			byte[] buf = new byte[16384];
			int bytesRead;
			while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
				settingsString += new String(buf, 0, bytesRead);
			}
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return settingsString;
	}

	/**
	 * Add the weather icon to the matrix layout.
	 * 
	 * @param theCode
	 * @param matrix
	 * @throws IOException
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
		addToMatrix(layout, matrix, 27, 17); // Added +7 at 3rd parameter here after doing degree symbol
	}

	/**
	 * Append Data to the File in S3.
	 * 
	 * @param filename
	 * @param msg
	 */
	private void appendData(String filename, String msg) {
		MinioClient minioClient;
		String dataString = "";
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			// Retrieve string from S3.
			InputStream stream = minioClient.getObject("smart-clock-settings", filename);
			byte[] buf = new byte[16384];
			int bytesRead;
			while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
				dataString += new String(buf, 0, bytesRead);
			}
			stream.close();
			boolean shouldInsert = true;
			String[] dataLines = dataString.split("\\r?\\n");
			String latestData = dataLines[dataLines.length - 1];
			String[] oldKeyValue = latestData.split(",");
			String[] newKeyValue = msg.split(",");
			if (filename.equals("weather.csv")) {
				if (oldKeyValue[0].equalsIgnoreCase(newKeyValue[0])) {
					shouldInsert = false;
				}

			} else if (filename.equals("tempHum.csv")) {
				if (oldKeyValue[0].equalsIgnoreCase(newKeyValue[0])
						&& oldKeyValue[1].equalsIgnoreCase(newKeyValue[1])) {
					shouldInsert = false;
				}
			}

			// Append new data
			if (shouldInsert) {
				dataString += "\n" + msg;

				minioClient.removeObject("smart-clock-settings", filename);
				ByteArrayInputStream bais = new ByteArrayInputStream(dataString.getBytes("UTF-8"));
				minioClient.putObject("smart-clock-settings", filename, bais, bais.available(),
						"application/octet-stream");
				bais.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add the temperature to the matrix layout.
	 * 
	 * @param theTemperature
	 * @param matrix
	 * @throws IOException
	 */
	private void addTemperature(String theTemperature, int[][] matrix) throws IOException {
		MinioClient minioClient;
		String settingsString = "";
		try {
			// Initialize connection
			minioClient = new MinioClient("https://s3.amazonaws.com", "AKIAIWHBXX6HIVNDII3Q",
					"abpg9V9EtBnNA+bzMw2tcLS9OqhSIDpdNNrb1P3R");

			// Retrieve string from S3.
			InputStream stream = minioClient.getObject("smart-clock-settings", "settings.txt");
			byte[] buf = new byte[16384];
			int bytesRead;
			while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
				settingsString += new String(buf, 0, bytesRead);
			}
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		int red = 255, blue = 255, green = 0;
		String[] settings = settingsString.split("\\r?\\n");
		for (int i = 0; i < settings.length; i++) {
			String[] keyValue = settings[i].split("=");
			if (keyValue[0].equalsIgnoreCase("wRed")) {
				red = Integer.parseInt(keyValue[1]);
			} else if (keyValue[0].equalsIgnoreCase("wGreen")) {
				green = Integer.parseInt(keyValue[1]);
			} else if (keyValue[0].equalsIgnoreCase("wBlue")) {
				blue = Integer.parseInt(keyValue[1]);
			}
		}
		Color color = new Color(red, green, blue);
		// Convert the temperature to char array (e.g. from 12 to [1,2])
		char[] tempChar = theTemperature.toCharArray();

		LinkedList<Pixel> layout = readFile(tempChar[0] + ".csv");
		addToMatrixCC(layout, matrix, 2, 24, color);

		LinkedList<Pixel> mLayout = readFile(tempChar[1] + ".csv");
		addToMatrixCC(mLayout, matrix, 8, 24, color);

		LinkedList<Pixel> nLayout = readFile("symbol_2.csv");
		addToMatrixCC(nLayout, matrix, 14, 25, color);
	}

	/**
	 * Add the layout to the matrix according to the starting column and row.
	 * 
	 * @param theLayout
	 *            a linked list of Pixel objects
	 * @param matrix
	 *            the 2D matrix
	 * @param startCol
	 *            start column of data to add to the matrix
	 * @param startRow
	 *            start row of data to add to the matrix
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
	 * Add the layout to the matrix with custom color according to the starting
	 * column and row.
	 * 
	 * @param theLayout
	 *            a linked list of Pixel objects
	 * @param matrix
	 *            the 2D matrix
	 * @param startCol
	 *            start column of data to add to the matrix
	 * @param startRow
	 *            start row of data to add to the matrix
	 * @param color
	 *            the color
	 */
	public void addToMatrixCC(LinkedList<Pixel> theLayout, int[][] matrix, int startCol, int startRow, Color color) {
		int row, col = 0;
		for (int i = 0; i < theLayout.size(); i++) {
			row = theLayout.get(i).getRow();
			col = theLayout.get(i).getCol();
			System.out.println("row = " + row + " col = " + col);
			// Not sure it is [col][row] or [row][col]
			matrix[row + startRow][col + startCol] = 1;

			colorMatrix[row + startRow][col + startCol] = color;
		}
	}

	/**
	 * Convert the 2D matrix to 1D array (the LinkedList of Pixel).
	 * 
	 * @param matrix the matrix.
	 * @return the converted matrix.
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
