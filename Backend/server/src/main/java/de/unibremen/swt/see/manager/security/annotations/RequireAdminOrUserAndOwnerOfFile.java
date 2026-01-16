package de.unibremen.swt.see.manager.security.annotations;

import de.unibremen.swt.see.manager.security.RoleNames;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Security policy annotation for requiring either admin privileges
 * or user privileges combined with ownership validation for a file.
 */
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('" + RoleNames.ADMIN + "') or hasRole('" + RoleNames.USER + "') and @accessControlService.canAccessFile(principal.id, #id)")
public @interface RequireAdminOrUserAndOwnerOfFile {
}
