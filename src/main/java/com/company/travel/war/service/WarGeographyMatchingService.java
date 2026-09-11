package com.company.travel.war.service;

import com.company.travel.war.entity.WarGeography;
import com.company.travel.war.repository.WarGeographyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WarGeographyMatchingService {

    private final WarGeographyRepository repository;

    public WarGeographyMatchingService(WarGeographyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MatchResult match(String destinationCountry, String destinationCity) {
        List<WarGeography> activeGeographies = repository.findAllByActiveTrue().stream()
                .filter(WarGeography::isActive)
                .toList();
        Set<Long> activeRootIds = activeGeographies.stream()
                .filter(geography -> geography.getRelatedToWarGeographyId() == null)
                .map(WarGeography::getId)
                .collect(Collectors.toSet());

        for (WarGeography geography : activeGeographies) {
            boolean direct = geography.getRelatedToWarGeographyId() == null;
            boolean relatedToActiveWar = geography.getRelatedToWarGeographyId() != null
                    && activeRootIds.contains(geography.getRelatedToWarGeographyId());
            if ((direct || relatedToActiveWar)
                    && locationMatches(geography, destinationCountry, destinationCity)) {
                return new MatchResult(true, geography);
            }
        }
        return new MatchResult(false, null);
    }

    private boolean locationMatches(WarGeography geography,
            String destinationCountry, String destinationCity) {
        if (!same(destinationCountry, geography.getCountryCode())) {
            return false;
        }
        return geography.getRegionOrCity() == null
                || same(destinationCity, geography.getRegionOrCity());
    }

    private boolean same(String first, String second) {
        return first != null && second != null
                && first.trim().toLowerCase(Locale.ROOT)
                        .equals(second.trim().toLowerCase(Locale.ROOT));
    }

    public record MatchResult(boolean matched, WarGeography geography) {
    }
}