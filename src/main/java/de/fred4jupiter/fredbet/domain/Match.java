package de.fred4jupiter.fredbet.domain;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Match {

	private Team teamOne;
	
	private Team teamTwo;
}
