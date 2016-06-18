package de.fred4jupiter.fredbet.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import de.fred4jupiter.fredbet.domain.Country;
import de.fred4jupiter.fredbet.domain.Statistic;
import de.fred4jupiter.fredbet.repository.StatisticRepository;

@Service
public class StatisticService {

	private final StatisticRepository statisticRepository;

	private final Country favoriteCountry;

	@Autowired
	public StatisticService(Environment environment, StatisticRepository statisticRepository) {
		this.statisticRepository = statisticRepository;
		String favoriteCountryString = environment.getProperty("favorite.country", Country.GERMANY.name());
		this.favoriteCountry = Country.fromName(favoriteCountryString);
		Assert.notNull(this.favoriteCountry);
	}

	public List<Statistic> createStatistic() {
		List<Statistic> statisticList = statisticRepository.createStatistic();

		final Map<String, Integer> favoriteCountryPointsPerUserMap = statisticRepository
				.sumPointsPerUserForFavoriteCountry(favoriteCountry);
		Optional<Integer> maxOptional = favoriteCountryPointsPerUserMap.values().stream().max(Comparator.comparing(i -> i));

		for (Statistic statistic : statisticList) {
			statistic.setFavoriteCountry(this.favoriteCountry);
			final Integer favoriteCountryPoints = favoriteCountryPointsPerUserMap.get(statistic.getUsername());
			statistic.setPointsFavoriteCountry(favoriteCountryPoints);
			if (maxOptional.isPresent() && favoriteCountryPoints.equals(maxOptional.get())) {
				statistic.setFavoriteCountryCandidate(true);
			}
		}
		return statisticList;
	}
}
