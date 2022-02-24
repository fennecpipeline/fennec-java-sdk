package org.fennecpipeline.sdk;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.fennec.sdk.common.stages.maven.ComputeMavenVersion;

import java.io.File;

import static org.fennec.sdk.error.Fail.fail;
import static org.fennec.sdk.exec.local.LocalExecService.exec;
import static org.fennec.sdk.pipeline.Pipeline.stage;
import static org.fennec.sdk.utilities.tests.Surefire.surefire;
import static org.fennec.sdk.utils.Utils.env;
import static org.fennec.sdk.utils.Utils.getProjectFolder;

@Slf4j
public class Pipeline {

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
            } finally {
                context.setTestResults(surefire().getTestsResults("Unit tests"));
            }
        });

        stage("Deploy", context -> {
            exec("mvn", "-Prelease", "deploy", "-DskipTests");
        });

        stage("Complete Release", context -> {
            try {
                git.add().addFilepattern(".").call();
                git.commit().setMessage("Version " + context.getVersion()).call();
                git.tag().setName(context.getVersion()).call();
                git.push().setPushAll().setPushTags().call();
            } catch (Exception e) {
                fail("Unable to complete release", e);
            }
        });
    }

}
