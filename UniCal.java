/**
 * A Servlet application for unit calculator powered with database for user-defined units
 * @author Zhiwei Jia
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;

/**
 * Servlet implementation class UniCal
 */
@WebServlet("/UniCal")
public class UniCal extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private int hitCount;

	public void init() { 
	    hitCount = 0;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		// create the db instance
		Connection con = null;
        PreparedStatement ps = null;
		
		// create a unical instance
	    UnitCalculator uc = new UnitCalculator();
	    
	    // set up response io
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.append("<html><body>"); 

        // get the parameter      
		Map<String,Quantity> custom = null;
		String input = request.getParameter("input");
		String result = input;
		Cookie[] cookies = request.getCookies();
		String clear = "                              ";

		// clear all history if users choose to do so
		if (cookies != null && input.equals(clear)) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie curr = new Cookie(cookies[i].getName(), "");
				curr.setPath("/");
				curr.setDomain("http://unical.herokuapp.com");  
				curr.setMaxAge(0);
				response.addCookie(curr);
			}
			return;
		}	
		Map<String,Quantity> classic = QuantityDB.getDB();
		// parsing and obtaining result
		try {
			
	        // Register JDBC driver
	        Class.forName("org.postgresql.Driver");

	        // Open a connection
	        try {
	            con = getConnection();
	        } catch (Exception e) {
	        	System.exit(-1);
	        }
	        
	        // create a table
        	Statement st = con.createStatement();
	        String sql = "CREATE TABLE IF NOT EXISTS unitdb(db bytea)";  
	        st.executeUpdate(sql);

	        // retrieve the quantity database
	        st = con.createStatement();
	        ResultSet rs = st.executeQuery("SELECT * FROM unitdb;");
	        
			// get the map, and there should be only one object in the UnitDB
        	byte[] readBytes = null;
        	ObjectInputStream mapIn = null;
        	Object obj = null;
			while (rs.next()) 
                readBytes = rs.getBytes(1);
        	if (readBytes != null) {
	            mapIn = new ObjectInputStream(new ByteArrayInputStream(readBytes));
		        obj = mapIn.readObject();
		    }
			try {
				@SuppressWarnings("unchecked")
				Map<String, Quantity> c = (Map<String, Quantity>)obj;
				c.putAll(classic);  // this is for maintaining classic units
				custom = c;
			} catch (Exception ex) {
                System.exit(-1);
			}
				
	        // evaluate
			AST ast = null;	
			uc.tokenize(input);
			try {
				ast = uc.parse();
				result = ast.eval(custom) + ""; 
			} catch (ParseError e) {
			    out.append("<p>There is an error occur: "+e.getMessage()+"</p>"); 
			}		
			rs.close();
			st.close();
            mapIn.close();			
	    } catch (Exception e) {
            // do nothing
	    } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
            	System.exit(-1);
            }
        } 
			
		// update the UnitDB
		try {
		    // Register JDBC driver
		    Class.forName("org.postgresql.Driver");
            
		    // Open a connection
		    try {
		       con = UniCal.getConnection();
		    } catch (Exception e) {
		       System.exit(-1);
		    }
			
		    // Execute SQL query to update db
   	        ps = con.prepareStatement("DELETE FROM unitdb;");
		    ps.executeUpdate();
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream(baos);
		    oos.writeObject(custom);
		    byte[] writeBytes = baos.toByteArray();
		    ps = con.prepareStatement("INSERT INTO UnitDB(db) VALUES (?);");
		    ps.setBytes(1, writeBytes);
		    ps.executeUpdate();
   	    
  		    // close the resources
		    ps.close();
		    oos.close();
		    baos.close();
		    con.close();
		} catch (Exception e){
		    System.exit(-1);
		} finally {
	        try {
	            if (ps != null) {
	                ps.close();
	            }
	            if (con != null) {
	                con.close();
	            }
	        } catch (SQLException ex) {
	            System.exit(-1);
	        }
	    } 
		
		// create two cookies for a term: first the result, then the input (inverse order)
		if (input != null) {
			Cookie ck2 = new Cookie("unicalcresul" + ++hitCount, result);
			ck2.setMaxAge(60*60);
			ck2.setDomain("http://unical.herokuapp.com");  
			ck2.setPath("/");
			ck2.setComment("for displaying history");
			response.addCookie(ck2);
			Cookie ck1 = new Cookie("unicalcinput" + ++hitCount, input);
			ck1.setMaxAge(60*60);
			ck1.setDomain("http://unical.herokuapp.com");  
			ck1.setPath("/");
			ck1.setComment("for displaying history");
			response.addCookie(ck1);
		}

		// for storing valid cookies
		ArrayList<Cookie> coos = new ArrayList<>();
		
		if (input != null) {
		
			// display the current calculation
			out.append("<p>Input >> " + input + "</p>");
			out.append("<p>Result: " + result + "</p>");
		}
		
		if (cookies != null) {
			
			// storing valid cookies as history	
			for (Cookie coo: cookies) 
				if (coo.getName().length() >= 12 && coo.getName().substring(0,7).equals("unicalc")) 
					coos.add(coo);
			
			// sorting according to hit count
			Collections.sort(coos, new CookieComparator());
			
			// setting the correct number of entries of shown history 
			int hisCount = coos.size() - 70;
			if (hisCount < 0) 
				hisCount = 0;
			
			// display certain number of recent history
			for (int i = coos.size()-1; i >= hisCount; i--) {
				if (coos.get(i).getName().substring(0,12).equals("unicalcinput")) {
					out.append("<p>Input >> "+coos.get(i).getValue()+"</p>");
				}
				else if (coos.get(i).getName().substring(0,12).equals("unicalcresul")) {
					out.append("<p>Result: "+coos.get(i).getValue()+"</p>");
				}
			}
		}
		
		// closing tags
		out.append("</body></html>"); 
		
		int diff = coos.size() - 60;
		if (cookies != null && diff > 0) {	
			for (int i = 0; i < 2; i++) {
				Cookie curr = new Cookie(coos.get(i).getName(), "");
				curr.setPath("/");
				curr.setDomain("http://unical.herokuapp.com");  
				curr.setMaxAge(0);
				response.addCookie(curr);
			}
		}
		
		// closing the response printer
		out.close();
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	/**
	 * helper method to get connection
	 */
	private static Connection getConnection() throws URISyntaxException, SQLException {
		URI dbUri = new URI(System.getenv("DATABASE_URL"));

	    // set the proper name
	    String username = dbUri.getUserInfo().split(":")[0];
	    String password = dbUri.getUserInfo().split(":")[1];
	    String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

	    // set connection and return it
	    return DriverManager.getConnection(dbUrl, username, password);
	}
}

