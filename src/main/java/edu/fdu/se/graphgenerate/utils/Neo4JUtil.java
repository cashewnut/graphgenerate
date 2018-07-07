package edu.fdu.se.graphgenerate.utils;

import static org.neo4j.driver.v1.Values.parameters;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

public class Neo4JUtil {

	private final static String uri = LoadProperties.get("URI");
	private final static String username = LoadProperties.get("USERNAME");
	private final static String password = LoadProperties.get("PASSWORD");
	
//	 private final static String uri = "bolt://localhost:7687";
//	 private final static String username = "xiyaoguo@yeah.net";
//	 private final static String password = "5611786xyy";

	public static Driver getDriver() {
		Driver driver = null;
		try {
			driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return driver;
	}

	public static void closeDriver(Driver driver) {
		try {
			if (driver != null)
				driver.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Session getSession(Driver driver) {
		Session session = null;
		try {
			if (driver != null)
				session = driver.session();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return session;
	}

	public static void closeSession(Session session) {
		try {
			if (session != null)
				session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Driver driver = Neo4JUtil.getDriver();
		Session session = Neo4JUtil.getSession(driver);
		Integer id = session.writeTransaction(new TransactionWork<Integer>() {
			@Override
			public Integer execute(Transaction tx) {
				StatementResult result = tx.run(
						"CREATE(n:aaa:$b3{name:$name,ff:$ff}) SET n:bbb RETURN id(n)",
						parameters("b3","bab","name", "n","ff","ss"));
				return result.single().get(0).asInt();
			}
		});
		Neo4JUtil.closeSession(session);
		Neo4JUtil.closeDriver(driver);
		System.out.println(id);

	}

}
