package de.unibremen.swt.see.manager.security.annotations;

import de.unibremen.swt.see.manager.security.RoleNames;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Security policy annotation for requiring admin privileges.
 */
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('" + RoleNames.ADMIN + "')")
public @interface RequireAdmin {
}
