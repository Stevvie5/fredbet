package de.fred4jupiter.fredbet.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.fred4jupiter.fredbet.domain.AppUser;
import de.fred4jupiter.fredbet.domain.AppUserBuilder;
import de.fred4jupiter.fredbet.domain.Country;
import de.fred4jupiter.fredbet.domain.Group;
import de.fred4jupiter.fredbet.domain.Match;
import de.fred4jupiter.fredbet.domain.MatchBuilder;
import de.fred4jupiter.fredbet.props.FredBetProfile;
import de.fred4jupiter.fredbet.props.FredbetConstants;
import de.fred4jupiter.fredbet.props.FredbetProperties;
import de.fred4jupiter.fredbet.security.FredBetRole;
import de.fred4jupiter.fredbet.service.BettingService;
import de.fred4jupiter.fredbet.service.InfoService;
import de.fred4jupiter.fredbet.service.MatchService;
import de.fred4jupiter.fredbet.service.UserAlreadyExistsException;
import de.fred4jupiter.fredbet.service.UserService;
import de.fred4jupiter.fredbet.service.image.ImageAdministrationService;
import de.fred4jupiter.fredbet.web.info.InfoType;

@Component
public class DataBasePopulator {

	private static final String DEFAULT_PASSWORD_ADMIN_USER = FredbetConstants.TECHNICAL_USERNAME;

	private static final Logger LOG = LoggerFactory.getLogger(DataBasePopulator.class);

	@Autowired
	private MatchService matchService;

	@Autowired
	private Environment environment;

	@Autowired
	private UserService userService;

	@Autowired
	private BettingService bettingService;

	@Autowired
	private RandomValueGenerator randomValueGenerator;

	@Autowired
	private FredbetProperties fredbetProperties;

	@Autowired
	private InfoService infoService;

	@Autowired
	private ImageAdministrationService imageAdministrationService;

	@PostConstruct
	private void initDatabaseWithDemoData() {
		if (!environment.acceptsProfiles(FredBetProfile.INTEGRATION_TEST)) {
			createDefaultUsers();
			addRulesIfEmpty();
		}

		if (fredbetProperties.isCreateDemoData()) {
			createAdditionalUsers();
			createRandomMatches();
			createImageGroups("Misc");
		}
	}

	public void createRandomMatches() {
		bettingService.deleteAllBets();
		matchService.deleteAllMatches();

		createRandomForGroup(Group.GROUP_A, 4);
		createRandomForGroup(Group.GROUP_B, 4);
		createRandomForGroup(Group.GROUP_C, 4);
		createRandomForGroup(Group.GROUP_D, 4);
		createRandomForGroup(Group.GROUP_E, 4);
		createRandomForGroup(Group.GROUP_F, 4);
		createRandomForGroup(Group.GROUP_G, 4);
		createRandomForGroup(Group.GROUP_H, 4);

		createRandomForGroup(Group.ROUND_OF_SIXTEEN, 8);
		createRandomForGroup(Group.QUARTER_FINAL, 4);
		createRandomForGroup(Group.SEMI_FINAL, 2);
		createRandomForGroup(Group.FINAL, 1);
		createRandomForGroup(Group.GAME_FOR_THIRD, 1);
	}

	public void createDemoBetsForAllUsers() {
		LOG.info("createDemoBetsForAllUsers...");
		bettingService.deleteAllBets();

		List<Match> allMatches = matchService.findAll();
		List<AppUser> users = userService.findAll();
		users.forEach(appUser -> {
			allMatches.forEach(match -> {
				Integer goalsTeamOne = randomValueGenerator.generateRandomValue();
				Integer goalsTeamTwo = randomValueGenerator.generateRandomValue();
				bettingService.createAndSaveBetting(appUser, match, goalsTeamOne, goalsTeamTwo);
			});

			ImmutablePair<Country, Country> teamPair = randomValueGenerator.generateTeamPair();
			bettingService.saveExtraBet(teamPair.getLeft(), teamPair.getRight(), appUser.getUsername());
		});

	}

	public void createDemoResultsForAllMatches() {
		LOG.info("createDemoResultsForAllUsers...");

		List<Match> allMatches = matchService.findAll();
		allMatches.forEach(match -> {
			if (match.getCountryOne() == null) {
				ImmutablePair<Country, Country> teamPair = randomValueGenerator.generateTeamPair();
				match.setCountryOne(teamPair.getLeft());
				match.setCountryTwo(teamPair.getRight());
				match.setTeamNameOne(null);
				match.setTeamNameTwo(null);
			}
			match.enterResult(randomValueGenerator.generateRandomValue(), randomValueGenerator.generateRandomValue());
			matchService.save(match);
		});
	}

	private void createImageGroups(String... imageGroups) {
		for (String imageGroup : imageGroups) {
			imageAdministrationService.createImageGroup(imageGroup);
		}
	}

	private void addRulesIfEmpty() {
		ClassPathResource classPathResource = new ClassPathResource("content/rules_de.txt");
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream()) {
			IOUtils.copyLarge(classPathResource.getInputStream(), byteOut);
			String rulesInGerman = byteOut.toString("UTF-8");

			Locale locale = LocaleContextHolder.getLocale();
			infoService.saveInfoContentIfNotPresent(InfoType.RULES, rulesInGerman, locale);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void createRandomForGroup(Group group, int numberOfMatches) {
		for (int i = 0; i < numberOfMatches; i++) {
			ImmutablePair<Country, Country> teamPair = randomValueGenerator.generateTeamPair();
			Match match = MatchBuilder.create().withTeams(teamPair.getLeft(), teamPair.getRight()).withGroup(group).withStadium("Somewhere")
					.withKickOffDate(LocalDateTime.now().plusDays(1).plusMinutes(i)).build();
			matchService.save(match);
		}
	}

	private void createAdditionalUsers() {
		LOG.info("createAdditionalUsers: creating additional demo users ...");

		for (int i = 1; i <= 5; i++) {
			saveIfNotPresent(
					AppUserBuilder.create().withUsernameAndPassword("test" + i, "test" + i).withRole(FredBetRole.ROLE_USER).build());
		}
	}

	private void createDefaultUsers() {
		LOG.info("createDefaultUsers: creating default users ...");

		// admin user will also be used for remote shell login
		saveIfNotPresent(AppUserBuilder.create().withUsernameAndPassword(FredbetConstants.TECHNICAL_USERNAME, DEFAULT_PASSWORD_ADMIN_USER)
				.withRole(FredBetRole.ROLE_ADMIN).deletable(false).build());

		saveIfNotPresent(AppUserBuilder.create().withUsernameAndPassword("michael", "michael").withRole(FredBetRole.ROLE_ADMIN).build());
	}

	private void saveIfNotPresent(AppUser appUser) {
		try {
			userService.insertAppUser(appUser);
		} catch (UserAlreadyExistsException e) {
			LOG.debug(e.getMessage());
		}
	}

	@Transactional
	public void deleteAllBetsAndMatches() {
		bettingService.deleteAllBets();
		matchService.deleteAllMatches();
	}

}
