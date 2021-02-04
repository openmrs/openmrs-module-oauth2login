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
		User daemonUser = this.userService.getUser(user.getUserId());
		this.userService.saveUser(updateUserInfo(daemonUser, user));
	}
	
	/**
	 * Update user information with base on another user
	 * 
	 * @param userToUpdate user to update with info
	 * @param userToCopy user to get info from
	 * @return the user updated
	 */
	private User updateUserInfo(User userToUpdate, User userToCopy) {
		userToUpdate.getPerson().getPersonName().setGivenName(userToCopy.getPerson().getPersonName().getGivenName());
		userToUpdate.getPerson().getPersonName().setMiddleName(userToCopy.getPerson().getPersonName().getMiddleName());
		userToUpdate.getPerson().getPersonName().setFamilyName(userToCopy.getPerson().getPersonName().getFamilyName());
		userToUpdate.getPerson().setGender(userToCopy.getPerson().getGender());
		userToUpdate.setEmail(userToCopy.getEmail());
		return userToUpdate;
	}
}
