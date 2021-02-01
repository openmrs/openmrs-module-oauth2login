package org.openmrs.module.oauth2login.authscheme;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class OAuth2UserTest {
	
	private OAuth2User oauth2User;
	
	private Properties oauth2Props = new Properties();
	
	@Test
	public void getRoleNames_shouldParseAndTrimRoleNamesWhenMappingIsDefined() {
		// setup
		oauth2Props.setProperty(OAuth2User.MAPPINGS_PFX + OAuth2User.PROP_ROLES, "roles");
		oauth2User = new OAuth2User("jdoe@example.com", "{\"roles\":\"nurse, doctor\"}");
		
		// replay
		List<String> roleNames = oauth2User.getRoleNames(oauth2Props);
		
		// verify
		Assert.assertThat(roleNames, hasSize(2));
		Assert.assertThat(roleNames, containsInAnyOrder("nurse", "doctor"));
	}
	
	@Test
	public void getRoleNames_shouldParseToEmptyRoleNamesWhenMappingIsNotDefined() {
		// setup
		oauth2Props = new Properties();
		oauth2User = new OAuth2User("jdoe@example.com", "{\"roles\":\"nurse, doctor\"}");
		
		// replay
		List<String> roleNames = oauth2User.getRoleNames(oauth2Props);
		
		// verify
		Assert.assertThat(roleNames, empty());
	}
}
