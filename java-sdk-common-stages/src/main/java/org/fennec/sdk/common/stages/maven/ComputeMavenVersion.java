package org.fennec.sdk.common.stages.maven;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.fennec.sdk.pipeline.StageContext;
import org.fennec.sdk.pipeline.model.SimpleStageHandler;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.fennec.sdk.error.Fail.fail;
import static org.fennec.sdk.utilities.data.XmlUtils.readXML;
import static org.fennec.sdk.utils.Utils.env;
import static org.fennec.sdk.utils.Utils.getProjectFolder;

/**
 * <p>Computes the version from maven POM and then put it in the context.
 * It gets the last tag from current branch prefixed by the version in the pom.xml
 * It increments the patch number
 * If no tag is present it starts at .1
 * Then it applies the prefix and suffix
 * </p>
 * Example with version 1.1-SNAPSHOT:
 * <ul>
 *     <li>Main version is 1.1</li>
 *     <li>Fetch tags give tags 1.1.1, 1.1.2 and 1.1.3</li>
 *     <li>It creates version 1.1.3</li>
 * <ul/>
 */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ComputeMavenVersion implements SimpleStageHandler {

    /**
     * The number of Digit for the version. Examples:
     * <ul>
     *     <li>3 for a version X.Y.Z</li>
     *     <li>4 for a version W.X.Y.Z</li>
     * </ul>
     * <p>
     * Default is 3
     */
    private int numberOfDigits;

    /**
     * Add a prefix (example: release-). Default: Empty
     */
    private String prefix;

    /**
     * Add a suffix (example: .RC). Default empty
     */
    private String suffix;

    /**
     * Provide git client
     */
    private Git git;

    /**
     * Provide pom location. Default is user.dir/../pom.xml
     */
    private String pomLocation;

    @Override
    public void run(StageContext context) {
        try {

            if (git == null) {
                File dir = new File(getProjectFolder());
                git = Git.open(dir);
            }

            String majorMinor = readXML(new File(Optional
                    .ofNullable(pomLocation)
                    .orElse(getProjectFolder() + "/pom.xml"))).get("version").asText()
                    // The digit prefix * 2
                    .substring(0, (numberOfDigits - 1) * 2 - 1);

            log.info("Raw version: {}", majorMinor);

            Map<ObjectId, List<Ref>> refMap = git
                    .tagList()
                    .call()
                    .stream()
                    .collect(Collectors.groupingBy(ref -> Optional
                            .ofNullable(ref.getPeeledObjectId())
                            .orElse(ref.getObjectId())));

            String version = StreamSupport
                    .stream(git
                            .log()
                            .add(git.getRepository().resolve(env("REF").orElse(git.getRepository().getFullBranch())))
                            .call()
                            .spliterator(), false)
                    .filter(rev -> refMap.containsKey(rev))
                    .map(rev -> refMap.get(rev))
                    .flatMap(List::stream)
                    .map(Ref::getName)
                    .map(refName -> refName.replace("refs/tags/", ""))
                    .filter(ref -> ref.startsWith(majorMinor))
                    .sorted(Collections.reverseOrder())
                    .findFirst()
                    .map(tag -> String.format("%s.%d",
                            majorMinor,
                            Integer.valueOf(tag.replace(majorMinor + ".", "")) + 1))
                    .orElse(majorMinor + ".1");

            if (prefix != null) {
                version = String.format("%s%s", prefix, version);
            }

            if (suffix != null) {
                version = String.format("%s%s", version, suffix);
            }

            log.info("Computed version: {}", version);

            context.setVersion(version);
        } catch (IOException | GitAPIException e) {
            fail("Unable to init repository", e);
        }
    }
}
