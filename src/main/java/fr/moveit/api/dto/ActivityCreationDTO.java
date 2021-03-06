package fr.moveit.api.dto;

import fr.moveit.api.entity.ActivityVisibility;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
public class ActivityCreationDTO {
	private String name;
	private Date start;
	private Date end;

	private ActivityVisibility visibility;

	private String description;

	private String location;
	private Float price;

	private Long interest;

	private Set<Long> participants = new HashSet<>();
}
