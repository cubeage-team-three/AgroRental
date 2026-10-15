package com.agrorental.common.config;

import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.operator.otp.OperatorOtpService;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.DemoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P0-4/P0-5 regression tests: unsafe development defaults must never reach an unprofiled, staging or
 * production deployment.
 */
@DisplayName("P0-4/P0-5: Deployment configuration safety")
class DeploymentSafetyTest {

    private static final String TEST_JWT_SECRET = "test-secret-key-32-chars-minimum-length-for-hmac-sha256";

    private static MockEnvironment env(String[] active, String[] defaults) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(active);
        if (defaults != null) {
            environment.setDefaultProfiles(defaults);
        }
        return environment;
    }

    private static MockEnvironment explicitDev() {
        return env(new String[]{"dev"}, null);
    }

    private static MockEnvironment implicitDefaultDev() {
        return env(new String[]{}, new String[]{"dev"});
    }

    @Nested
    @DisplayName("OTP disclosure gate (farmer and operator)")
    class OtpDisclosureGate {

        private boolean farmerAllowed(MockEnvironment environment, boolean optIn) {
            FarmerOtpService service = new FarmerOtpService(null, null, null, null);
            service.setEnvironment(environment);
            service.setIncludeDevMock(optIn);
            return service.isDevMockOtpAllowed();
        }

        private boolean operatorAllowed(MockEnvironment environment, boolean optIn) {
            OperatorOtpService service = new OperatorOtpService(null, null, null, null);
            service.setEnvironment(environment);
            service.setIncludeDevMock(optIn);
            return service.isDevMockOtpAllowed();
        }

        @Test
        @DisplayName("Allowed only with explicit dev profile AND explicit opt-in")
        void explicitDevWithOptInIsAllowed() {
            assertThat(farmerAllowed(explicitDev(), true)).isTrue();
            assertThat(operatorAllowed(explicitDev(), true)).isTrue();
            assertThat(farmerAllowed(explicitDev(), false)).isFalse();
            assertThat(operatorAllowed(explicitDev(), false)).isFalse();
        }

        @Test
        @DisplayName("Omitted profile (implicit spring.profiles.default=dev) never discloses, even with opt-in")
        void implicitDefaultProfileNeverDiscloses() {
            assertThat(farmerAllowed(implicitDefaultDev(), true)).isFalse();
            assertThat(operatorAllowed(implicitDefaultDev(), true)).isFalse();
        }

        @ParameterizedTest(name = "active profiles [{0}] never disclose")
        @ValueSource(strings = {"prod", "staging", "dev,prod", "dev,staging", "dev,PROD", "production", "DEV", "DEVELOPMENT", "test"})
        @DisplayName("Production, staging, mixed and malformed profiles never disclose, even with opt-in")
        void productionStagingAndMalformedProfilesNeverDisclose(String profiles) {
            MockEnvironment environment = env(profiles.split(","), null);
            assertThat(farmerAllowed(environment, true)).isFalse();
            assertThat(operatorAllowed(environment, true)).isFalse();
        }

        @Test
        @DisplayName("Default profile configured as prod or staging never discloses")
        void prodOrStagingAsDefaultProfileNeverDiscloses() {
            assertThat(farmerAllowed(env(new String[]{"dev"}, new String[]{"prod"}), true)).isFalse();
            assertThat(operatorAllowed(env(new String[]{"dev"}, new String[]{"staging"}), true)).isFalse();
        }

        @Test
        @DisplayName("Missing environment never discloses")
        void missingEnvironmentNeverDiscloses() {
            FarmerOtpService farmer = new FarmerOtpService(null, null, null, null);
            farmer.setEnvironment(null);
            farmer.setIncludeDevMock(true);
            OperatorOtpService operator = new OperatorOtpService(null, null, null, null);
            operator.setEnvironment(null);
            operator.setIncludeDevMock(true);
            assertThat(farmer.isDevMockOtpAllowed()).isFalse();
            assertThat(operator.isDevMockOtpAllowed()).isFalse();
        }
    }

    @Nested
    @DisplayName("H2 console registration")
    class H2Console {

        private final ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(H2ConsoleConfig.class);

        @Test
        @DisplayName("Registered with explicit dev profile and console enabled, without remote access")
        void registeredOnlyInExplicitDevWithPropertyEnabled() {
            runner.withPropertyValues("spring.profiles.active=dev", "spring.h2.console.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(ServletRegistrationBean.class);
                        ServletRegistrationBean<?> bean = context.getBean(ServletRegistrationBean.class);
                        assertThat(bean.getInitParameters()).doesNotContainKey("webAllowOthers");
                    });
        }

        @Test
        @DisplayName("Not registered for an implicit default dev profile")
        void notRegisteredForImplicitDefaultProfile() {
            runner.withPropertyValues("spring.profiles.default=dev", "spring.h2.console.enabled=true")
                    .run(context -> assertThat(context).doesNotHaveBean(ServletRegistrationBean.class));
        }

        @ParameterizedTest(name = "not registered for active profiles [{0}]")
        @ValueSource(strings = {"staging", "prod", "dev,staging", "dev,prod"})
        @DisplayName("Not registered in staging or production even when the property is enabled")
        void notRegisteredInStagingOrProduction(String profiles) {
            runner.withPropertyValues("spring.profiles.active=" + profiles, "spring.h2.console.enabled=true")
                    .run(context -> assertThat(context).doesNotHaveBean(ServletRegistrationBean.class));
        }

        @Test
        @DisplayName("Not registered in explicit dev when the console property is disabled or missing")
        void notRegisteredWhenPropertyDisabled() {
            runner.withPropertyValues("spring.profiles.active=dev", "spring.h2.console.enabled=false")
                    .run(context -> assertThat(context).doesNotHaveBean(ServletRegistrationBean.class));
            runner.withPropertyValues("spring.profiles.active=dev")
                    .run(context -> assertThat(context).doesNotHaveBean(ServletRegistrationBean.class));
        }

        @Test
        @DisplayName("Security rule for /h2-console is opened only when the console is enabled")
        void securityPermitOnlyWhenConsoleEnabled() {
            MockEnvironment enabled = explicitDev();
            enabled.setProperty("spring.h2.console.enabled", "true");
            MockEnvironment implicit = implicitDefaultDev();
            implicit.setProperty("spring.h2.console.enabled", "true");
            MockEnvironment staging = env(new String[]{"staging"}, null);
            staging.setProperty("spring.h2.console.enabled", "true");

            assertThat(DeploymentProfiles.isH2ConsoleEnabled(enabled)).isTrue();
            assertThat(DeploymentProfiles.isH2ConsoleEnabled(implicit)).isFalse();
            assertThat(DeploymentProfiles.isH2ConsoleEnabled(staging)).isFalse();
            assertThat(DeploymentProfiles.isH2ConsoleEnabled(explicitDev())).isFalse();
        }
    }

    @Nested
    @DisplayName("Startup safety validator")
    class StartupValidator {

        @Test
        @DisplayName("External database without an explicit profile is rejected")
        void externalDatabaseWithoutExplicitProfileIsRejected() {
            MockEnvironment environment = implicitDefaultDev();
            environment.setProperty("spring.datasource.url", "jdbc:mysql://db.example.invalid:3306/agro");

            assertThatThrownBy(() -> StartupSafetyValidator.validate(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no Spring profile was explicitly activated");
        }

        @ParameterizedTest(name = "external database rejected with unrecognized profile [{0}]")
        @ValueSource(strings = {"production", "PROD", "Staging", "local", "test"})
        @DisplayName("External database with a misspelled or unrecognized profile is rejected")
        void externalDatabaseWithUnrecognizedProfileIsRejected(String profile) {
            MockEnvironment environment = env(new String[]{profile}, null);
            environment.setProperty("spring.datasource.url", "jdbc:mysql://db.example.invalid:3306/agro");

            assertThatThrownBy(() -> StartupSafetyValidator.validate(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no Spring profile was explicitly activated");
        }

        @ParameterizedTest(name = "external database allowed with explicit profile [{0}]")
        @ValueSource(strings = {"prod", "staging", "dev"})
        @DisplayName("External database is allowed with an explicit profile")
        void externalDatabaseWithExplicitProfileIsAllowed(String profile) {
            MockEnvironment environment = env(new String[]{profile}, null);
            environment.setProperty("spring.datasource.url", "jdbc:mysql://db.example.invalid:3306/agro");

            assertThatCode(() -> StartupSafetyValidator.validate(environment)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Embedded H2 database is allowed without an explicit profile")
        void embeddedDatabaseWithoutProfileIsAllowed() {
            MockEnvironment environment = implicitDefaultDev();
            environment.setProperty("spring.datasource.url", "jdbc:h2:mem:agro_rental_db");

            assertThatCode(() -> StartupSafetyValidator.validate(environment)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "dev combined with [{0}] is rejected")
        @ValueSource(strings = {"prod", "staging"})
        @DisplayName("dev must never be active together with prod or staging")
        void devCombinedWithProductionProfileIsRejected(String profile) {
            MockEnvironment environment = env(new String[]{"dev", profile}, null);
            environment.setProperty("spring.datasource.url", "jdbc:h2:mem:agro_rental_db");

            assertThatThrownBy(() -> StartupSafetyValidator.validate(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must not be active together");
        }

        @Test
        @DisplayName("Unresolvable mandatory datasource placeholder is left to the staging validator")
        void unresolvedDatasourcePlaceholderIsDeferred() {
            MockEnvironment environment = env(new String[]{"staging"}, null);
            environment.setProperty("spring.datasource.url", "${SOME_UNSET_DATASOURCE_URL_FOR_TEST}");

            assertThatCode(() -> StartupSafetyValidator.validate(environment)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Committed configuration files")
    class ConfigurationFiles {

        private Properties load(String name) throws IOException {
            Properties properties = new Properties();
            try (InputStream in = new ClassPathResource(name).getInputStream()) {
                properties.load(in);
            }
            return properties;
        }

        // A password property may only be empty or a placeholder without a default value
        private static final Pattern SAFE_PASSWORD_VALUE = Pattern.compile("^$|^\\$\\{[A-Z0-9_]+:?}$");
        private static final String PASSWORD_RESET_PREFIX = "app.password-reset.";

        @ParameterizedTest(name = "{0} has no hard-coded password fallbacks")
        @ValueSource(strings = {"application.properties", "application-dev.properties",
                "application-staging.properties", "application-prod.properties"})
        @DisplayName("No password property has a hard-coded value or default")
        void noHardCodedPasswordDefaults(String file) throws IOException {
            Properties properties = load(file);
            properties.stringPropertyNames().stream()
                    .filter(key -> key.toLowerCase().contains("password"))
                    // app.password-reset.* is non-secret feature configuration, checked separately below
                    .filter(key -> !key.startsWith(PASSWORD_RESET_PREFIX))
                    .forEach(key -> assertThat(properties.getProperty(key).trim())
                            .as("%s in %s", key, file)
                            .matches(SAFE_PASSWORD_VALUE));
        }

        @ParameterizedTest(name = "{0}: password-reset settings are non-secret flags/numbers and never default to token disclosure")
        @ValueSource(strings = {"application.properties", "application-dev.properties",
                "application-staging.properties", "application-prod.properties"})
        @DisplayName("Password-reset settings hold no secrets and default the dev token disclosure to false")
        void passwordResetSettingsAreNonSecretAndSafe(String file) throws IOException {
            Properties properties = load(file);
            properties.stringPropertyNames().stream()
                    .filter(key -> key.startsWith(PASSWORD_RESET_PREFIX))
                    .forEach(key -> assertThat(properties.getProperty(key).trim())
                            .as("%s in %s", key, file)
                            .matches("^(\\d+|false|\\$\\{[A-Z0-9_]+:false})$"));
            String devToken = properties.getProperty(PASSWORD_RESET_PREFIX + "dev-token-in-response");
            if (devToken != null) {
                assertThat(devToken.trim()).as("dev token flag in %s", file).matches("^(false|\\$\\{[A-Z0-9_]+:false})$");
            }
        }

        @Test
        @DisplayName("Password-reset dev token disclosure is hard false in production and staging files")
        void passwordResetDevTokenDisabledInProductionAndStaging() throws IOException {
            assertThat(load("application-prod.properties").getProperty(PASSWORD_RESET_PREFIX + "dev-token-in-response")).isEqualTo("false");
            assertThat(load("application-staging.properties").getProperty(PASSWORD_RESET_PREFIX + "dev-token-in-response")).isEqualTo("false");
        }

        @Test
        @DisplayName("Production datasource password is mandatory (no fallback)")
        void productionDatasourcePasswordIsMandatory() throws IOException {
            assertThat(load("application-prod.properties").getProperty("spring.datasource.password"))
                    .isEqualTo("${SPRING_DATASOURCE_PASSWORD}");
        }

        @Test
        @DisplayName("H2 console is off by default and in staging; only the dev file enables it")
        void h2ConsolePropertyDefaults() throws IOException {
            assertThat(load("application.properties").getProperty("spring.h2.console.enabled")).isEqualTo("false");
            assertThat(load("application-staging.properties").getProperty("spring.h2.console.enabled")).isEqualTo("false");
            assertThat(load("application-dev.properties").getProperty("spring.h2.console.enabled")).isEqualTo("true");
        }

        @Test
        @DisplayName("OTP disclosure property is false in production and staging files")
        void otpDisclosurePropertyDisabledInProductionAndStaging() throws IOException {
            assertThat(load("application-prod.properties").getProperty("app.otp.include-dev-mock")).isEqualTo("false");
            assertThat(load("application-staging.properties").getProperty("app.otp.include-dev-mock")).isEqualTo("false");
        }
    }

    @Nested
    @DisplayName("Full application startup")
    class FullStartup {

        private ConfigurableApplicationContext start(String... args) {
            SpringApplication application = new SpringApplication(DemoApplication.class);
            return application.run(args);
        }

        /** Plain HTTP GET against the started app on its random loopback port. */
        private int getStatus(ConfigurableApplicationContext context, String path) throws Exception {
            String port = context.getEnvironment().getProperty("local.server.port");
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        }

        @Test
        @DisplayName("MySQL without an explicit profile is refused before any database connection is attempted")
        void mysqlWithoutExplicitProfileIsRefusedBeforeConnecting() {
            // Port 1 on loopback: nothing listens there, so reaching the database would yield a connection error instead
            assertThatThrownBy(() -> start(
                    "--spring.datasource.url=jdbc:mysql://127.0.0.1:1/agro_safety_check",
                    "--spring.datasource.username=nobody",
                    "--spring.datasource.password=unused",
                    "--app.jwt.secret=" + TEST_JWT_SECRET,
                    "--server.port=0"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no Spring profile was explicitly activated")
                    // thrown during bean factory post-processing, i.e. before any DataSource bean exists
                    .hasStackTraceContaining("invokeBeanFactoryPostProcessors");
        }

        @Test
        @DisplayName("Unprofiled H2 deployment: no H2 console, no OTP disclosure, no mock data, no default admin")
        void unprofiledDeploymentHasNoDevelopmentConveniences() throws Exception {
            try (ConfigurableApplicationContext context = start(
                    "--spring.datasource.url=jdbc:h2:mem:deployment_safety_unprofiled;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "--app.jwt.secret=" + TEST_JWT_SECRET,
                    "--server.port=0")) {
                assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
                assertThat(context.containsBean("h2ConsoleServlet")).isFalse();
                assertThat(getStatus(context, "/h2-console/")).as("H2 console must not be reachable").isEqualTo(401);
                assertThat(context.getBean(FarmerOtpService.class).isDevMockOtpAllowed()).isFalse();
                assertThat(context.getBean(OperatorOtpService.class).isDevMockOtpAllowed()).isFalse();
                assertThat(context.getBean(PartnerRepository.class).count()).isZero();
                assertThat(context.getBean(AdminRepository.class).count()).isZero();
            }
        }

        @Test
        @DisplayName("Explicit dev profile keeps local development conveniences")
        void explicitDevKeepsLocalConveniences() throws Exception {
            try (ConfigurableApplicationContext context = start(
                    "--spring.profiles.active=dev",
                    "--spring.datasource.url=jdbc:h2:mem:deployment_safety_dev;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "--app.jwt.secret=" + TEST_JWT_SECRET,
                    "--server.port=0")) {
                assertThat(context.containsBean("h2ConsoleServlet")).isTrue();
                assertThat(getStatus(context, "/h2-console/")).as("H2 console usable in explicit dev").isEqualTo(200);
                assertThat(context.getBean(FarmerOtpService.class).isDevMockOtpAllowed()).isTrue();
                assertThat(context.getBean(OperatorOtpService.class).isDevMockOtpAllowed()).isTrue();
                assertThat(context.getBean(PartnerRepository.class).count()).isPositive();
            }
        }
    }
}
