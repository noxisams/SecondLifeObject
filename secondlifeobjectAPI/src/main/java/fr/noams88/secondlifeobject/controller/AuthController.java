package fr.noams88.secondlifeobject.controller;

import fr.noams88.secondlifeobject.message.request.LoginForm;
import fr.noams88.secondlifeobject.message.request.SignUpForm;
import fr.noams88.secondlifeobject.message.response.JwtResponse;
import fr.noams88.secondlifeobject.message.response.ResponseMessage;
import fr.noams88.secondlifeobject.model.Role;
import fr.noams88.secondlifeobject.model.RoleName;
import fr.noams88.secondlifeobject.model.User;
import fr.noams88.secondlifeobject.repository.RoleRepository;
import fr.noams88.secondlifeobject.repository.UserRepository;
import fr.noams88.secondlifeobject.security.jwt.JwtProvider;
import fr.noams88.secondlifeobject.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder encoder;
	private final JwtProvider jwtProvider;
	private final UserService userService;
	private final JwtProvider tokenProvider;

	public AuthController(AuthenticationManager authenticationManager,
						  UserRepository userRepository,
						  RoleRepository roleRepository,
						  PasswordEncoder encoder,
						  JwtProvider jwtProvider,
						  UserService userService,
						  JwtProvider tokenProvider) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.encoder = encoder;
		this.jwtProvider = jwtProvider;
		this.userService = userService;
		this.tokenProvider = tokenProvider;
	}

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginForm loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);

		String jwt = jwtProvider.generateJwtToken(authentication);

		return ResponseEntity.ok(new JwtResponse(jwt));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpForm signUpRequest) {
		// V??rifie unique Username
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return new ResponseEntity<>(new ResponseMessage("Echec -> Cet username est d??j?? pris!"),
					HttpStatus.BAD_REQUEST);
		}

		// V??rifie unique Email
		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return new ResponseEntity<>(new ResponseMessage("Echec -> Cet email est d??j?? pris!"),
					HttpStatus.BAD_REQUEST);
		}

		// Cr??ation de l'utilisateur
		User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(), encoder.encode(signUpRequest.getPassword()));

		Set<String> strRoles = signUpRequest.getRole();
		Set<Role> roles = new HashSet<>();

		strRoles.forEach(role -> {
			switch (role) {
			case "ADMIN":
				Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
						.orElseThrow(() -> new RuntimeException("Echec -> Le role n'a pas ??t?? trouv??"));
				roles.add(adminRole);

				break;
			case "PM":
				Role pmRole = roleRepository.findByName(RoleName.ROLE_PM)
						.orElseThrow(() -> new RuntimeException("Echec -> Le role n'a pas ??t?? trouv??"));
				roles.add(pmRole);

				break;
			default:
				Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
						.orElseThrow(() -> new RuntimeException("Echec -> Le role n'a pas ??t?? trouv??"));
				roles.add(userRole);
			}
		});

		user.setRoles(roles);
		userRepository.save(user);

		return new ResponseEntity<>(new ResponseMessage("Utilisateur enregistr?? avec succ??s!"), HttpStatus.OK);
	}

	// R??cup??ration des informations de l'utilisateur connect??
	@PostMapping("/getUser")
	public ResponseEntity<User> getUser(@RequestBody String token) {
		String username = tokenProvider.getUsernameFromJwtToken(token);
		User user = userService.getByUsername(username);

		return new ResponseEntity<>(user, HttpStatus.OK);
	}
}
