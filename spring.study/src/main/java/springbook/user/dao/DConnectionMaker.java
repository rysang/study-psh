package springbook.user.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DConnectionMaker implements ConnectionMaker{

	public Connection makeConnection() throws ClassNotFoundException,
			SQLException {
		Class.forName("org.hsqldb.jdbcDriver"); 
		Connection c = DriverManager.getConnection("jdbc:hsqldb:hsql://127.0.0.1/lecture", "sa", "");
		return c;
	}

	
}
