package fr.moveit.api.controller;

import fr.moveit.api.dto.UserModificationDTO;
import fr.moveit.api.entity.User;
import fr.moveit.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
	private final UserService userService;

	@GetMapping Iterable<User> getAll(){
		return userService.getAll();
	}


	@GetMapping("/me")
	public User getCurrentUser(){
		return userService.getCurrentUser();
	}

	@PostMapping("/edit")
	public void editOwnInformation(@RequestBody UserModificationDTO body) {
		userService.editUser(userService.getCurrentUser(), body);
	}

	@PostMapping("/friend")
	public void addFriend(@RequestParam Long id) {
		userService.addFriend(id);
	}

	@DeleteMapping("/friend")
	public void removeFriend(@RequestParam Long id) {
		userService.removeFriend(id);
	}

	@PostMapping("/interest")
	public void addInterest(@RequestParam  Long id) {
		userService.addInterest(id);
	}

	@DeleteMapping("/interest")
	public void removeInterest(@RequestParam  Long id) {
		userService.removeInterest(id);
	}
}
