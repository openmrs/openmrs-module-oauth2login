package org.openmrs.module.oauth2login.authscheme;

import org.openmrs.User;
import org.openmrs.api.UserService;

public class UpdateUserTask implements Runnable {
	
	private UserService userService;
	
	private User user;
	
	public UpdateUserTask(UserService userService, User user) {
		this.userService = userService;
		this.user = user;
	}
	
	@Override
	public void run() {
		this.userService.saveUser(user);
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
}
