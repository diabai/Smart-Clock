package com.uwt.tcss573;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

public class SystemDB {

	private String userName;
	private String password;
	private String serverName;
	private Connection conn;
	private ArrayList<String> settingsList;

	public SystemDB() {
		settingsList = new ArrayList<>();
		userName = "weatherdb";
		password = "myweatherdb";
		serverName = "weatherinstance.ckxeajky6rzs.us-west-2.rds.amazonaws.com";
	}

	/**
	 * Creates a sql connection to MySQL using the properties for userid, password
	 * and server information.
	 * 
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public void createConnection() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		Properties connectionProps = new Properties();
		connectionProps.put("user", userName);
		connectionProps.put("password", password);

		conn = DriverManager.getConnection(
				"jdbc:mysql://" + serverName + "/" + userName + "?user=" + userName + "&password=" + password);
	}

	/**
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public ArrayList<String> getSettings() throws SQLException, ClassNotFoundException {
		if (conn == null) {
			createConnection();
		}
		Statement stmt = null;
		String query = "SELECT `name` FROM Settings;";

		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				String settingName = rs.getString("name");
				settingsList.add(settingName);
			}
		} catch (SQLException e) {
			System.out.println(e);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
		return settingsList;
	}

	public String updateSettings(String newSetting) throws SQLException, ClassNotFoundException {

		if (conn == null) {
			createConnection();
		}

		// String settingToUpdate = getSettings().get(index);
		String sql = "UPDATE Settings SET `name` = ?  WHERE `name` = ? ";
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn.prepareStatement(sql);

			preparedStatement.setString(1, (String) newSetting);

			// preparedStatement.setString(1, (String) settingToUpdate);
			preparedStatement.setString(2, (String) "Tally");
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e);
			e.printStackTrace();
			return "Error updating table";
		}
		return "Table updated successfully";
	}

}
