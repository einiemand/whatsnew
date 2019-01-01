package rpc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;

/**
 * Servlet implementation class Register
 */
@WebServlet("/register")
public class Register extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Register() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		DBConnection connection = DBConnectionFactory.getConnection();
		JSONObject input = RpcHelper.readJSONObject(request);
		JSONObject obj = new JSONObject();
		try {
			String userId = input.getString("user_id");
			if (connection.isUserIdUsed(userId)) {
				response.setStatus(401);
				obj.put("status", "User Id Already In Use");
			} else {
				String password = input.getString("password");
				String firstName = input.getString("first_name");
				String lastName = input.getString("last_name");
				connection.registerNewUser(userId, password, firstName, lastName);
				obj.put("status", "OK");
			}
			RpcHelper.writeJsonObject(response, obj);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			connection.close();
		}

	}

}
