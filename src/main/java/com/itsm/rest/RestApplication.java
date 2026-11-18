package com.itsm.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Aktiviert JAX-RS unter /itsm-demo/api/*.
 */
@ApplicationPath("api")
public class RestApplication extends Application {
}
