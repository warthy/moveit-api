package fr.moveit.api.dto;

import lombok.Data;

@Data
public class UserCreationDTO {
	private String username;

	private String password;

	private String email;

	private String firstName;

	private String lastName;

	private String description;
}