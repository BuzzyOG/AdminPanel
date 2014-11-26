package com.mcapanel.web.controllers;

import java.io.IOException;
import java.util.List;

import javax.persistence.OptimisticLockException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.mcapanel.web.database.Group;
import com.mcapanel.web.database.User;
import com.mcapanel.web.handlers.Controller;

public class UsersController extends Controller
{
	public boolean canView()
	{
		return isLoggedIn() && user.getGroup().hasPermission("web.users.view");
	}
	
	public boolean index()
	{
		request.setAttribute("userGroups", getUserGroups());
		
		request.setAttribute("users", arrayToString(getUsersJson(true)));
		
		return renderView();
	}
	
	@SuppressWarnings("unchecked")
	public boolean saveUsers() throws IOException
	{
		if (isMethod("POST"))
		{
			includeIndex(false);
			mimeType("application/json");
			
			JSONObject obj = new JSONObject();
			
			if (canView())
			{
				String data = request.getParameter("data");
				
				if (data != null)
				{
					JSONArray users = (JSONArray) JSONValue.parse(data);
					
					for (Object o : users)
					{
						JSONObject user = (JSONObject) o;
						
						User u = db.find(User.class, Integer.parseInt(user.get("id").toString()));
						Group g = db.find(Group.class).where().ieq("group_name", (String) user.get("group")).findUnique();
						
						if (u != null && g != null)
						{
							u.setGroupId(g.getId());
							
							try
							{
								db.save(u);
							} catch (OptimisticLockException e) { }
						}
					}
					
					obj.put("good", "Successfully saved all user settings.");
				} else
					obj.put("error", "Error parsing your request.");
			} else
				obj.put("error", "You do not have permission to do that.");
			
			response.getWriter().println(obj.toJSONString());
			
			return true;
		}
		
		return error();
	}
	
	@SuppressWarnings("unchecked")
	private JSONArray getUsersJson(boolean raw)
	{
		JSONArray s = new JSONArray();
		
		List<User> users = db.find(User.class).findList();
		
		String b = raw ? "<td>" : "";
		String e = raw ? "</td>" : "";
		
		for (User u : users)
		{
			JSONArray ar = new JSONArray();
			
			if (raw) ar.add("<tr style='cursor: default;'>");
			ar.add(b + u.getId() + e);
			ar.add(b + u.getUsername() + e);
			ar.add(b + "<span class=\"gname\" style=\"cursor: pointer;\">" + u.getGroup().getGroupName() + "</span>" + e);
			ar.add(b + "<span class=\"label label-" + (u.isWhitelisted() ? "success\">true" : "danger\">false") + "</span>" + e);
			ar.add(b + "<span class=\"label label-" + (u.isBlacklisted() ? "success\">true" : "danger\">false") + "</span>" + e);
			if (raw) ar.add("</tr>");
			
			s.add(ar);
		}
		
		return s;
	}
	
	private String getUserGroups()
	{
		String s = "";
		
		for (Group g : db.find(Group.class).findList())
		{
			s += "<option value='" + g.getGroupName() + "'>" + g.getGroupName() + "</option>";
		}
		
		return s;
	}
}