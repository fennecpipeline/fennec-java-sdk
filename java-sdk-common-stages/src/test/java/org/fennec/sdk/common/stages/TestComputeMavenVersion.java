package org.fennec.sdk.common.stages;

import lombok.SneakyThrows;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.fennec.sdk.common.stages.maven.ComputeMavenVersion;
import org.fennec.sdk.pipeline.StageContextDefaultImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TestComputeMavenVersion {

    private void mockFetch(Git git) {
        FetchCommand fetchCommand = mock(FetchCommand.class);
        when(git.fetch()).thenReturn(fetchCommand);
        when(fetchCommand.setTagOpt(TagOpt.FETCH_TAGS)).thenReturn(fetchCommand);
        when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    }

    @Test
    @SneakyThrows
    public void testInitialVersion() {
        Git git = mock(Git.class);
        ListTagCommand mockListTag = mock(ListTagCommand.class);

        Repository repository = mock(Repository.class);
        when(git.getRepository()).thenReturn(repository);

        mockFetch(git);

        when(git.tagList()).thenReturn(mockListTag);
        when(mockListTag.call()).thenReturn(Arrays.asList());
        when(repository.getFullBranch()).thenReturn("main");
        ObjectId objectId = ObjectId.fromString("2e517ccd481e8395c5e94d2a7099b701b10fbde0");
        when(repository.resolve(eq("main"))).thenReturn(objectId);

        LogCommand logCommand = mock(LogCommand.class);
        when(git.log()).thenReturn(logCommand);
        when(logCommand.add(eq(objectId))).thenReturn(logCommand);

        when(logCommand.call()).thenReturn(Collections.emptyList());

        ComputeMavenVersion computeMavenVersion = ComputeMavenVersion
                .builder()
                .git(git)
                .credentialsProvider(new UsernamePasswordCredentialsProvider("username", "password"))
                .pomLocation("src/test/resources/pom.xml")
                .build();

        StageContextDefaultImpl stageContext = new StageContextDefaultImpl("Test", null, null);
        computeMavenVersion.run(stageContext);

        assertThat(stageContext.getVersion(), equalTo("1.0.1"));
    }

    @Test
    @SneakyThrows
    public void testPrefixSuffixAnd4Digits() {
        Git git = mock(Git.class);
        ListTagCommand mockListTag = mock(ListTagCommand.class);

        Repository repository = mock(Repository.class);
        when(git.getRepository()).thenReturn(repository);

        mockFetch(git);

        when(git.tagList()).thenReturn(mockListTag);
        when(mockListTag.call()).thenReturn(Arrays.asList());
        when(repository.getFullBranch()).thenReturn("main");
        ObjectId objectId = ObjectId.fromString("2e517ccd481e8395c5e94d2a7099b701b10fbde0");
        when(repository.resolve(eq("main"))).thenReturn(objectId);

        LogCommand logCommand = mock(LogCommand.class);
        when(git.log()).thenReturn(logCommand);
        when(logCommand.add(eq(objectId))).thenReturn(logCommand);

        when(logCommand.call()).thenReturn(Collections.emptyList());

        ComputeMavenVersion computeMavenVersion = ComputeMavenVersion
                .builder()
                .git(git)
                .prefix("candidate-")
                .suffix("-RC")
                .pomLocation("src/test/resources/pom4digits.xml")
                .build();

        StageContextDefaultImpl stageContext = new StageContextDefaultImpl("Test", null, null);
        computeMavenVersion.run(stageContext);

        assertThat(stageContext.getVersion(), equalTo("candidate-1.0.2.1-RC"));
    }

    @Test
    @SneakyThrows
    public void testUpgradeVersion() {
        Git git = mock(Git.class);
        ListTagCommand mockListTag = mock(ListTagCommand.class);

        Repository repository = mock(Repository.class);
        when(git.getRepository()).thenReturn(repository);
        when(git.tagList()).thenReturn(mockListTag);

        mockFetch(git);

        RevWalk revCommits = new RevWalk((ObjectReader) null);
        RevCommit revCommit101 = revCommits.lookupCommit(ObjectId.fromString("b7f3de4666fd4b1e535163c0ced78f2920aac4a4"));
        RevCommit revCommitNothing = revCommits.lookupCommit(ObjectId.fromString(
                "240575bf514e2a50c1f216af42b794dde13df8e3"));
        RevCommit revCommit102 = revCommits.lookupCommit(ObjectId.fromString("2e517ccd481e8395c5e94d2a7099b701b10fbde0"));

        Ref ref1 = ref("refs/tags/1.0.1", revCommit101);
        Ref ref2 = ref("refs/tags/1.0.2", revCommit102);
        when(mockListTag.call()).thenReturn(Arrays.asList(ref1, ref2));
        when(repository.getFullBranch()).thenReturn("main");
        ObjectId objectId = ObjectId.fromString("2e517ccd481e8395c5e94d2a7099b701b10fbde0");
        when(repository.resolve(eq("main"))).thenReturn(objectId);

        LogCommand logCommand = mock(LogCommand.class);
        when(git.log()).thenReturn(logCommand);
        when(logCommand.add(eq(objectId))).thenReturn(logCommand);

        Iterable<RevCommit> commitList = Arrays.asList(revCommit102, revCommitNothing, revCommit101);
        when(logCommand.call()).thenReturn(commitList);

        ComputeMavenVersion computeMavenVersion = ComputeMavenVersion
                .builder()
                .git(git)
                .pomLocation("src/test/resources/pom.xml")
                .build();

        StageContextDefaultImpl stageContext = new StageContextDefaultImpl("Test", null, null);
        computeMavenVersion.run(stageContext);

        assertThat(stageContext.getVersion(), equalTo("1.0.3"));
    }

    private Ref ref(String name, RevCommit revCommit) {
        Ref ref = mock(Ref.class);
        when(ref.getObjectId()).thenReturn(revCommit);
        when(ref.getName()).thenReturn(name);
        return ref;
    }

}
