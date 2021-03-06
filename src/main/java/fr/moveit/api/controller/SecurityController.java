package fr.moveit.api.controller;

import fr.moveit.api.configuration.Roles;
import fr.moveit.api.dto.AuthentificationDTO;
import fr.moveit.api.dto.PasswordChangeDTO;
import fr.moveit.api.dto.UserCreationDTO;
import fr.moveit.api.entity.User;
import fr.moveit.api.exceptions.BadRequestException;
import fr.moveit.api.exceptions.ForbiddenException;
import fr.moveit.api.security.jwt.JWTPayload;
import fr.moveit.api.security.jwt.JWTProvider;
import fr.moveit.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

@RestController
@RequestMapping("/s")
@RequiredArgsConstructor
public class SecurityController {

	private final Logger log = LoggerFactory.getLogger(SecurityController.class);

	final private AuthenticationManager authenticationManager;

	final private UserService userService;

	final private JWTProvider tokenProvider;


	@PostMapping("/login")
	public JWTPayload login(@RequestBody AuthentificationDTO body) {
		String username = body.getUsername();
		String password = body.getPassword();

		try {
			String token = tokenProvider.createToken(
					authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
							username,
							password,
							userService.loadUserByUsername(username).getRoles()
					)),
					body.getRememberMe()
			);

			return new JWTPayload(token, "");
		} catch (RuntimeException e) {
			log.info(e.getMessage());
			throw new BadCredentialsException(e.getMessage());
		}
	}


	@PostMapping("/register")
	public JWTPayload register(@RequestBody UserCreationDTO dto) {
		userService.createUser(dto);
		try {
			String token = tokenProvider.createToken(
					authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
							dto.getUsername(),
							dto.getPassword(),
							Collections.singletonList(new SimpleGrantedAuthority(Roles.USER))
					)),
					false
			);

			return new JWTPayload(token, "");
		} catch (RuntimeException e) {
			log.info(e.getMessage());
			throw new BadCredentialsException(e.getMessage());
		}
	}

	@GetMapping("/refresh")
	public JWTPayload refresh(HttpServletRequest req) {
		String token = tokenProvider.createToken(
				authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
						req.getRemoteUser(),
						"",
						userService.loadUserByUsername(req.getRemoteUser()).getRoles()
				)),
				false
		);

		return new JWTPayload(token, "");
	}


	@PostMapping("/change-pswd")
	public void changePassword(@RequestBody PasswordChangeDTO body){
		userService.changePassword(userService.getCurrentUser(), body);
	}

}
