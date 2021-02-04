package org.openmrs.module.oauth2login.authscheme;

import org.openmrs.User;
import org.openmrs.api.UserService;

public class UpdateUserTask implements Runnable {
	
	private UserService userService;
	
	OAuth2TokenCredentials credentials;
	
	public UpdateUserTask(UserService userService, OAuth2TokenCredentials credentials) {
		this.userService = userService;
		this.credentials = credentials;
	}
	
	@Override
	public void run() {
		User daemonUser = this.userService.getUserByUsername(credentials.getUserInfo().getUsername());
		this.userService.saveUser(updateUserInfo(daemonUser, credentials));
	}
	
	/**
	 * Update user information with base on another user
	 * 
	 * @param userToUpdate user to update with info
	 * @param credentials user credentials
	 * @return the user updated
	 */
	private User updateUserInfo(User userToUpdate, OAuth2TokenCredentials credentials) {
		userToUpdate.getPerson().getPersonName()
		        .setGivenName(credentials.getUserInfo().getOpenmrsUser().getPersonName().getGivenName());
		userToUpdate.getPerson().getPersonName()
		        .setMiddleName(credentials.getUserInfo().getOpenmrsUser().getPersonName().getMiddleName());
		userToUpdate.getPerson().getPersonName()
		        .setFamilyName(credentials.getUserInfo().getOpenmrsUser().getPersonName().getFamilyName());
		userToUpdate.getPerson().setGender(credentials.getUserInfo().getOpenmrsUser().getPerson().getGender());
		userToUpdate.setEmail(credentials.getUserInfo().getOpenmrsUser().getEmail());
		return userToUpdate;
	}
}
