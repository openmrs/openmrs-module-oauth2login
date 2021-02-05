package org.openmrs.module.oauth2login.authscheme;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.openmrs.module.oauth2login.authscheme.UserInfo.PROP_ROLES;

import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class UserInfoTest {
	
	private UserInfo userInfo;
	
	private Properties oauth2Props = new Properties();
	
	@Test
	public void getRoleNames_shouldParseAndTrimRoleNamesWhenMappingIsDefined() {
		// setup
		oauth2Props.setProperty(PROP_ROLES, "roles");
		userInfo = new UserInfo(oauth2Props, "{\"roles\":\"Nurse, Doctor\"}");
		
		// replay
		List<String> roleNames = userInfo.getRoleNames();
		
		// verify
		Assert.assertThat(roleNames, hasSize(2));
		Assert.assertThat(roleNames, containsInAnyOrder("Nurse", "Doctor"));
	}
	
	@Test
	public void getRoleNames_shouldParseToEmptyRoleNamesWhenMappingIsNotDefined() {
		// setup
		oauth2Props = new Properties();
		userInfo = new UserInfo(oauth2Props, "{\"roles\":\"Nurse, Doctor\"}");
		
		// replay
		List<String> roleNames = userInfo.getRoleNames();
		
		// verify
		Assert.assertThat(roleNames, empty());
	}
	
	@Test
	public void getRoleNames_shouldParseToEmptyRoleNamesWhenNoneInUserInfo() {
		// setup
		oauth2Props.setProperty(PROP_ROLES, "roles");
		userInfo = new UserInfo(oauth2Props, "{}");
		
		// replay
		List<String> roleNames = userInfo.getRoleNames();
		
		// verify
		Assert.assertThat(roleNames, empty());
	}
}
