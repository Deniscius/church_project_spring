package com.eyram.dev.church_project_spring.security;

/**
 * Permissions métier canoniques utilisées comme authorities Spring Security.
 *
 * <p>Le backend reste la source de vérité : le frontend peut masquer/afficher
 * des actions à partir de ces authorities, mais la décision de sécurité finale
 * est toujours prise côté serveur.</p>
 */
public enum Permission {
    DASHBOARD_VIEW("dashboard:view"),

    DEMAND_READ("demand:read"),
    DEMAND_EDIT("demand:edit"),
    DEMAND_DELETE("demand:delete"),
    DEMAND_VALIDATE("demand:validate"),
    DEMAND_AUDIT("demand:audit"),
    DEMAND_DATE_MANAGE("demand-date:manage"),

    PAYMENT_READ("payment:read"),
    PAYMENT_MANAGE("payment:manage"),
    PAYMENT_DELETE("payment:delete"),

    TREASURY_READ("treasury:read"),
    TREASURY_MANAGE("treasury:manage"),
    PAYOUT_MANAGE("payout:manage"),

    SUBSCRIPTION_READ("subscription:read"),
    SUBSCRIPTION_CHECKOUT("subscription:checkout"),
    SUBSCRIPTION_ACTIVATE("subscription:activate"),
    SUBSCRIPTION_MANAGE("subscription:manage"),

    RECEIPT_MANAGE("receipt:manage"),

    INVOICE_READ("invoice:read"),
    INVOICE_MANAGE("invoice:manage"),

    SCHEDULE_READ("schedule:read"),
    SCHEDULE_MANAGE("schedule:manage"),
    CELEBRATION_MANAGE("celebration:manage"),
    CELEBRATION_SCHEDULE_MANAGE("celebration-schedule:manage"),

    REQUEST_TYPE_READ("request-type:read"),
    REQUEST_TYPE_MANAGE("request-type:manage"),

    PRICING_READ("pricing:read"),
    PRICING_MANAGE("pricing:manage"),

    USER_MANAGE("user:manage"),
    PARISH_READ("parish:read"),
    PARISH_MANAGE("parish:manage"),
    PARISH_SETTINGS_MANAGE("parish-settings:manage"),
    PARISH_ACCESS_MANAGE("parish-access:manage"),
    PARISH_REGISTRATION_READ("parish-registration:read"),
    PARISH_REGISTRATION_MANAGE("parish-registration:manage"),
    DEANERY_MANAGE("deanery:manage"),
    PAYMENT_TYPE_MANAGE("payment-type:manage"),

    FINANCE_READ("finance:read"),
    FINANCE_MANAGE("finance:manage"),

    SAAS_PLAN_READ("saas-plan:read"),
    SAAS_PLAN_MANAGE("saas-plan:manage"),

    PROFILE_READ("profile:read"),
    SYSTEM_ADMIN("system:admin");

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
