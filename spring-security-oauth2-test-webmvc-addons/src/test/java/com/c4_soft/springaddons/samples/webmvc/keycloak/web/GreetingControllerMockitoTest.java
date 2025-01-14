
package com.c4_soft.springaddons.samples.webmvc.keycloak.web;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.c4_soft.springaddons.samples.webmvc.keycloak.KeycloakSpringBootSampleApp;
import com.c4_soft.springaddons.samples.webmvc.keycloak.service.MessageService;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.MockMvcSupport;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.keycloak.ServletKeycloakAuthUnitTestingSupport;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = GreetingController.class)
@Import({ ServletKeycloakAuthUnitTestingSupport.UnitTestConfig.class, KeycloakSpringBootSampleApp.KeycloakConfig.class })
// because this sample stands in the middle of non spring-boot-keycloak projects, keycloakproperties are isolated in
// application-keycloak.properties
@ActiveProfiles("keycloak")
public class GreetingControllerMockitoTest {
	private static final String GREETING = "Hello %s! You are granted with %s.";

	@MockBean
	MessageService messageService;

	@MockBean
	JwtDecoder jwtDecoder;

	@Autowired
	MockMvcSupport api;

	@Before
	public void setUp() {
		when(messageService.greet(any())).thenAnswer(invocation -> {
			final Authentication auth = invocation.getArgument(0, Authentication.class);
			return String.format(GREETING, auth.getName(), auth.getAuthorities());
		});
	}

	@Test
	public void whenAuthenticatedWithKeycloakAuthenticationTokenThenCanGreet() throws Exception {
		configureSecurityContext("ch4mpy", "USER", "AUTHORIZED_PERSONNEL", "TESTER");

		api
				.get("/greet")
				.andExpect(status().isOk())
				.andExpect(content().string(startsWith("Hello ch4mpy! You are granted with ")))
				.andExpect(content().string(containsString("AUTHORIZED_PERSONNEL")))
				.andExpect(content().string(containsString("USER")))
				.andExpect(content().string(containsString("TESTER")));
	}

	@Test
	public void whenAuthenticatedWithoutAuthorizedPersonnelThenSecuredRouteIsForbidden() throws Exception {
		configureSecurityContext("ch4mpy", "USER");

		api.get("/secured-route").andExpect(status().isForbidden());
	}

	@Test
	public void whenAuthenticatedWithAuthorizedPersonnelThenSecuredRouteIsOk() throws Exception {
		configureSecurityContext("ch4mpy", "AUTHORIZED_PERSONNEL");

		api.get("/secured-route").andExpect(status().isOk());
	}

	private void configureSecurityContext(String username, String... roles) {
		final Principal principal = mock(Principal.class);
		when(principal.getName()).thenReturn(username);

		final OidcKeycloakAccount account = mock(OidcKeycloakAccount.class);
		when(account.getRoles()).thenReturn(new HashSet<>(Arrays.asList(roles)));
		when(account.getPrincipal()).thenReturn(principal);

		final KeycloakAuthenticationToken authentication = mock(KeycloakAuthenticationToken.class);
		when(authentication.getAccount()).thenReturn(account);

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
