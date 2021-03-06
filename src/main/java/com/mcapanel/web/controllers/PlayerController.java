package com.mcapanel.web.controllers;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mcapanel.web.database.User;
import com.mcapanel.web.handlers.Controller;

@SuppressWarnings({"unchecked"})
public class PlayerController extends Controller
{
	public boolean canView()
	{
		return bukkitServer.getPluginConnector().connected();
	}
	
	public boolean view() throws IOException
	{
		if (arguments.size() == 1)
		{
			includeSidebar();
			
			String pRet = bukkitServer.getPluginConnector().sendMethodResponse("getPlayer", arguments.get(0));
			
			if (pRet != null)
			{
				try
				{
					JSONObject player = (JSONObject) jsonParser.parse(pRet);
	
					if (!(Boolean) player.get("exists"))
						return error();
					
					User u = ap.getUserFromPlayer((String) player.get("name"));
					
					request.setAttribute("player", player.get("name"));
					request.setAttribute("group", u != null ? u.getGroup().getGroupName() : "Not Registered");
					
					request.setAttribute("playerStatus", (Boolean) player.get("online") ? "Online" : "Offline");
					request.setAttribute("statusLabel", (Boolean) player.get("online") ? "success" : "danger");
					
					request.setAttribute("firstPlayed", player.get("firstPlayed"));
					request.setAttribute("lastPlayed", player.get("lastPlayed"));
					
					request.setAttribute("health", player.get("health"));
					request.setAttribute("food", player.get("food"));
				
					return renderView();
				} catch (ParseException e) { }
			}
		}
		
		return error();
	}
	
	public boolean event() throws IOException
	{
		if (isMethod("POST"))
		{
			includeIndex(false);
			mimeType("application/json");
			
			JSONObject obj = new JSONObject();
			
			if (bukkitServer.getPluginConnector().connected())
			{
				if (isLoggedIn() && arguments.size() >= 2)
				{
					String player = arguments.get(0);
					String method = arguments.get(1);
					String message = arguments.size() == 3 ? arguments.get(2) : null;
					
					if (((method.equals("heal") || method.equals("feed")) && user.getGroup().hasPermission("server.players.healfeed"))
							|| (method.equals("kill") && user.getGroup().hasPermission("server.players.kill"))
							|| (method.equals("kick") && user.getGroup().hasPermission("server.players.kick"))
							|| (method.equals("ban") && user.getGroup().hasPermission("server.players.ban")))
					{
						String ret = bukkitServer.getPluginConnector().sendMethodResponse("doPlayerEvent", player, method, message);
						
						try
						{
							obj = (JSONObject) new JSONParser().parse(ret);
						} catch (ParseException e)
						{
							obj.put("error", "Invalid method supplied");
						}
					} else
						obj.put("error", "You do not have permission to do that.");
				} else
					obj.put("error", "You do not have permission to do that.");
			} else
				obj.put("error", "The server is not started.");
			
			response.getWriter().println(obj.toJSONString());
			
			return true;
		}
		
		return error();
	}
}