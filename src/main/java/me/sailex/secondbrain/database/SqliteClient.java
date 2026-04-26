package me.sailex.secondbrain.database;

import me.sailex.secondbrain.SecondBrain;
import java.io.File;
import java.sql.*;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SQLite client for managing the database.
 */
public class SqliteClient {

	private static final Logger LOGGER = LogManager.getLogger(SqliteClient.class);
	private Connection connection;

	/**
	 * Create the database.
	 */
	public void initDatabase(String databaseName) {
		String databasePath = initDataBaseDir();
		try {
			String jdbcUrl = String.format("jdbc:sqlite:%s/%s.db", databasePath, databaseName);
			connection = DriverManager.getConnection(jdbcUrl);
			if (connection.isValid(3)) {
				LOGGER.info("Connected to database at: {}", databasePath);
			}
		} catch (SQLException e) {
			LOGGER.error("Error creating/connecting to database: {}", e.getMessage());
		}
	}

	private String initDataBaseDir() {
		File configDir = FabricLoader.getInstance().getConfigDir().toFile();
		File sqlDbDir = new File(configDir, SecondBrain.MOD_ID);
		if (sqlDbDir.mkdirs()) {
			LOGGER.info("Database directory created at: {}", sqlDbDir.getAbsolutePath());
		}
		return sqlDbDir.getAbsolutePath();
	}

	/**
	 * Select entries from db.
	 * @param sql the SQL query
	 */
	public ResultSet query(String sql) {
		try {
			Statement statement = requireConnection().createStatement();
			statement.closeOnCompletion();
			return statement.executeQuery(sql);
		} catch (SQLException e) {
			throw new IllegalStateException("Error selecting rows for query: " + sql, e);
		}
	}

	/**
	 * Execute prepared statement.
	 * @param statement the prepared statement
	 */
	public void update(PreparedStatement statement) {
		try {
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Error executing prepared statement: " + statement, e);
		}
	}

	public PreparedStatement buildPreparedStatement(String sql) {
		try {
			return requireConnection().prepareStatement(sql);
		} catch (SQLException e) {
			throw new IllegalStateException("Error building prepared statement: " + sql, e);
		}
	}

	/**
	 * Create a table in the database.
	 * @param sql the SQL query to create a table
	 */
	public void update(String sql) {
		try (Statement statement = requireConnection().createStatement()) {
			statement.execute(sql);
		} catch (SQLException e) {
			throw new IllegalStateException("Error executing SQL query: " + sql, e);
		}
	}

	/**
	 * Close the database connection.
	 */
	public void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
				LOGGER.info("Database connection closed.");
			}
		} catch (SQLException e) {
			LOGGER.error("Error closing database connection: {}", e.getMessage());
		}
	}

	private Connection requireConnection() {
		if (connection == null) {
			throw new IllegalStateException("Database connection is not initialized.");
		}
		try {
			if (connection.isClosed()) {
				throw new IllegalStateException("Database connection is already closed.");
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Could not access database connection state.", e);
		}
		return connection;
	}
}
