package de.fred4jupiter.fredbet.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Implementing class for resolving roles to permissions.
 *
 * @author michael
 */
@Component
public class PermissionResolverImpl implements PermissionResolver {

    @Autowired
    private FredBetUserGroups fredBetUserGroups;

    @Override
    public Collection<GrantedAuthority> resolvePermissions(Collection<? extends GrantedAuthority> roleAuthorities) {
        return roleAuthorities.stream().map(GrantedAuthority::getAuthority)
                .flatMap(userGroup -> fredBetUserGroups.getPermissionsForUserGroup(userGroup).stream()).collect(Collectors.toSet());
    }
}
