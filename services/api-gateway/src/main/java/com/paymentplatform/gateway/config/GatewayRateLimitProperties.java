package com.paymentplatform.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public class GatewayRateLimitProperties {

    private Route account = new Route();
    private Route transaction = new Route();

    public Route getAccount() {
        return account;
    }

    public void setAccount(Route account) {
        this.account = account;
    }

    public Route getTransaction() {
        return transaction;
    }

    public void setTransaction(Route transaction) {
        this.transaction = transaction;
    }

    public static class Route {
        private int replenishRate;
        private int burstCapacity;

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }
}
