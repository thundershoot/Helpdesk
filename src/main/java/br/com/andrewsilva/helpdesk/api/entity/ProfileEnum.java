package br.com.andrewsilva.helpdesk.api.entity;

public enum ProfileEnum {

	ROLE_ADMIN, ROLE_CUSTOMER, ROLE_TECNICIAN;

	public static ProfileEnum getProfile(String role) {
		switch (role) {
		case "ADMIN":
			return ROLE_ADMIN;
		case "CUSTOMER":
			return ROLE_CUSTOMER;
		case "TECNICIAN":
			return ROLE_TECNICIAN;
		default:
			return null;
		}
	}

}
