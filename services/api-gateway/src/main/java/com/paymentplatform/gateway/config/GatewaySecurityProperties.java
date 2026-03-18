package com.paymentplatform.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "security")
public class GatewaySecurityProperties {

    private Jwt jwt = new Jwt();
    private List<User> users = new ArrayList<>();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public static class Jwt {
        private String secret;
        private String issuer;
        private long expirationMinutes;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    public static class User {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
