package de.fred4jupiter.fredbet.repository;

import de.fred4jupiter.fredbet.AbstractTransactionalIntegrationTest;
import de.fred4jupiter.fredbet.domain.AppUser;
import de.fred4jupiter.fredbet.domain.AppUserBuilder;
import de.fred4jupiter.fredbet.security.FredBetUserGroup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class AppUserRepositoryIT extends AbstractTransactionalIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    public void saveAppUser() {
        AppUser appUser = AppUserBuilder.create().withDemoData().build();
        appUser = appUserRepository.save(appUser);
        assertNotNull(appUser.getId());

        AppUser foundAppUser = appUserRepository.getOne(appUser.getId());
        assertNotNull(foundAppUser);
        assertEquals(appUser.getUsername(), foundAppUser.getUsername());
    }

    @Test
    public void fetchUsersWithLastLoginSortAsc() {
        appUserRepository.deleteAll();

        appUserRepository.save(AppUserBuilder.create().withDemoData().withUsernameAndPassword("robert", "robert")
                .withLastLogin(LocalDateTime.now()).build());
        appUserRepository.save(AppUserBuilder.create().withDemoData().withUsernameAndPassword("albert", "albert")
                .withLastLogin(LocalDateTime.now().plusHours(1)).build());

        List<AppUser> resultList = appUserRepository.fetchLastLoginUsers();
        assertEquals(2, resultList.size());
        assertEquals("albert", resultList.get(0).getUsername());
        assertEquals("robert", resultList.get(1).getUsername());
    }

    @Test
    public void appUserCanHaveMultipleRoles() {
        AppUser appUser = AppUserBuilder.create().withDemoData().withUserGroup(FredBetUserGroup.ROLE_USER).withUserGroup(FredBetUserGroup.ROLE_ADMIN).build();
        appUser = appUserRepository.save(appUser);
        appUserRepository.flush();
        assertNotNull(appUser.getId());

        AppUser foundAppUser = appUserRepository.getOne(appUser.getId());
        assertNotNull(foundAppUser);
        assertEquals(appUser.getUsername(), foundAppUser.getUsername());
        assertThat(appUser.getRoles().size(), equalTo(2));
    }
}
