package com.findajobremotely.security;

import com.findajobremotely.model.Role;

/** The authenticated caller, rebuilt from the database on every request (so suspensions apply instantly). */
public record AuthPrincipal(String id, String email, Role role) {
}
