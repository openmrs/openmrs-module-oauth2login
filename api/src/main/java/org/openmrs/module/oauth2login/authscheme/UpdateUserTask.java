package org.openmrs.module.oauth2login.authscheme;

import org.openmrs.User;
import org.openmrs.api.UserService;

public class UpdateUserTask implements Runnable {
	
	private UserService userService;
	
	private UserInfo userInfo;
	
	public UpdateUserTask(UserService userService, UserInfo userInfo) {
		this.userService = userService;
		this.userInfo = userInfo;
	}
	
	@Override
	public void run() {
		User user = userService.getUserByUsername(userInfo.getUsername());
		userService.saveUser(updated(user));
	}
	
	/**
	 * Returns the updated user as per the user info.
	 * 
	 * @param user The user to update
	 * @return The updated user.
	 */
	private User updated(User user) {
		
		user.setEmail(userInfo.getOpenmrsUser().getEmail());
		
		user.getPerson().getPersonName().setGivenName(userInfo.getOpenmrsUser().getPersonName().getGivenName());
		user.getPerson().getPersonName().setMiddleName(userInfo.getOpenmrsUser().getPersonName().getMiddleName());
		user.getPerson().getPersonName().setFamilyName(userInfo.getOpenmrsUser().getPersonName().getFamilyName());
		user.getPerson().setGender(userInfo.getOpenmrsUser().getPerson().getGender());
		
		return user;
	}
}
