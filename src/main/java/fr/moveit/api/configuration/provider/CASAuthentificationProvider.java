package fr.moveit.api.configuration.provider;

import fr.moveit.api.configuration.Roles;
import fr.moveit.api.dto.CASAuthentificationDTO;
import fr.moveit.api.dto.CASUserDTO;
import fr.moveit.api.entity.User;
import fr.moveit.api.repository.RoleRepository;
import fr.moveit.api.repository.UserRepository;
import fr.moveit.api.utils.JsonUtils;
import fr.moveit.api.utils.exception.CASServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.security.auth.message.AuthException;
import java.util.ArrayList;
import java.util.Collections;

@Component
public class CASAuthentificationProvider implements AuthenticationProvider {

	final JsonUtils jsonUtils;

	final UserRepository repository;
	final RoleRepository roleRepository;

	private WebClient client;
	private final Logger LOG = LoggerFactory.getLogger(CASAuthentificationProvider.class);
	private final String ISEP_CAS_URL = "https://sso-portal.isep.fr";

	public CASAuthentificationProvider(JsonUtils jsonUtils, UserRepository repository, RoleRepository roleRepository) {
		this.jsonUtils = jsonUtils;
		this.repository = repository;
		this.roleRepository = roleRepository;
	}

	@PostConstruct
	public void initializeCASClient() {
		client = WebClient.builder()
				.baseUrl(ISEP_CAS_URL)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	@Override
	public Authentication authenticate(Authentication auth) throws AuthenticationException {
		try {
			String username = auth.getName();
			String password = auth.getCredentials().toString();
			ArrayList<GrantedAuthority> authorities = new ArrayList<>();

			authorities.add(new SimpleGrantedAuthority(Roles.USER));

			CASUserDTO CASUser = identifyToCAS(username, password);

			// If user is connecting for the first time,
			// then we create an account for the user in the db
			if(!repository.existsById(CASUser.getNumero())){
				User user = new User();
				user.setId(CASUser.getNumero());   // User's id is ISEP unique identification number

				user.setEmail(CASUser.getMail());
				user.setFirstName(CASUser.getPrenom());
				user.setLastName(CASUser.getNom());

				user.setRoles(Collections.singleton(roleRepository.findByRole(Roles.USER)));

				repository.save(user);
			}

			return new UsernamePasswordAuthenticationToken(CASUser, password, authorities);
		}catch (Exception e){
			throw new BadCredentialsException("Echec de l'authentification");
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}


	private CASUserDTO identifyToCAS(String username, String password) {
		try {
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("user", username);
			form.add("password", password);

			CASUserDTO response = client.post()
					.body(BodyInserters.fromFormData(form))
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
					.exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class).flatMap(json -> {
								/*
								 * We parse manually as the wrong content-type header is given when authentification failed
								 * (Content-Type	application/javascript)
								 */
								CASAuthentificationDTO body = jsonUtils.deserialize(json, CASAuthentificationDTO.class);
								if (body == null || body.getResult() == 0)
									return Mono.error(new AuthException("Authentification failed"));

								ResponseCookie CASCookie = clientResponse.cookies().getFirst("lemonldap");
								if (CASCookie == null) {
									LOG.error("CAS cookie (lemonldap) not found");
									return Mono.error(new AuthException("CAS cookie has been missing from the response"));
								}

								return accessUser(CASCookie);
							})
					)
					.block();

			if(response == null){
				LOG.warn("CAS user's information not accessed");
				throw new BadCredentialsException("Echec de l'authentification");
			}

			return response;
		} catch (WebClientException e) {
			LOG.error("CAS unavailable");
			throw new CASServiceException("CAS unavailable", e);
		}
	}

	private Mono<CASUserDTO> accessUser(ResponseCookie cookie) {
		return client.post()
				.uri("/session/my/global")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
				.cookie(cookie.getName(), cookie.getValue())
				.retrieve()
				.bodyToMono(CASUserDTO.class);
	}
}
