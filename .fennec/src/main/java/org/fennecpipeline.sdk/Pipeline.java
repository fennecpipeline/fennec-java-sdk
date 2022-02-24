package org.fennecpipeline.sdk;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.fennec.sdk.common.stages.maven.ComputeMavenVersion;

import java.io.File;

import static org.fennec.sdk.error.Fail.fail;
import static org.fennec.sdk.exec.local.LocalExecService.exec;
import static org.fennec.sdk.pipeline.Pipeline.link;
import static org.fennec.sdk.pipeline.Pipeline.stage;
import static org.fennec.sdk.utilities.data.PropertiesUtils.readPROPERTIES;
import static org.fennec.sdk.utilities.tests.Surefire.surefire;
import static org.fennec.sdk.utils.Utils.env;
import static org.fennec.sdk.utils.Utils.getProjectFolder;

@Slf4j
public class Pipeline {

    private static final String SONAR_LOGO = "<svg id=\"Calque_1\" data-name=\"Calque 1\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 512 512\"><defs><style>.cls-1{fill:#549dd0;}</style></defs><title>SonarQube icon</title><g id=\"Illustration_5\" data-name=\"Illustration 5\"><path class=\"cls-1\" d=\"M408.78,448.09H386.5c0-179.36-148-325.28-329.91-325.28V100.53C250.79,100.53,408.78,256.44,408.78,448.09Z\"/><path class=\"cls-1\" d=\"M424.18,328.48C397.43,216,306.27,122,192,89.2l5.12-17.84C317.73,106,414,205.23,442.24,324.19Z\"/><path class=\"cls-1\" d=\"M441.31,222.87c-27.55-60.08-74.49-112.46-132.17-147.51l7.72-12.7c60.19,36.58,109.18,91.27,138,154Z\"/></g></svg>";

    private static Git git;

    public static void main(String[] args) {

        stage("Init", context -> {
            try {
                File dir = new File(getProjectFolder());
                git = Git.open(dir);
            } catch (Exception e) {
                fail("Unable to init repository", e);
            }
        });

        stage("Compute version", ComputeMavenVersion.builder().numberOfDigits(3).git(git).build());

        stage("Set version", context -> {
            exec("mvn", "versions:set", "-DgenerateBackupPoms=false", "-DnewVersion=" + context.getVersion());
        });

        stage("Build", context -> {
            exec("mvn", "clean", "package", "-DskipTests");
        });

        stage("Verify", context -> {
            try {
                String sonarUrl = env("SONAR_URL", "http://localhost:9000");
                String sonarToken = env("SONAR_TOKEN").orElseThrow(() -> new IllegalStateException(
                        "Please provide SONAR_TOKEN env variable"));
                exec("mvn", "verify", "sonar:sonar", "-Dsonar.host.url=" + sonarUrl, "-Dsonar.login=" + sonarToken);
                String dashboardUrl = readPROPERTIES(new File("../target/sonar/report-task.txt"))
                        .get("dashboardUrl")
                        .asText();

                String sonarLink = dashboardUrl.substring(dashboardUrl.lastIndexOf("/"));
                link("Sonar", String.format("http://localhost:9000%s", sonarLink), SONAR_LOGO);
            } finally {
                context.setTestResults(surefire().getTestsResults("Unit tests"));
            }
        });

        stage("Deploy", context -> {
            String passPhrase = env("PGP_PASSPHRASE").orElseThrow(() -> new IllegalStateException(
                    "Please provide PGP_PASSPHRASE env variable"));
            exec("mvn", "-Prelease", "deploy", "-DskipTests", "-Dgpg.passphrase=" + passPhrase);
        });

        stage("Complete Release", context -> {
            try {
                git.add().addFilepattern(".").call();
                git.commit().setMessage("Version " + context.getVersion()).call();
                git.tag().setName(context.getVersion()).call();
                git.push().call();
                git.push().setPushTags().call();
            } catch (Exception e) {
                fail("Unable to Complete release", e);
            }
        });

        stage("Tear Down", context -> {
            try {
                exec("mvn",
                        "versions:set",
                        "-DgenerateBackupPoms=false",
                        "-DnewVersion=" + context.getVersion().substring(0, 3) + "-SNAPSHOT");
                git.commit().setMessage("Tear down version " + context.getVersion()).call();
                git.push().call();
            } catch (Exception e) {
                fail("Unable to complete tear down", e);
            }
        });
    }
}
