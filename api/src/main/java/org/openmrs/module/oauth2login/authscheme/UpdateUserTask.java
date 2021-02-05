package org.openmrs.module.oauth2login.authscheme;

import java.util.stream.Collectors;

import org.openmrs.User;
import org.openmrs.api.UserService;

public class UpdateUserTask implements Runnable {
	
	private UserService userService;
	
	private UserInfo userInfo;
	
	private User user;
	
	public UpdateUserTask(UserService userService, UserInfo userInfo) {
		this.userService = userService;
		this.userInfo = userInfo;
	}
	
	public User getUpdatedUser() {
		return user;
	}
	
	@Override
	public void run() {
		user = userService.getUserByUsername(userInfo.getUsername());
		user = userService.saveUser(updated(user));
	}
	
	/**
	 * Returns the updated user as per the user info.
	 * 
	 * @param user The user to update
	 * @return The updated user.
	 */
	private User updated(User user) {
		
		user.setEmail(userInfo.getOpenmrsUser().getEmail());
		user.setRoles(userInfo.getRoleNames().stream().map(roleName -> userService.getRole(roleName)).filter(r -> r != null).collect(Collectors.toSet()));
		
		user.getPerson().getPersonName().setGivenName(userInfo.getOpenmrsUser().getPersonName().getGivenName());
		user.getPerson().getPersonName().setMiddleName(userInfo.getOpenmrsUser().getPersonName().getMiddleName());
		user.getPerson().getPersonName().setFamilyName(userInfo.getOpenmrsUser().getPersonName().getFamilyName());
		user.getPerson().setGender(userInfo.getOpenmrsUser().getPerson().getGender());
		
		return user;
	}
}
