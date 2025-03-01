package org.vaadin.crudui.demo.entity;

public enum MaritalStatus {

	SINGLE, MARRIED, OTHER;

	@Override
	public String toString() {
		return name().charAt(0) + name().substring(1).toLowerCase();
	}

}
